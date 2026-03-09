package com.example.bickdemo.component;

import io.minio.GetBucketPolicyArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * MinIO 初始化器
 * 应用启动时自动设置 Bucket 权限为公开可读
 * @author Administrator
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MinioInitializer implements CommandLineRunner {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public void run(String... args) {
        setBucketPublic();
    }

    /**
     * 设置 Bucket 为公开可读
     */
    private void setBucketPublic() {
        try {
            String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);

            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build());
            log.info("MinIO Bucket '{}' 已设置为公开读取", bucketName);
        } catch (Exception e) {
            log.error("MinIO Bucket 公开设置失败：{}", e.getMessage(), e);
        }
    }
}
