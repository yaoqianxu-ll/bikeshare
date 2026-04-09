package com.example.bickdemo.controller.admin;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.StatisticsResponse;
import com.example.bickdemo.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计接口控制器。
 * 对外暴露系统概览统计，供前台仪表盘和后台总览页复用。
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final RentalService rentalService;

    /**
     * 获取系统统计数据。
     * 返回值包含租赁总量、活跃租赁量、车辆库存概览、车型分布与热门车辆等信息。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics() {
        StatisticsResponse statistics = rentalService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
