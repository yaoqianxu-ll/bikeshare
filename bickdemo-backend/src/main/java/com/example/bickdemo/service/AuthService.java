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
 * 认证服务类
 * 处理用户注册、登录、邮箱验证码、资料管理等业务逻辑
 * @author Administrator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String EMAIL_CODE_KEY_PREFIX = "auth:email:code:";
    private static final String[] EMAIL_CODE_TYPES = {"REGISTER", "RESET_PASSWORD", "UPDATE_EMAIL"};

    private final UserMapper userMapper;
    private final EmailAuthMapper emailAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MinioService minioService;
    private final EmailMailService emailMailService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.mail.code-expire-minutes:10}")
    private int emailCodeExpireMinutes;

    /**
     * 用户注册（邮箱验证码）
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userMapper.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userMapper.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }

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
     * 发送邮箱验证码
     */
    @Transactional
    public void sendEmailCode(EmailCodeRequest request) {
        String email = normalizeEmail(request.getEmail());
        String type = normalizeCodeType(request.getType());

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
        clearEmailCode(email);
        stringRedisTemplate.opsForValue().set(
                buildEmailCodeKey(email, type),
                code,
                Duration.ofMinutes(emailCodeExpireMinutes)
        );
        emailMailService.sendVerificationCode(email, code, type, emailCodeExpireMinutes);
    }

    /**
     * 用户名登录
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return buildAuthResponse(user);
    }

    /**
     * 邮箱登录
     */
    public AuthResponse loginByEmail(EmailLoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("该邮箱尚未注册");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("邮箱或密码错误");
        }

        return buildAuthResponse(user);
    }

    /**
     * 邮箱找回密码
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
     * 获取当前用户信息
     */
    public Optional<User> getCurrentUser(String username) {
        return Optional.ofNullable(userMapper.findByUsername(username));
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public AuthResponse updateUser(String username, UpdateUserRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String currentEmail = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        String nextUsername = StringUtils.hasText(request.getUsername()) ? request.getUsername().trim() : user.getUsername();

        if (!nextUsername.equals(user.getUsername())) {
            if (userMapper.existsByUsername(nextUsername)) {
                throw new RuntimeException("用户名已被使用");
            }
            user.setUsername(nextUsername);
        }

        if (StringUtils.hasText(request.getEmail())) {
            String normalizedEmail = normalizeEmail(request.getEmail());
            if (!normalizedEmail.equals(currentEmail)) {
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
     * 修改当前用户密码
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
     * 上传/更新用户头像（写入 users.avatar）
     * - 会尽量删除旧头像（删除失败不影响更新）
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

        String url = minioService.uploadImage(file);
        user.setAvatar(url);
        userMapper.updateById(user);
        return user;
    }

    /**
     * 删除用户头像（清空 users.avatar）
     * - 会尽量删除对象存储中的图片（删除失败不影响清空 DB 字段）
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
            if (!redisCode.equals(code)) {
                throw new RuntimeException("验证码错误");
            }
            return;
        }

        validateEmailCodeFromDatabase(email, code, normalizedType);
    }

    private void validateEmailCodeFromDatabase(String email, String code, String type) {
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
        return EMAIL_CODE_KEY_PREFIX + type + ":" + email;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("邮箱不能为空");
        }
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
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private AuthResponse buildAuthResponse(User user) {
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, user.getUsername(), user.getRole().name(), user.getId());
    }
}
