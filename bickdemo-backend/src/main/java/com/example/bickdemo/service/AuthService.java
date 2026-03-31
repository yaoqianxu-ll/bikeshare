package com.example.bickdemo.service; // 包声明：定义该类所属的包路径

import com.example.bickdemo.dto.AuthResponse; // 引入认证响应数据传输对象
import com.example.bickdemo.dto.EmailCodeRequest; // 引入邮箱验证码请求对象
import com.example.bickdemo.dto.EmailLoginRequest; // 引入邮箱登录请求对象
import com.example.bickdemo.dto.EmailResetPasswordRequest; // 引入邮箱重置密码请求对象
import com.example.bickdemo.dto.LoginRequest; // 引入登录请求对象
import com.example.bickdemo.dto.RegisterRequest; // 引入注册请求对象
import com.example.bickdemo.dto.UpdatePasswordRequest; // 引入更新密码请求对象
import com.example.bickdemo.dto.UpdateUserRequest; // 引入更新用户请求对象
import com.example.bickdemo.entity.EmailAuth; // 引入邮箱认证实体
import com.example.bickdemo.entity.User; // 引入用户实体
import com.example.bickdemo.entity.UserRole; // 引入用户角色枚举
import com.example.bickdemo.mapper.EmailAuthMapper; // 引入邮箱认证Mapper接口
import com.example.bickdemo.mapper.UserMapper; // 引入用户Mapper接口
import lombok.RequiredArgsConstructor; // 引入Lombok注解，用于生成构造函数
import lombok.extern.slf4j.Slf4j; // 引入日志注解
import jakarta.servlet.http.HttpServletRequest; // 引入HTTP请求对象
import org.springframework.beans.factory.annotation.Value; // 引入属性值注入注解
import org.springframework.security.authentication.AuthenticationManager; // 引入认证管理器
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // 引入用户名密码认证令牌
import org.springframework.data.redis.core.StringRedisTemplate; // 引入Redis字符串模板
import org.springframework.security.crypto.password.PasswordEncoder; // 引入密码加密器
import org.springframework.stereotype.Service; // 引入服务层注解
import org.springframework.transaction.annotation.Transactional; // 引入事务注解
import org.springframework.util.StringUtils; // 引入字符串工具类
import org.springframework.web.multipart.MultipartFile; // 引入多部分文件上传对象

import java.time.Duration; // 引入时间Duration类
import java.time.LocalDateTime; // 引入日期时间类
import java.util.Optional; // 引入Optional容器类
import java.util.concurrent.ThreadLocalRandom; // 引入线程本地随机数生成器

/**
 * 认证与用户资料服务。
 * 这里集中处理注册、登录、邮箱验证码、头像维护、资料修改等账号生命周期相关能力，
 * 控制器只负责接收请求并返回统一响应，真正的业务约束都下沉到这里统一管理。
 *
 * <p>之所以把邮箱验证码校验、资料唯一性校验、头像清理等细节放在同一个服务里，
 * 是为了保证"账号信息修改"这条业务链路始终只维护一套规则，避免控制器层重复判断。
 *
 * @author Administrator
 */
@Slf4j // 日志门面注解，自动生成日志对象
@Service // 服务层注解，标识这是一个服务类
@RequiredArgsConstructor // Lombok注解，生成包含所有final字段的构造函数
public class AuthService { // 认证服务类

    /**
     * Redis 中邮箱验证码的 key 前缀，最终会与用途和邮箱拼接成完整 key。
     */
    private static final String EMAIL_CODE_KEY_PREFIX = "auth:email:code:"; // 邮箱验证码在Redis中的key前缀

    /**
     * 当前系统支持的验证码用途。
     * 清理验证码时会遍历这个列表，把同一个邮箱在不同场景下的验证码一起清掉，
     * 避免旧验证码残留导致业务混乱。
     */
    private static final String[] EMAIL_CODE_TYPES = {"REGISTER", "RESET_PASSWORD", "UPDATE_EMAIL", "UPDATE_PASSWORD"}; // 支持的验证码类型数组

    private final UserMapper userMapper; // 用户数据访问对象，由构造函数注入
    private final EmailAuthMapper emailAuthMapper; // 邮箱认证数据访问对象
    private final PasswordEncoder passwordEncoder; // 密码加密器，用于密码的哈希处理
    private final AuthenticationManager authenticationManager; // Spring Security认证管理器
    private final JwtService jwtService; // JWT服务，用于生成和验证令牌
    private final MinioService minioService; // MinIO对象存储服务，用于头像上传
    private final EmailMailService emailMailService; // 邮件发送服务
    private final StringRedisTemplate stringRedisTemplate; // Redis模板，用于存储验证码
    private final SystemLogService systemLogService; // 系统日志服务，用于记录登录日志
    private final AdminNotificationPublisher adminNotificationPublisher; // 管理端通知发布器

    @Value("${app.mail.code-expire-minutes:10}") // 注入邮箱验证码过期分钟数，默认10分钟
    private int emailCodeExpireMinutes; // 邮箱验证码过期分钟数变量

    @Value("${app.redis.key-prefix:bickdemo:}") // 注入Redis键前缀，默认"bickdemo:"
    private String redisKeyPrefix; // Redis键前缀变量

    /**
     * 用户注册。
     * 先完成用户名/邮箱唯一性校验，再校验注册验证码，最后创建账号并签发 JWT。
     */
    @Transactional // 事务注解，确保注册流程的原子性
    public AuthResponse register(RegisterRequest request) { // 注册方法，接收注册请求对象
        String email = normalizeEmail(request.getEmail()); // 标准化邮箱地址（小写、去空格）

        // 注册前先做唯一性检查，避免无意义地消耗验证码。
        if (userMapper.existsByUsername(request.getUsername())) { // 检查用户名是否已存在
            throw new RuntimeException("用户名已存在"); // 用户名已存在，抛出异常
        }
        if (userMapper.existsByEmail(email)) { // 检查邮箱是否已被注册
            throw new RuntimeException("邮箱已被注册"); // 邮箱已存在，抛出异常
        }

        // 只有验证码通过后才允许真正落库创建用户。
        validateEmailCode(email, request.getCode(), "REGISTER"); // 校验注册验证码是否正确

        User user = new User(); // 创建新的用户对象
        user.setUsername(request.getUsername()); // 设置用户名
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 设置加密后的密码
        user.setEmail(email); // 设置邮箱地址
        user.setRole(UserRole.USER); // 设置用户角色为普通用户
        user.setEnabled(true); // 设置用户为启用状态
        userMapper.insert(user); // 将用户插入数据库

        // 发送管理端通知
        adminNotificationPublisher.notifyUserRegistered(user.getId(), user.getUsername(), user.getEmail());

        clearEmailCode(email); // 清理该邮箱的所有验证码

        return buildAuthResponse(user); // 构建并返回认证响应对象
    }

    /**
     * 发送邮箱验证码。
     * 不同业务场景会走不同的前置校验，例如注册要求邮箱未注册、找回密码要求邮箱已存在。
     */
    @Transactional // 事务注解
    public void sendEmailCode(EmailCodeRequest request) { // 发送邮箱验证码方法
        String email = normalizeEmail(request.getEmail()); // 标准化邮箱地址
        String type = normalizeCodeType(request.getType()); // 标准化验证码类型

        // 不同业务场景复用同一个发送接口，但规则不同，因此先按类型做前置拦截。
        if ("REGISTER".equals(type) && userMapper.existsByEmail(email)) { // 注册场景：邮箱不能已存在
            throw new RuntimeException("邮箱已被注册"); // 抛出异常
        }
        if ("RESET_PASSWORD".equals(type) && userMapper.findByEmail(email) == null) { // 重置密码场景：邮箱必须已注册
            throw new RuntimeException("该邮箱尚未注册"); // 抛出异常
        }
        if ("UPDATE_EMAIL".equals(type) && userMapper.existsByEmail(email)) { // 修改邮箱场景：邮箱不能已被使用
            throw new RuntimeException("邮箱已被使用"); // 抛出异常
        }
        if ("UPDATE_PASSWORD".equals(type) && userMapper.findByEmail(email) == null) { // 修改密码场景：邮箱必须已注册
            throw new RuntimeException("该邮箱尚未注册"); // 抛出异常
        }

        String code = generateVerifyCode(); // 生成6位随机验证码

        // 发新验证码前先清理旧验证码，保证同一邮箱同一时刻只有一份最新验证码可用。
        clearEmailCode(email); // 清理旧验证码
        stringRedisTemplate.opsForValue().set( // 将新验证码存入Redis
                buildEmailCodeKey(email, type), // 构建Redis键名
                code, // 验证码值
                Duration.ofMinutes(emailCodeExpireMinutes) // 设置过期时间
        );
        emailMailService.sendVerificationCode(email, code, type, emailCodeExpireMinutes); // 发送验证码邮件
    }

    /**
     * 测试账户硬编码
     */
    private static final String TEST_USERNAME = "test"; // 测试用户名常量
    private static final String TEST_PASSWORD = "123456"; // 测试密码常量

    /**
     * 用户名密码登录。
     * 认证成功后记录登录日志，再构造前端需要的 token + 用户基础信息响应。
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) { // 用户名密码登录方法
        // 硬编码测试账户 - 只读管理员
        if (TEST_USERNAME.equals(request.getUsername()) && TEST_PASSWORD.equals(request.getPassword())) { // 检查是否为测试账户登录
            User testUser = new User(); // 创建测试用户对象
            testUser.setId(-1L); // 设置测试用户ID为-1
            testUser.setUsername(TEST_USERNAME); // 设置测试用户名
            testUser.setRole(UserRole.ADMIN); // 设置为管理员角色
            testUser.setEnabled(true); // 设置为启用状态
            // 测试用户拥有额外的VIEWER权限，用于后端判断是否为测试账户
            systemLogService.recordLoginSuccess(testUser, "USERNAME", servletRequest, "测试账户登录成功"); // 记录测试账户登录成功日志
            return buildAuthResponseForViewer(testUser); // 为测试账户构建特殊响应
        }

        try { // 尝试执行认证
            authenticationManager.authenticate( // 调用认证管理器进行用户认证
                    new UsernamePasswordAuthenticationToken( // 创建用户名密码认证令牌
                            request.getUsername(), // 用户名
                            request.getPassword() // 密码
                    )
            );

            // Spring Security 认证成功后，再把完整用户信息查出来用于组装返回值和记日志。
            User user = userMapper.findByUsername(request.getUsername()); // 根据用户名查询用户信息
            if (user == null) { // 用户不存在
                throw new RuntimeException("用户不存在"); // 抛出异常
            }

            systemLogService.recordLoginSuccess(user, "USERNAME", servletRequest, "用户名登录成功"); // 记录登录成功日志
            return buildAuthResponse(user); // 构建并返回认证响应
        } catch (RuntimeException ex) { // 捕获运行时异常
            systemLogService.recordLoginFailure(request.getUsername(), "USERNAME", servletRequest, ex.getMessage()); // 记录登录失败日志
            throw ex; // 重新抛出异常
        }
    }

    /**
     * 邮箱密码登录。
     * 这里没有走 AuthenticationManager，而是直接按邮箱查用户并手动做密码比对。
     */
    public AuthResponse loginByEmail(EmailLoginRequest request, HttpServletRequest servletRequest) { // 邮箱登录方法
        String email = normalizeEmail(request.getEmail()); // 标准化邮箱地址
        try { // 尝试执行邮箱登录
            User user = userMapper.findByEmail(email); // 根据邮箱查询用户
            if (user == null) { // 用户不存在
                throw new RuntimeException("该邮箱尚未注册"); // 抛出异常
            }
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) { // 校验密码是否匹配
                throw new RuntimeException("邮箱或密码错误"); // 抛出异常
            }

            systemLogService.recordLoginSuccess(user, "EMAIL", servletRequest, "邮箱登录成功"); // 记录登录成功日志
            return buildAuthResponse(user); // 构建并返回认证响应
        } catch (RuntimeException ex) { // 捕获运行时异常
            systemLogService.recordLoginFailure(email, "EMAIL", servletRequest, ex.getMessage()); // 记录登录失败日志
            throw ex; // 重新抛出异常
        }
    }

    /**
     * 通过邮箱验证码重置密码。
     * 只有验证码与邮箱匹配并且未过期时，才允许更新密码。
     */
    @Transactional // 事务注解
    public void resetPasswordByEmail(EmailResetPasswordRequest request) { // 通过邮箱验证码重置密码方法
        String email = normalizeEmail(request.getEmail()); // 标准化邮箱地址
        validateEmailCode(email, request.getCode(), "RESET_PASSWORD"); // 校验重置密码验证码

        User user = userMapper.findByEmail(email); // 根据邮箱查询用户
        if (user == null) { // 用户不存在
            throw new RuntimeException("该邮箱尚未注册"); // 抛出异常
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword())); // 设置加密后的新密码
        userMapper.updateById(user); // 更新用户信息

        clearEmailCode(email); // 清理该邮箱的所有验证码
    }

    /**
     * 根据当前登录用户名查询用户详情。
     * 返回 Optional 是为了让控制器决定"找不到用户"时使用什么 HTTP 语义。
     */
    public Optional<User> getCurrentUser(String username) { // 获取当前用户方法
        return Optional.ofNullable(userMapper.findByUsername(username)); // 返回包含用户或null的Optional
    }

    /**
     * 更新当前用户资料。
     * 支持修改用户名、邮箱、头像和简介，其中邮箱变更必须通过验证码确认。
     */
    @Transactional // 事务注解
    public AuthResponse updateUser(String username, UpdateUserRequest request) { // 更新用户资料方法
        User user = userMapper.findByUsername(username); // 根据用户名查询用户
        if (user == null) { // 用户不存在
            throw new RuntimeException("用户不存在"); // 抛出异常
        }

        String currentEmail = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase(); // 获取当前邮箱并标准化
        String nextUsername = StringUtils.hasText(request.getUsername()) ? request.getUsername().trim() : user.getUsername(); // 确定要更新的用户名

        // 用户名改动需要重新做唯一性校验。
        if (!nextUsername.equals(user.getUsername())) { // 检查用户名是否被修改
            if (userMapper.existsByUsername(nextUsername)) { // 检查新用户名是否已存在
                throw new RuntimeException("用户名已被使用"); // 抛出异常
            }
            user.setUsername(nextUsername); // 更新用户名
        }

        if (StringUtils.hasText(request.getEmail())) { // 检查是否提供了新邮箱
            String normalizedEmail = normalizeEmail(request.getEmail()); // 标准化新邮箱
            if (!normalizedEmail.equals(currentEmail)) { // 检查邮箱是否真的被修改了
                // 修改邮箱属于高风险操作，必须校验目标邮箱唯一且要求提供验证码。
                if (userMapper.existsByEmail(normalizedEmail)) { // 检查新邮箱是否已被使用
                    throw new RuntimeException("邮箱已被使用"); // 抛出异常
                }
                if (!StringUtils.hasText(request.getCode())) { // 检查是否提供了验证码
                    throw new RuntimeException("修改邮箱需要先填写验证码"); // 抛出异常
                }
                validateEmailCode(normalizedEmail, request.getCode(), "UPDATE_EMAIL"); // 校验邮箱验证码
                clearEmailCode(normalizedEmail); // 清理验证码
                user.setEmail(normalizedEmail); // 更新邮箱
            }
        }

        // 头像字段允许前端直接传已上传好的 URL。
        if (request.getAvatar() != null) { // 检查是否提供了新头像
            user.setAvatar(request.getAvatar()); // 更新头像URL
        }

        if (request.getBio() != null) { // 检查是否提供了新简介
            String normalizedBio = request.getBio().trim(); // 标准化简介
            user.setBio(StringUtils.hasText(normalizedBio) ? normalizedBio : null); // 设置简介或null
        }

        userMapper.updateById(user); // 更新用户信息到数据库
        return buildAuthResponse(user); // 构建并返回认证响应
    }

    /**
     * 修改当前登录用户密码。
     * 除了验证旧密码外，还要求用户完成当前绑定邮箱的验证码校验，降低账号被盗后的风险。
     */
    @Transactional // 事务注解
    public void updatePassword(String username, UpdatePasswordRequest request) { // 修改密码方法
        User user = userMapper.findByUsername(username); // 根据用户名查询用户
        if (user == null) { // 用户不存在
            throw new RuntimeException("用户不存在"); // 抛出异常
        }
        if (!StringUtils.hasText(user.getEmail())) { // 检查用户是否已绑定邮箱
            throw new RuntimeException("当前账号尚未绑定邮箱，无法通过邮箱验证码修改密码"); // 抛出异常
        }

        String email = normalizeEmail(user.getEmail()); // 标准化邮箱地址
        validateEmailCode(email, request.getCode(), "UPDATE_PASSWORD"); // 校验修改密码验证码

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) { // 校验旧密码是否正确
            throw new RuntimeException("当前密码错误"); // 抛出异常
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) { // 检查新密码是否与当前密码相同
            throw new RuntimeException("新密码不能与当前密码相同"); // 抛出异常
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword())); // 设置加密后的新密码
        userMapper.updateById(user); // 更新用户信息
        clearEmailCode(email); // 清理该邮箱的所有验证码
    }

    /**
     * 上传或更新用户头像。
     * 新头像上传成功后会写回 users.avatar，旧头像采用 best-effort 删除，
     * 即旧文件删不掉也不阻塞用户完成头像更新。
     */
    @Transactional // 事务注解
    public User uploadAvatar(String username, MultipartFile file) { // 上传头像方法
        if (file == null || file.isEmpty()) { // 检查文件是否为空
            throw new RuntimeException("请选择要上传的头像"); // 抛出异常
        }

        User user = userMapper.findByUsername(username); // 根据用户名查询用户
        if (user == null) { // 用户不存在
            throw new RuntimeException("用户不存在"); // 抛出异常
        }

        String old = user.getAvatar(); // 获取旧头像URL
        if (old != null && !old.trim().isEmpty()) { // 检查是否有旧头像
            try { // 尝试删除旧头像
                minioService.deleteImage(old); // 调用MinIO服务删除旧头像
            } catch (Exception ignored) { // 捕获异常但忽略
                // best-effort: do not fail avatar update if deletion fails // 尽力删除，删除失败不影响更新
            }
        }

        // 只有新文件上传成功后才覆盖数据库中的头像地址。
        String url = minioService.uploadImage(file); // 上传新头像并获取URL
        user.setAvatar(url); // 设置用户新头像URL
        userMapper.updateById(user); // 更新用户信息
        return user; // 返回更新后的用户对象
    }

    /**
     * 删除当前用户头像。
     * 与上传逻辑类似，对象存储中的旧文件删除失败不会回滚数据库字段清空操作。
     */
    @Transactional // 事务注解
    public User deleteAvatar(String username) { // 删除头像方法
        User user = userMapper.findByUsername(username); // 根据用户名查询用户
        if (user == null) { // 用户不存在
            throw new RuntimeException("用户不存在"); // 抛出异常
        }

        String old = user.getAvatar(); // 获取旧头像URL
        if (old != null && !old.trim().isEmpty()) { // 检查是否有旧头像
            try { // 尝试删除旧头像
                minioService.deleteImage(old); // 调用MinIO服务删除旧头像
            } catch (Exception ignored) { // 捕获异常但忽略
                // best-effort // 尽力删除
            }
        }

        user.setAvatar(null); // 将用户头像设为null
        userMapper.updateById(user); // 更新用户信息
        return user; // 返回更新后的用户对象
    }

    private void validateEmailCode(String email, String code, String type) { // 校验邮箱验证码私有方法
        String normalizedType = normalizeCodeType(type); // 标准化验证码类型
        String redisCode = stringRedisTemplate.opsForValue().get(buildEmailCodeKey(email, normalizedType)); // 从Redis获取验证码
        if (StringUtils.hasText(redisCode)) { // 检查Redis中是否有验证码
            // 优先使用 Redis 中的最新验证码，兼容数据库旧数据只是兜底。
            if (!redisCode.equals(code)) { // 校验验证码是否匹配
                throw new RuntimeException("验证码错误"); // 抛出异常
            }
            return; // 验证码正确，直接返回
        }

        validateEmailCodeFromDatabase(email, code, normalizedType); // Redis没有则从数据库校验
    }

    private void validateEmailCodeFromDatabase(String email, String code, String type) { // 从数据库校验验证码的私有方法
        // 数据库校验属于兼容历史实现的降级路径，避免旧环境因数据迁移问题无法登录/注册。
        EmailAuth record = emailAuthMapper.findByEmail(email); // 从数据库查找邮箱认证记录
        if (record == null) { // 记录不存在
            throw new RuntimeException("请先获取验证码"); // 抛出异常
        }
        if (!type.equalsIgnoreCase(String.valueOf(record.getCodeType()))) { // 检查验证码类型是否匹配
            throw new RuntimeException("验证码用途不匹配，请重新获取"); // 抛出异常
        }
        if (!StringUtils.hasText(record.getVerifyCode()) || !record.getVerifyCode().equals(code)) { // 检查验证码是否正确
            throw new RuntimeException("验证码错误"); // 抛出异常
        }
        if (record.getCodeExpireAt() == null || record.getCodeExpireAt().isBefore(LocalDateTime.now())) { // 检查验证码是否过期
            throw new RuntimeException("验证码已过期，请重新获取"); // 抛出异常
        }
    }

    private void clearEmailCode(String email) { // 清理邮箱所有验证码的私有方法
        // 同一个邮箱的验证码是"一次性凭证"，使用后要把全部场景缓存一起清理掉。
        for (String type : EMAIL_CODE_TYPES) { // 遍历所有验证码类型
            stringRedisTemplate.delete(buildEmailCodeKey(email, type)); // 删除该类型对应的验证码
        }
        clearEmailCodeFromDatabase(email); // 同时清理数据库中的验证码记录
    }

    private void clearEmailCodeFromDatabase(String email) { // 清理数据库中验证码记录的私有方法
        EmailAuth record = emailAuthMapper.findByEmail(email); // 查找邮箱认证记录
        if (record == null) return; // 记录不存在，直接返回
        record.setVerifyCode(null); // 清除验证码
        record.setCodeType(null); // 清除验证码类型
        record.setCodeExpireAt(null); // 清除过期时间
        emailAuthMapper.updateById(record); // 更新记录
    }

    private String buildEmailCodeKey(String email, String type) { // 构建邮箱验证码Redis键的私有方法
        return redisKeyPrefix + EMAIL_CODE_KEY_PREFIX + type + ":" + email; // 拼接并返回完整键名
    }

    private String normalizeEmail(String email) { // 标准化邮箱地址的私有方法
        if (!StringUtils.hasText(email)) { // 检查邮箱是否为空
            throw new RuntimeException("邮箱不能为空"); // 抛出异常
        }
        // 邮箱统一转小写，避免大小写差异导致唯一性校验不一致。
        return email.trim().toLowerCase(); // 去空格并转为小写返回
    }

    private String normalizeCodeType(String type) { // 标准化验证码类型的私有方法
        if (!StringUtils.hasText(type)) { // 检查类型是否为空
            throw new RuntimeException("验证码用途不能为空"); // 抛出异常
        }
        String normalized = type.trim().toUpperCase(); // 去空格并转为大写
        if (!"REGISTER".equals(normalized) // 检查是否为支持的类型
                && !"RESET_PASSWORD".equals(normalized)
                && !"UPDATE_EMAIL".equals(normalized)
                && !"UPDATE_PASSWORD".equals(normalized)) {
            throw new RuntimeException("不支持的验证码用途"); // 抛出异常
        }
        return normalized; // 返回标准化后的类型
    }

    private String generateVerifyCode() { // 生成验证码的私有方法
        // 生成 6 位随机验证码。
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000)); // 生成并返回6位随机数字字符串
    }

    private AuthResponse buildAuthResponse(User user) { // 构建认证响应的私有方法
        // 统一封装登录/注册/更新资料后的返回结构，保证前端字段来源一致。
        String jwtToken = jwtService.generateToken(user); // 为用户生成JWT令牌
        return new AuthResponse(jwtToken, user.getUsername(), user.getRole().name(), user.getId(), false); // 创建并返回认证响应对象
    }

    /**
     * 为只读测试账户构建响应
     */
    private AuthResponse buildAuthResponseForViewer(User user) { // 为测试账户构建认证响应的私有方法
        String jwtToken = jwtService.generateTokenForViewer(user); // 为测试用户生成特殊JWT令牌
        return new AuthResponse(jwtToken, user.getUsername(), user.getRole().name(), user.getId(), true); // 创建并返回认证响应对象，isViewer标记为true
    }
} // 认证服务类结束
