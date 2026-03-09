package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.StatisticsResponse;
import com.example.bickdemo.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计接口控制器
 * 提供系统统计数据的查询接口
 * @author Administrator
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final RentalService rentalService;

    /**
     * 获取统计数据
     * 包括总租赁数、活跃租赁数、可用自行车数等
     */
    @GetMapping
    public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics() {
        StatisticsResponse statistics = rentalService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
