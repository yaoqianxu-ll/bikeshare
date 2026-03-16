package com.example.bickdemo.service;

import com.example.bickdemo.dto.AuthResponse;
import com.example.bickdemo.dto.EmailCodeRequest;
import com.example.bickdemo.dto.EmailLoginRequest;
import com.example.bickdemo.dto.EmailResetPasswordRequest;
import com.example.bickdemo.dto.LoginRequest;
import com.example.bickdemo.dto.RegisterRequest;
import com.example.bickdemo.dto.UpdatePasswordRequest;
import com.example.bickdemo.dto.UpdateUserRequest;
import com.example.bickdemo.entity.EmailAuth;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.UserRole;
import com.example.bickdemo.mapper.EmailAuthMapper;
import com.example.bickdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 认证与用户资料服务。
 * 这里集中处理注册、登录、邮箱验证码、头像维护、资料修改等账号生命周期相关能力，
 * 控制器只负责接收请求并返回统一响应，真正的业务约束都下沉到这里统一管理。
 *
 * <p>之所以把邮箱验证码校验、资料唯一性校验、头像清理等细节放在同一个服务里，
 * 是为了保证“账号信息修改”这条业务链路始终只维护一套规则，避免控制器层重复判断。
 *
 * @author Administrator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Redis 中邮箱验证码的 key 前缀，最终会与用途和邮箱拼接成完整 key。
     */
    private static final String EMAIL_CODE_KEY_PREFIX = "auth:email:code:";

    /**
     * 当前系统支持的验证码用途。
     * 清理验证码时会遍历这个列表，把同一个邮箱在不同场景下的验证码一起清掉，
     * 避免旧验证码残留导致业务混乱。
     */
    private static final String[] EMAIL_CODE_TYPES = {"REGISTER", "RESET_PASSWORD", "UPDATE_EMAIL"};

    private final UserMapper userMapper;
    private final EmailAuthMapper emailAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MinioService minioService;
    private final EmailMailService emailMailService;
    private final StringRedisTemplate stringRedisTemplate;
    private final SystemLogService systemLogService;

    @Value("${app.mail.code-expire-minutes:10}")
    private int emailCodeExpireMinutes;

    @Value("${app.redis.key-prefix:bickdemo:}")
    private String redisKeyPrefix;

    /**
     * 用户注册。
     * 先完成用户名/邮箱唯一性校验，再校验注册验证码，最后创建账号并签发 JWT。
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        // 注册前先做唯一性检查，避免无意义地消耗验证码。
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userMapper.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 只有验证码通过后才允许真正落库创建用户。
        validateEmailCode(email, request.getCode(), "REGISTER");

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(email);
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        userMapper.insert(user);

        clearEmailCode(email);

        return buildAuthResponse(user);
    }

    /**
     * 发送邮箱验证码。
     * 不同业务场景会走不同的前置校验，例如注册要求邮箱未注册、找回密码要求邮箱已存在。
     */
    @Transactional
    public void sendEmailCode(EmailCodeRequest request) {
        String email = normalizeEmail(request.getEmail());
        String type = normalizeCodeType(request.getType());

        // 不同业务场景复用同一个发送接口，但规则不同，因此先按类型做前置拦截。
        if ("REGISTER".equals(type) && userMapper.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }
        if ("RESET_PASSWORD".equals(type) && userMapper.findByEmail(email) == null) {
            throw new RuntimeException("该邮箱尚未注册");
        }
        if ("UPDATE_EMAIL".equals(type) && userMapper.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被使用");
        }

        String code = generateVerifyCode();

        // 发新验证码前先清理旧验证码，保证同一邮箱同一时刻只有一份最新验证码可用。
        clearEmailCode(email);
        stringRedisTemplate.opsForValue().set(
                buildEmailCodeKey(email, type),
                code,
                Duration.ofMinutes(emailCodeExpireMinutes)
        );
        emailMailService.sendVerificationCode(email, code, type, emailCodeExpireMinutes);
    }

    /**
     * 用户名密码登录。
     * 认证成功后记录登录日志，再构造前端需要的 token + 用户基础信息响应。
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Spring Security 认证成功后，再把完整用户信息查出来用于组装返回值和记日志。
            User user = userMapper.findByUsername(request.getUsername());
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            systemLogService.recordLoginSuccess(user, "USERNAME", servletRequest, "用户名登录成功");
            return buildAuthResponse(user);
        } catch (RuntimeException ex) {
            systemLogService.recordLoginFailure(request.getUsername(), "USERNAME", servletRequest, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 邮箱密码登录。
     * 这里没有走 AuthenticationManager，而是直接按邮箱查用户并手动做密码比对。
     */
    public AuthResponse loginByEmail(EmailLoginRequest request, HttpServletRequest servletRequest) {
        String email = normalizeEmail(request.getEmail());
        try {
            User user = userMapper.findByEmail(email);
            if (user == null) {
                throw new RuntimeException("该邮箱尚未注册");
            }
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("邮箱或密码错误");
            }

            systemLogService.recordLoginSuccess(user, "EMAIL", servletRequest, "邮箱登录成功");
            return buildAuthResponse(user);
        } catch (RuntimeException ex) {
            systemLogService.recordLoginFailure(email, "EMAIL", servletRequest, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 通过邮箱验证码重置密码。
     * 只有验证码与邮箱匹配并且未过期时，才允许更新密码。
     */
    @Transactional
    public void resetPasswordByEmail(EmailResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        validateEmailCode(email, request.getCode(), "RESET_PASSWORD");

        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("该邮箱尚未注册");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        clearEmailCode(email);
    }

    /**
     * 根据当前登录用户名查询用户详情。
     * 返回 Optional 是为了让控制器决定“找不到用户”时使用什么 HTTP 语义。
     */
    public Optional<User> getCurrentUser(String username) {
        return Optional.ofNullable(userMapper.findByUsername(username));
    }

    /**
     * 更新当前用户资料。
     * 支持修改用户名、邮箱、头像和简介，其中邮箱变更必须通过验证码确认。
     */
    @Transactional
    public AuthResponse updateUser(String username, UpdateUserRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String currentEmail = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        String nextUsername = StringUtils.hasText(request.getUsername()) ? request.getUsername().trim() : user.getUsername();

        // 用户名改动需要重新做唯一性校验。
        if (!nextUsername.equals(user.getUsername())) {
            if (userMapper.existsByUsername(nextUsername)) {
                throw new RuntimeException("用户名已被使用");
            }
            user.setUsername(nextUsername);
        }

        if (StringUtils.hasText(request.getEmail())) {
            String normalizedEmail = normalizeEmail(request.getEmail());
            if (!normalizedEmail.equals(currentEmail)) {
                // 修改邮箱属于高风险操作，必须校验目标邮箱唯一且要求提供验证码。
                if (userMapper.existsByEmail(normalizedEmail)) {
                    throw new RuntimeException("邮箱已被使用");
                }
                if (!StringUtils.hasText(request.getCode())) {
                    throw new RuntimeException("修改邮箱需要先填写验证码");
                }
                validateEmailCode(normalizedEmail, request.getCode(), "UPDATE_EMAIL");
                clearEmailCode(normalizedEmail);
                user.setEmail(normalizedEmail);
            }
        }

        // 头像字段允许前端直接传已上传好的 URL。
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        if (request.getBio() != null) {
            String normalizedBio = request.getBio().trim();
            user.setBio(StringUtils.hasText(normalizedBio) ? normalizedBio : null);
        }

        userMapper.updateById(user);
        return buildAuthResponse(user);
    }

    /**
     * 修改当前登录用户密码。
     * 需要验证旧密码，并防止把新密码改成与当前密码一致。
     */
    @Transactional
    public void updatePassword(String username, UpdatePasswordRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("新密码不能与当前密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    /**
     * 上传或更新用户头像。
     * 新头像上传成功后会写回 users.avatar，旧头像采用 best-effort 删除，
     * 即旧文件删不掉也不阻塞用户完成头像更新。
     */
    @Transactional
    public User uploadAvatar(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要上传的头像");
        }

        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String old = user.getAvatar();
        if (old != null && !old.trim().isEmpty()) {
            try {
                minioService.deleteImage(old);
            } catch (Exception ignored) {
                // best-effort: do not fail avatar update if deletion fails
            }
        }

        // 只有新文件上传成功后才覆盖数据库中的头像地址。
        String url = minioService.uploadImage(file);
        user.setAvatar(url);
        userMapper.updateById(user);
        return user;
    }

    /**
     * 删除当前用户头像。
     * 与上传逻辑类似，对象存储中的旧文件删除失败不会回滚数据库字段清空操作。
     */
    @Transactional
    public User deleteAvatar(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String old = user.getAvatar();
        if (old != null && !old.trim().isEmpty()) {
            try {
                minioService.deleteImage(old);
            } catch (Exception ignored) {
                // best-effort
            }
        }

        user.setAvatar(null);
        userMapper.updateById(user);
        return user;
    }

    private void validateEmailCode(String email, String code, String type) {
        String normalizedType = normalizeCodeType(type);
        String redisCode = stringRedisTemplate.opsForValue().get(buildEmailCodeKey(email, normalizedType));
        if (StringUtils.hasText(redisCode)) {
            // 优先使用 Redis 中的最新验证码，兼容数据库旧数据只是兜底。
            if (!redisCode.equals(code)) {
                throw new RuntimeException("验证码错误");
            }
            return;
        }

        validateEmailCodeFromDatabase(email, code, normalizedType);
    }

    private void validateEmailCodeFromDatabase(String email, String code, String type) {
        // 数据库校验属于兼容历史实现的降级路径，避免旧环境因数据迁移问题无法登录/注册。
        EmailAuth record = emailAuthMapper.findByEmail(email);
        if (record == null) {
            throw new RuntimeException("请先获取验证码");
        }
        if (!type.equalsIgnoreCase(String.valueOf(record.getCodeType()))) {
            throw new RuntimeException("验证码用途不匹配，请重新获取");
        }
        if (!StringUtils.hasText(record.getVerifyCode()) || !record.getVerifyCode().equals(code)) {
            throw new RuntimeException("验证码错误");
        }
        if (record.getCodeExpireAt() == null || record.getCodeExpireAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
    }

    private void clearEmailCode(String email) {
        // 同一个邮箱的验证码是“一次性凭证”，使用后要把全部场景缓存一起清理掉。
        for (String type : EMAIL_CODE_TYPES) {
            stringRedisTemplate.delete(buildEmailCodeKey(email, type));
        }
        clearEmailCodeFromDatabase(email);
    }

    private void clearEmailCodeFromDatabase(String email) {
        EmailAuth record = emailAuthMapper.findByEmail(email);
        if (record == null) return;
        record.setVerifyCode(null);
        record.setCodeType(null);
        record.setCodeExpireAt(null);
        emailAuthMapper.updateById(record);
    }

    private String buildEmailCodeKey(String email, String type) {
        return redisKeyPrefix + EMAIL_CODE_KEY_PREFIX + type + ":" + email;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("邮箱不能为空");
        }
        // 邮箱统一转小写，避免大小写差异导致唯一性校验不一致。
        return email.trim().toLowerCase();
    }

    private String normalizeCodeType(String type) {
        if (!StringUtils.hasText(type)) {
            throw new RuntimeException("验证码用途不能为空");
        }
        String normalized = type.trim().toUpperCase();
        if (!"REGISTER".equals(normalized) && !"RESET_PASSWORD".equals(normalized) && !"UPDATE_EMAIL".equals(normalized)) {
            throw new RuntimeException("不支持的验证码用途");
        }
        return normalized;
    }

    private String generateVerifyCode() {
        // 生成 6 位随机验证码。
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private AuthResponse buildAuthResponse(User user) {
        // 统一封装登录/注册/更新资料后的返回结构，保证前端字段来源一致。
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, user.getUsername(), user.getRole().name(), user.getId());
    }
}
