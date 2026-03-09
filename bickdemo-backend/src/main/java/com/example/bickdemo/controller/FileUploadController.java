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
 * 文件上传控制器
 * 基于 MinIO 对象存储实现图片上传功能
 * @author Administrator
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final MinioService minioService;

    /**
     * 上传图片
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
     * 删除图片
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
