package com.example.bickdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 支付宝运行环境策略
 * 统一处理正式环境与沙箱环境的支付行为边界，避免生产环境误走沙箱分支。
 */
@Component
public class AlipayRuntimePolicy {

    private final Environment environment;

    @Value("${alipay.sandbox:true}")
    private boolean sandbox;

    public AlipayRuntimePolicy(Environment environment) {
        this.environment = environment;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public boolean isProdProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    }

    public boolean isProductionSandboxMisconfigured() {
        return isProdProfileActive() && sandbox;
    }

    public boolean allowSandboxFallback() {
        return sandbox && !isProdProfileActive();
    }

    public boolean allowClientSideConfirmation() {
        return allowSandboxFallback();
    }

    public void assertServerSidePaymentFlowAllowed() {
        if (isProductionSandboxMisconfigured()) {
            throw new IllegalStateException("生产环境禁止使用支付宝沙箱网关，请将 ALIPAY_SANDBOX 设置为 false");
        }
    }

    public void assertRealGatewayCredentialsConfigured(String appId, String privateKey, String alipayPublicKey) {
        if (allowSandboxFallback()) {
            return;
        }

        if (!StringUtils.hasText(appId) || !StringUtils.hasText(privateKey) || !StringUtils.hasText(alipayPublicKey)) {
            throw new IllegalStateException("支付宝正式环境配置不完整，请检查 ALIPAY_APP_ID、ALIPAY_PRIVATE_KEY 和 ALIPAY_PUBLIC_KEY");
        }
    }

    public String describeRuntimeMode() {
        if (isProductionSandboxMisconfigured()) {
            return "生产环境误配沙箱";
        }
        return allowSandboxFallback() ? "沙箱联调" : "正式支付";
    }

    /**
     * 供测试直接设置运行标志，运行时仍由 Spring 配置注入覆盖。
     */
    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }
}
