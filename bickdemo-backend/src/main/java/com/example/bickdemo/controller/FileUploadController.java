package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传接口。
 * 当前主要承担富文本、头像、背景图等图片文件的上传与删除能力。
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final MinioService minioService;

    /**
     * 上传图片到 MinIO，并返回前端可直接使用的访问 URL。
     *
     * @param file 图片文件
     * @return 图片访问 URL
     */
    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = minioService.uploadImage(file);
            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);
            return ResponseEntity.ok(ApiResponse.success("上传成功", result));
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body(ApiResponse.error(500, "上传失败：" + e.getMessage()));
        }
    }

    /**
     * 根据图片 URL 删除对象存储中的文件。
     *
     * @param imageUrl 图片 URL
     * @return 删除结果
     */
    @DeleteMapping("/delete-image")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @RequestParam("imageUrl") String imageUrl) {
        try {
            minioService.deleteImage(imageUrl);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body(ApiResponse.error(500, "删除失败：" + e.getMessage()));
        }
    }
}
