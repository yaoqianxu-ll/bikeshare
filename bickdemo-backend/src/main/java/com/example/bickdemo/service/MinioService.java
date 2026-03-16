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
 * MinIO 对象存储服务。
 * 负责处理头像、背景图、论坛配图等图片资源的上传和删除，并统一做文件类型与大小校验。
 *
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

    /**
     * 系统允许上传的图片 MIME 类型。
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    /**
     * 上传图片大小上限，防止超大文件占用对象存储和网络带宽。
     */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传图片到 MinIO。
     * 上传前会做文件类型和大小校验，上传成功后返回可直接访问的 URL。
     *
     * @param file 图片文件
     * @return 图片访问 URL
     */
    public String uploadImage(MultipartFile file) {
        try {
            // 先校验 MIME 类型，避免把非图片文件当成资源存入对象存储。
            validateImageType(file);

            // 再校验文件大小，限制单文件上传体积。
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("图片大小不能超过 5MB");
            }

            // 用 UUID 生成对象名，避免原始文件名冲突。
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 文件流直接写入 MinIO，不在本地磁盘落临时副本。
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 返回完整访问 URL，前端后续可以直接把这个地址写入业务表字段。
            return getImageUrl(filename);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            throw new RuntimeException("上传图片失败：" + e.getMessage());
        }
    }

    /**
     * 根据图片 URL 删除对应对象。
     *
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
     * 按对象名拼接出完整访问地址。
     *
     * @param filename 文件名
     * @return 图片访问 URL
     */
    public String getImageUrl(String filename) {
        // endpoint 配置可能带末尾斜杠，这里做一次标准化，避免 URL 出现双斜杠。
        String baseUrl = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return baseUrl + "/" + bucketName + "/" + filename;
    }

    /**
     * 从图片 URL 中提取对象名。
     *
     * @param imageUrl 图片 URL
     * @return 文件名
     */
    private String extractFilenameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        try {
            // 优先按当前 MinIO endpoint + bucket 的前缀提取对象名。
            String baseUrl = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
            String prefix = baseUrl + "/" + bucketName + "/";
            if (imageUrl.startsWith(prefix)) {
                return imageUrl.substring(prefix.length());
            }
            // 如果 URL 不是标准前缀格式，则退化为取最后一个 / 之后的部分。
            return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        } catch (Exception e) {
            log.error("从 URL 提取文件名失败：{}", imageUrl, e);
            return null;
        }
    }

    /**
     * 校验上传文件是否属于允许的图片类型。
     *
     * @param file 文件
     */
    private void validateImageType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("不支持的图片类型，允许的类型：JPEG, JPG, PNG, GIF, WEBP");
        }
    }
}
