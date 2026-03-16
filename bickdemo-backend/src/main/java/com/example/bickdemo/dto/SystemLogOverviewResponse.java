package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogOverviewResponse {

    private Long totalUserCount;

    private Long totalPostCount;

    private Long totalVisitCount;

    private Long todayVisitCount;

    private Long blacklistCount;

    private Long todayLoginCount;

    private Long todayLoginFailCount;

    private Long todayOperationCount;

    private Long todayOperationFailCount;
}
