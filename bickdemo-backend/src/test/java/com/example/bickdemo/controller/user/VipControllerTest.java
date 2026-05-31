package com.example.bickdemo.controller.user;

import com.example.bickdemo.config.AlipayRuntimePolicy;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.VipConfirmRequest;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.VipOrder;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.VipOrderService;
import com.example.bickdemo.service.VipPlanService;
import com.example.bickdemo.service.VipService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipControllerTest {

    @Test
    void shouldRejectClientSideConfirmationWhenProdUsesSandboxFlag() {
        VipService vipService = mock(VipService.class);
        VipOrderService vipOrderService = mock(VipOrderService.class);
        VipPlanService vipPlanService = mock(VipPlanService.class);
        UserMapper userMapper = mock(UserMapper.class);

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        AlipayRuntimePolicy policy = new AlipayRuntimePolicy(environment);
        policy.setSandbox(true);

        VipController controller = new VipController(vipService, vipOrderService, vipPlanService, userMapper, policy);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("tester")
                .password("secret")
                .authorities("ROLE_USER")
                .build();

        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        when(userMapper.findByUsername("tester")).thenReturn(user);

        VipOrder order = new VipOrder()
                .setOrderNo("VIPTEST20260531")
                .setUserId(1L)
                .setPackageType("MONTHLY")
                .setStatus("PENDING");
        when(vipOrderService.getOrderByNo("VIPTEST20260531")).thenReturn(order);

        VipConfirmRequest request = new VipConfirmRequest();
        request.setOrderNo("VIPTEST20260531");

        ResponseEntity<ApiResponse<String>> response = controller.confirmPayment(userDetails, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("正式环境");
        verify(vipOrderService, never()).markOrderPaid(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
