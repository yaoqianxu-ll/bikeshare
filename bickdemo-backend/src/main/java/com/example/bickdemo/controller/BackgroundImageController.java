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
 * 背景图接口控制器。
 * 对外提供游客可见的背景图读取接口，以及管理员可用的上传、编辑、启停接口。
 */
@RestController
@RequestMapping("/api/backgrounds")
@RequiredArgsConstructor
public class BackgroundImageController {

    private final BackgroundImageService backgroundImageService;
    private final MinioService minioService;

    /**
     * 获取当前启用的背景图列表。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BackgroundImage>>> getAllBackgrounds() {
        List<BackgroundImage> backgrounds = backgroundImageService.getAllEnabled();
        return ResponseEntity.ok(ApiResponse.success(backgrounds));
    }

    /**
     * 获取可供本地切换的背景图库，不改变系统全局启用状态。
     */
    @GetMapping("/selectable")
    public ResponseEntity<ApiResponse<List<BackgroundImage>>> getSelectableBackgrounds() {
        List<BackgroundImage> backgrounds = backgroundImageService.getAllSelectable();
        return ResponseEntity.ok(ApiResponse.success(backgrounds));
    }

    /**
     * 管理员查看全部背景图，包括未启用项。
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "背景管理", action = "获取背景列表", type = "查询")
    public ResponseEntity<ApiResponse<List<BackgroundImage>>> getAllBackgroundsAdmin() {
        List<BackgroundImage> backgrounds = backgroundImageService.getAll();
        return ResponseEntity.ok(ApiResponse.success(backgrounds));
    }

    /**
     * 根据 ID 获取背景图详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BackgroundImage>> getBackgroundById(@PathVariable Long id) {
        BackgroundImage background = backgroundImageService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(background));
    }

    /**
     * 上传背景图并创建记录，仅管理员可调用。
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "背景管理", action = "上传背景")
    public ResponseEntity<ApiResponse<BackgroundImage>> uploadBackground(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "sort", defaultValue = "0") Integer sort,
            @RequestParam(value = "type", defaultValue = "CUSTOM") String type) {
        try {
            // 先把图片上传到对象存储，再把返回 URL 落入背景图表。
            String imageUrl = minioService.uploadImage(file);

            // 新上传的背景图默认不启用，避免直接覆盖当前线上背景。
            BackgroundImage image = new BackgroundImage();
            image.setName(name != null ? name : file.getOriginalFilename());
            image.setImageUrl(imageUrl);
            image.setType(type != null ? type : "CUSTOM");
            image.setEnabled(false); // 默认不启用
            image.setSort(sort);

            BackgroundImage created = backgroundImageService.create(image);
            return ResponseEntity.ok(ApiResponse.success("上传成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("上传失败：" + e.getMessage()));
        }
    }

    /**
     * 更新背景图信息，仅管理员可调用。
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
     * 删除背景图，仅管理员可调用。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "背景管理", action = "删除背景")
    public ResponseEntity<ApiResponse<Void>> deleteBackground(@PathVariable Long id) {
        backgroundImageService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 设置某张背景图是否为全局启用项，仅管理员可调用。
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
