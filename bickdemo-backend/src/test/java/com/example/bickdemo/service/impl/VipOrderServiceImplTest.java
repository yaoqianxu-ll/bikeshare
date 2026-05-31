package com.example.bickdemo.service.impl;

import com.example.bickdemo.mapper.VipOrderMapper;
import com.example.bickdemo.service.VipPlanService;
import com.example.bickdemo.service.VipService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VipOrderServiceImplTest {

    @Test
    void shouldEscapeAmpersandsInsideAlipayFormAction() {
        VipOrderServiceImpl service = new VipOrderServiceImpl(
                mock(VipOrderMapper.class),
                mock(VipService.class),
                mock(VipPlanService.class),
                mock(RabbitTemplate.class)
        );

        String rawForm = """
                <form name="punchout_form" method="post" action="https://openapi-sandbox.dl.alipaydev.com/gateway.do?sign_type=RSA2&timestamp=2026-05-31+20%3A46%3A17&version=1.0">
                  <input type="hidden" name="biz_content" value="{}">
                </form>
                """;

        String normalized = ReflectionTestUtils.invokeMethod(service, "normalizeAlipayFormAction", rawForm);

        assertThat(normalized)
                .contains("sign_type=RSA2&amp;timestamp=2026-05-31+20%3A46%3A17&amp;version=1.0")
                .doesNotContain("sign_type=RSA2&timestamp=2026-05-31+20%3A46%3A17&version=1.0");
    }
}
