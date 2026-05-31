package com.example.bickdemo.service.impl;

import com.example.bickdemo.config.AlipayRuntimePolicy;
import com.example.bickdemo.entity.VipOrder;
import com.example.bickdemo.mapper.VipOrderMapper;
import com.example.bickdemo.service.VipPlanService;
import com.example.bickdemo.service.VipService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class VipOrderServiceImplTest {

    @Test
    void shouldReturnSimulatedSandboxLinkWhenLocalSandboxMissingCredentials() {
        VipOrderServiceImpl service = createService(false, true);

        Map<String, Object> result = service.generatePayUrl(createOrder());

        assertThat(result.get("isHtml")).isEqualTo(false);
        assertThat((String) result.get("payUrl")).contains("openapi-sandbox.dl.alipaydev.com");
    }

    @Test
    void shouldThrowWhenProdSandboxAttemptsToUseFallback() {
        VipOrderServiceImpl service = createService(true, true);

        assertThatThrownBy(() -> service.generatePayUrl(createOrder()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALIPAY_SANDBOX");
    }

    @Test
    void shouldThrowWhenRealGatewayCredentialsAreMissing() {
        VipOrderServiceImpl service = createService(true, false);

        assertThatThrownBy(() -> service.generatePayUrl(createOrder()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALIPAY_APP_ID");
    }

    private VipOrderServiceImpl createService(boolean prodProfile, boolean sandbox) {
        MockEnvironment environment = new MockEnvironment();
        if (prodProfile) {
            environment.setActiveProfiles("prod");
        }

        AlipayRuntimePolicy policy = new AlipayRuntimePolicy(environment);
        policy.setSandbox(sandbox);

        VipOrderServiceImpl service = new VipOrderServiceImpl(
                mock(VipOrderMapper.class),
                mock(VipService.class),
                mock(VipPlanService.class),
                mock(RabbitTemplate.class),
                policy
        );

        ReflectionTestUtils.setField(service, "alipayAppId", "");
        ReflectionTestUtils.setField(service, "alipayPrivateKey", "");
        ReflectionTestUtils.setField(service, "alipayPublicKey", "");
        ReflectionTestUtils.setField(service, "returnUrl", "https://bikeshare.online/points");
        return service;
    }

    private VipOrder createOrder() {
        return new VipOrder()
                .setOrderNo("VIPTEST20260531")
                .setPackageType("MONTHLY")
                .setAmount(new BigDecimal("9.90"));
    }
}
