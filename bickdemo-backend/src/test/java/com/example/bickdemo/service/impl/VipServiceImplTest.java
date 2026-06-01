package com.example.bickdemo.service.impl;

import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.VipMember;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.mapper.VipBenefitMapper;
import com.example.bickdemo.service.AdminNotificationPublisher;
import com.example.bickdemo.service.PointsService;
import com.example.bickdemo.service.VipMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VipServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private VipBenefitMapper vipBenefitMapper;

    @Mock
    private PointsService pointsService;

    @Mock
    private AdminNotificationPublisher adminNotificationPublisher;

    @Mock
    private VipMemberService vipMemberService;

    @InjectMocks
    private VipServiceImpl vipService;

    @Test
    void shouldActivateVipForPaidOrderInsteadOfOverwritingExistingVip() {
        User user = new User();
        user.setId(1L);
        user.setExperiencePoints(100);

        VipMember vipMember = new VipMember();
        vipMember.setUserId(1L);
        vipMember.setLastOrderNo("ORDER_OLD");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(vipMemberService.getVipMemberByUserId(1L)).thenReturn(vipMember);

        vipService.grantVip(1L, 30, 50, "ORDER_NEW");

        assertThat(user.getExperiencePoints()).isEqualTo(150);
        assertThat(user.getVipLevel()).isEqualTo(2);
        verify(userMapper).updateById(user);
        verify(vipMemberService).activateVip(1L, "ORDER_NEW", 30);
        verify(vipMemberService, never()).overwriteVip(anyLong(), anyInt(), nullable(String.class));
    }

    @Test
    void shouldSkipPaidOrderGrantWhenOrderAlreadyApplied() {
        User user = new User();
        user.setId(1L);
        user.setExperiencePoints(200);

        VipMember vipMember = new VipMember();
        vipMember.setUserId(1L);
        vipMember.setLastOrderNo("ORDER_DUPLICATE");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(vipMemberService.getVipMemberByUserId(1L)).thenReturn(vipMember);

        vipService.grantVip(1L, 30, 50, "ORDER_DUPLICATE");

        assertThat(user.getExperiencePoints()).isEqualTo(200);
        verify(userMapper, never()).updateById(any(User.class));
        verify(vipMemberService, never()).activateVip(anyLong(), anyString(), anyInt());
        verify(vipMemberService, never()).overwriteVip(anyLong(), anyInt(), nullable(String.class));
    }

    @Test
    void shouldKeepOverwriteGrantForAdminScenarioWithoutOrderNo() {
        User user = new User();
        user.setId(1L);
        user.setExperiencePoints(10);

        when(userMapper.selectById(1L)).thenReturn(user);

        vipService.grantVip(1L, 15, 20, null);

        assertThat(user.getExperiencePoints()).isEqualTo(30);
        verify(userMapper).updateById(user);
        verify(vipMemberService).overwriteVip(1L, 15, null);
        verify(vipMemberService, never()).activateVip(anyLong(), anyString(), anyInt());
    }
}
