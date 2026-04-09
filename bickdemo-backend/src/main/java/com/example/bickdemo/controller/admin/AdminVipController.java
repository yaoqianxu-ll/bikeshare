package com.example.bickdemo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.entity.VipBenefit;
import com.example.bickdemo.mapper.VipBenefitMapper;
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
        vipService.grantVip(userId, days, experience);
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
}