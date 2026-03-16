package com.example.bickdemo.controller;

import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.entity.BackgroundImage;
import com.example.bickdemo.service.BackgroundImageService;
import com.example.bickdemo.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 背景图片控制器
 */
@RestController
@RequestMapping("/api/backgrounds")
@RequiredArgsConstructor
public class BackgroundImageController {

    private final BackgroundImageService backgroundImageService;
    private final MinioService minioService;

    /**
     * 获取所有启用的背景图片
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BackgroundImage>>> getAllBackgrounds() {
        List<BackgroundImage> backgrounds = backgroundImageService.getAllEnabled();
        return ResponseEntity.ok(ApiResponse.success(backgrounds));
    }

    /**
     * 获取所有可选择的背景图片（游客/普通用户，用于本地选择；不改变全局 enabled）
     */
    @GetMapping("/selectable")
    public ResponseEntity<ApiResponse<List<BackgroundImage>>> getSelectableBackgrounds() {
        List<BackgroundImage> backgrounds = backgroundImageService.getAllSelectable();
        return ResponseEntity.ok(ApiResponse.success(backgrounds));
    }

    /**
     * 获取所有背景图片（管理员，包含未启用的）
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "背景管理", action = "获取背景列表", type = "查询")
    public ResponseEntity<ApiResponse<List<BackgroundImage>>> getAllBackgroundsAdmin() {
        List<BackgroundImage> backgrounds = backgroundImageService.getAll();
        return ResponseEntity.ok(ApiResponse.success(backgrounds));
    }

    /**
     * 根据 ID 获取背景图片
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BackgroundImage>> getBackgroundById(@PathVariable Long id) {
        BackgroundImage background = backgroundImageService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(background));
    }

    /**
     * 上传背景图片并创建（仅管理员）
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "背景管理", action = "上传背景")
    public ResponseEntity<ApiResponse<BackgroundImage>> uploadBackground(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "sort", defaultValue = "0") Integer sort) {
        try {
            // 上传图片到 MinIO
            String imageUrl = minioService.uploadImage(file);

            // 创建背景图片记录
            BackgroundImage image = new BackgroundImage();
            image.setName(name != null ? name : file.getOriginalFilename());
            image.setImageUrl(imageUrl);
            image.setType("CUSTOM");
            image.setEnabled(false); // 默认不启用
            image.setSort(sort);

            BackgroundImage created = backgroundImageService.create(image);
            return ResponseEntity.ok(ApiResponse.success("上传成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("上传失败：" + e.getMessage()));
        }
    }

    /**
     * 更新背景图片（仅管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "背景管理", action = "编辑背景")
    public ResponseEntity<ApiResponse<BackgroundImage>> updateBackground(
            @PathVariable Long id,
            @RequestBody BackgroundImage image) {
        BackgroundImage updated = backgroundImageService.update(id, image);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    /**
     * 删除背景图片（仅管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "背景管理", action = "删除背景")
    public ResponseEntity<ApiResponse<Void>> deleteBackground(@PathVariable Long id) {
        backgroundImageService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 设置启用的背景图片（仅管理员）
     */
    @PostMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "背景管理", action = "切换背景启用状态")
    public ResponseEntity<ApiResponse<Void>> setEnabledBackground(
            @PathVariable Long id,
            @RequestParam Boolean enabled) {
        backgroundImageService.setEnabled(id, enabled);
        return ResponseEntity.ok(ApiResponse.success("设置成功", null));
    }
}
