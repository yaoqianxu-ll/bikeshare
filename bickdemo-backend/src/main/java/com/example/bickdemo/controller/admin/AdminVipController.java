package com.example.bickdemo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.vo.VipAdminDashboardVo;
import com.example.bickdemo.dto.VipAdminMemberAdjustDto;
import com.example.bickdemo.dto.VipAdminMemberDetailDto;
import com.example.bickdemo.dto.VipAdminMemberPageDto;
import com.example.bickdemo.dto.VipAdminOrderPageDto;
import com.example.bickdemo.entity.VipBenefit;
import com.example.bickdemo.entity.VipPlan;
import com.example.bickdemo.mapper.VipBenefitMapper;
import com.example.bickdemo.service.VipAdminService;
import com.example.bickdemo.service.VipPlanService;
import com.example.bickdemo.service.VipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/vip")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVipController {

    private final VipService vipService;
    private final VipBenefitMapper vipBenefitMapper;
    private final VipAdminService vipAdminService;
    private final VipPlanService vipPlanService;

    /**
     * 发放VIP
     */
    @PostMapping("/grant")
    @AdminOperationLog(module = "VIP管理", action = "发放VIP", type = "管理")
    public ResponseEntity<ApiResponse<String>> grantVip(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer days = Integer.valueOf(params.get("days").toString());
        Integer experience = params.get("experience") != null
                ? Integer.valueOf(params.get("experience").toString()) : null;
        vipService.grantVip(userId, days, experience, null); // 管理员发放没有订单号
        return ResponseEntity.ok(ApiResponse.success("VIP发放成功"));
    }

    /**
     * 撤销VIP
     */
    @PostMapping("/revoke")
    @AdminOperationLog(module = "VIP管理", action = "撤销VIP", type = "管理")
    public ResponseEntity<ApiResponse<String>> revokeVip(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        vipService.revokeVip(userId);
        return ResponseEntity.ok(ApiResponse.success("VIP撤销成功"));
    }

    /**
     * 获取VIP权益列表
     */
    @GetMapping("/benefits")
    @AdminOperationLog(module = "VIP管理", action = "查看权益列表", type = "查询")
    public ResponseEntity<ApiResponse<List<VipBenefit>>> getBenefits() {
        List<VipBenefit> benefits = vipBenefitMapper.selectList(null);
        return ResponseEntity.ok(ApiResponse.success(benefits));
    }

    /**
     * 更新VIP权益状态
     */
    @PutMapping("/benefits")
    @AdminOperationLog(module = "VIP管理", action = "更新权益状态", type = "管理")
    public ResponseEntity<ApiResponse<String>> updateBenefit(@RequestBody VipBenefit benefit) {
        LambdaUpdateWrapper<VipBenefit> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(VipBenefit::getId, benefit.getId())
                .set(VipBenefit::getBenefitName, benefit.getBenefitName())
                .set(VipBenefit::getDescription, benefit.getDescription())
                .set(VipBenefit::getIsActive, benefit.getIsActive());
        vipBenefitMapper.update(null, wrapper);
        return ResponseEntity.ok(ApiResponse.success("权益更新成功"));
    }

    /**
     * 新增VIP权益
     */
    @PostMapping("/benefits")
    @AdminOperationLog(module = "VIP管理", action = "新增权益", type = "管理")
    public ResponseEntity<ApiResponse<String>> createBenefit(@RequestBody VipBenefit benefit) {
        VipBenefit newBenefit = new VipBenefit();
        newBenefit.setBenefitKey(benefit.getBenefitKey());
        newBenefit.setBenefitName(benefit.getBenefitName());
        newBenefit.setDescription(benefit.getDescription());
        newBenefit.setIsActive(benefit.getIsActive() != null ? benefit.getIsActive() : true);
        vipBenefitMapper.insert(newBenefit);
        return ResponseEntity.ok(ApiResponse.success("权益新增成功"));
    }

    /**
     * 删除VIP权益
     */
    @DeleteMapping("/benefits/{id}")
    @AdminOperationLog(module = "VIP管理", action = "删除权益", type = "管理")
    public ResponseEntity<ApiResponse<String>> deleteBenefit(@PathVariable Long id) {
        vipBenefitMapper.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("权益删除成功"));
    }

    // ==================== VIP管理端新接口 ====================

    /**
     * 获取VIP仪表盘统计
     */
    @GetMapping("/dashboard")
    @AdminOperationLog(module = "VIP管理", action = "查看仪表盘", type = "查询")
    public ResponseEntity<ApiResponse<VipAdminDashboardVo>> getDashboard() {
        VipAdminDashboardVo dashboard = vipAdminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * 分页查询VIP会员列表
     */
    @GetMapping("/members")
    @AdminOperationLog(module = "VIP管理", action = "查看会员列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<VipAdminMemberDetailDto>>> pageMembers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        VipAdminMemberPageDto dto = new VipAdminMemberPageDto();
        dto.setPage(page);
        dto.setSize(size);
        dto.setKeyword(keyword);
        dto.setStatus(status);
        Page<VipAdminMemberDetailDto> result = vipAdminService.pageMembers(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取会员详情
     */
    @GetMapping("/members/{userId}")
    @AdminOperationLog(module = "VIP管理", action = "查看会员详情", type = "查询")
    public ResponseEntity<ApiResponse<VipAdminMemberDetailDto>> getMemberDetail(@PathVariable Long userId) {
        VipAdminMemberDetailDto detail = vipAdminService.getMemberDetail(userId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * 调整会员状态
     */
    @PostMapping("/members/adjust")
    @AdminOperationLog(module = "VIP管理", action = "调整会员状态", type = "管理")
    public ResponseEntity<ApiResponse<String>> adjustMember(@RequestBody VipAdminMemberAdjustDto dto) {
        vipAdminService.adjustMember(dto);
        return ResponseEntity.ok(ApiResponse.success("调整成功"));
    }

    /**
     * 分页查询VIP订单列表
     */
    @GetMapping("/orders")
    @AdminOperationLog(module = "VIP管理", action = "查看订单列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<?>>> pageOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String userKeyword,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String status) {
        VipAdminOrderPageDto dto = new VipAdminOrderPageDto();
        dto.setPage(page);
        dto.setSize(size);
        dto.setOrderNo(orderNo);
        dto.setUserKeyword(userKeyword);
        dto.setPlanCode(planCode);
        dto.setStatus(status);
        Page<?> result = vipAdminService.pageOrders(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有套餐列表
     */
    @GetMapping("/plans")
    @AdminOperationLog(module = "VIP管理", action = "查看套餐列表", type = "查询")
    public ResponseEntity<ApiResponse<List<Object>>> listPlans() {
        List<Object> plans = vipAdminService.listPlans();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    /**
     * 更新套餐
     */
    @PutMapping("/plans/{id}")
    @AdminOperationLog(module = "VIP管理", action = "更新套餐", type = "管理")
    public ResponseEntity<ApiResponse<String>> updatePlan(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        String name = (String) params.get("name");
        Integer days = params.get("days") != null ? Integer.valueOf(params.get("days").toString()) : null;
        Integer priceFen = params.get("priceFen") != null ? Integer.valueOf(params.get("priceFen").toString()) : null;
        Boolean enabled = params.get("enabled") != null ? Boolean.valueOf(params.get("enabled").toString()) : null;
        String description = (String) params.get("description");

        vipAdminService.updatePlan(id, name, days, priceFen, enabled, description);
        return ResponseEntity.ok(ApiResponse.success("套餐更新成功"));
    }
}