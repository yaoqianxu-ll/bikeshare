package com.example.bickdemo.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * MinIO 对象存储服务类
 * 提供图片上传、删除等功能
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    // 允许的图片类型
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传图片到 MinIO
     * @param file 图片文件
     * @return 图片访问 URL
     */
    public String uploadImage(MultipartFile file) {
        try {
            // 验证文件类型
            validateImageType(file);

            // 验证文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("图片大小不能超过 5MB");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 上传到 MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 返回图片访问 URL
            return getImageUrl(filename);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            throw new RuntimeException("上传图片失败：" + e.getMessage());
        }
    }

    /**
     * 删除图片
     * @param imageUrl 图片 URL
     */
    public void deleteImage(String imageUrl) {
        try {
            String filename = extractFilenameFromUrl(imageUrl);
            if (filename != null) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filename)
                        .build());
            }
        } catch (Exception e) {
            log.error("删除图片失败", e);
            throw new RuntimeException("删除图片失败：" + e.getMessage());
        }
    }

    /**
     * 获取图片访问 URL
     * @param filename 文件名
     * @return 图片访问 URL
     */
    public String getImageUrl(String filename) {
        // 确保 endpoint 不以 / 结尾
        String baseUrl = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return baseUrl + "/" + bucketName + "/" + filename;
    }

    /**
     * 从 URL 中提取文件名
     * @param imageUrl 图片 URL
     * @return 文件名
     */
    private String extractFilenameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        try {
            // 从 URL 中提取 bucket 后面的部分
            String baseUrl = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
            String prefix = baseUrl + "/" + bucketName + "/";
            if (imageUrl.startsWith(prefix)) {
                return imageUrl.substring(prefix.length());
            }
            // 如果没有匹配前缀，尝试直接从末尾获取文件名
            return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        } catch (Exception e) {
            log.error("从 URL 提取文件名失败：{}", imageUrl, e);
            return null;
        }
    }

    /**
     * 验证图片类型
     * @param file 文件
     */
    private void validateImageType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("不支持的图片类型，允许的类型：JPEG, JPG, PNG, GIF, WEBP");
        }
    }
}
