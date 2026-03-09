package com.example.bickdemo.component;

import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.UserRole;
import com.example.bickdemo.entity.BackgroundImage;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.mapper.BackgroundImageMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据初始化器
 * 应用启动时自动初始化 MinIO Bucket 和创建默认用户账号
 * @author Administrator
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MinioClient minioClient;
    private final BackgroundImageMapper backgroundImageMapper;
    private final String bucketName = "bicycles";

    @Override
    public void run(String... args) {
        // 初始化 MinIO bucket
        initMinioBucket();

        // 创建默认管理员账号
        if (!userMapper.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setRole(UserRole.ADMIN);
            admin.setEnabled(true);
            userMapper.insert(admin);
            System.out.println("默认管理员账号创建成功：admin / admin123");
        }

        // 创建测试用户
        if (!userMapper.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@example.com");
            user.setRole(UserRole.USER);
            user.setEnabled(true);
            userMapper.insert(user);
            System.out.println("测试用户账号创建成功：user / user123");
        }

        // 初始化默认背景图片
        initBackgroundImages();
    }

    /**
     * 初始化 MinIO Bucket
     */
    private void initMinioBucket() {
        try {
            // 检查 bucket 是否存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                // 创建 bucket
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO Bucket '{}' 创建成功", bucketName);
            } else {
                log.info("MinIO Bucket '{}' 已存在", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO Bucket 初始化失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 初始化默认背景图片
     */
    private void initBackgroundImages() {
        List<BackgroundImage> existing = backgroundImageMapper.selectList(null);
        if (existing != null && !existing.isEmpty()) {
            log.info("背景图片已存在，跳过初始化");
            return;
        }

        try {
            BackgroundImage bg1 = new BackgroundImage();
            bg1.setName("默认背景 1");
            bg1.setImageUrl("https://images.unsplash.com/photo-1571333250630-f0230c320b6d?w=1920&q=80");
            bg1.setType("DEFAULT");
            bg1.setEnabled(true);
            bg1.setSort(1);
            backgroundImageMapper.insert(bg1);

            BackgroundImage bg2 = new BackgroundImage();
            bg2.setName("城市骑行");
            bg2.setImageUrl("https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=1920&q=80");
            bg2.setType("DEFAULT");
            bg2.setEnabled(false);
            bg2.setSort(2);
            backgroundImageMapper.insert(bg2);

            BackgroundImage bg3 = new BackgroundImage();
            bg3.setName("山地自行车");
            bg3.setImageUrl("https://images.unsplash.com/photo-1517649763962-0c623066013b?w=1920&q=80");
            bg3.setType("DEFAULT");
            bg3.setEnabled(false);
            bg3.setSort(3);
            backgroundImageMapper.insert(bg3);

            BackgroundImage bg4 = new BackgroundImage();
            bg4.setName("公园骑行");
            bg4.setImageUrl("https://images.unsplash.com/photo-1541625602341-73c00d2bda61?w=1920&q=80");
            bg4.setType("DEFAULT");
            bg4.setEnabled(false);
            bg4.setSort(4);
            backgroundImageMapper.insert(bg4);

            BackgroundImage bg5 = new BackgroundImage();
            bg5.setName("日落骑行");
            bg5.setImageUrl("https://images.unsplash.com/photo-1507035895480-2b3156c312a6?w=1920&q=80");
            bg5.setType("DEFAULT");
            bg5.setEnabled(false);
            bg5.setSort(5);
            backgroundImageMapper.insert(bg5);

            log.info("默认背景图片初始化完成");
        } catch (Exception e) {
            log.error("背景图片初始化失败：{}", e.getMessage(), e);
        }
    }
}
