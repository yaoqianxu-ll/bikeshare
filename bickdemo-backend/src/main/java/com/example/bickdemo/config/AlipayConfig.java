package com.example.bickdemo.config;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 支付宝配置
 */
@Slf4j
@Configuration
public class AlipayConfig {

    private final AlipayRuntimePolicy runtimePolicy;

    public AlipayConfig(AlipayRuntimePolicy runtimePolicy) {
        this.runtimePolicy = runtimePolicy;
    }

    @Value("${alipay.app-id:}")
    private String appId;

    @Value("${alipay.private-key:}")
    private String privateKey;

    @Value("${alipay.alipay-public-key:}")
    private String alipayPublicKey;

    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    @PostConstruct
    public void init() {
        try {
            Config config = new Config();
            config.protocol = "https";
            config.gatewayHost = runtimePolicy.isSandbox() ? "openapi-sandbox.dl.alipaydev.com" : "openapi.alipay.com";
            config.signType = "RSA2";
            config.appId = appId;
            config.merchantPrivateKey = privateKey;
            config.alipayPublicKey = alipayPublicKey;
            config.notifyUrl = notifyUrl;

            Factory.setOptions(config);
            if (runtimePolicy.isProductionSandboxMisconfigured()) {
                log.error("检测到 prod 环境仍启用支付宝沙箱，请立即将 ALIPAY_SANDBOX 设置为 false");
            }
            log.info("支付宝SDK初始化完成，运行模式: {}, gatewayHost={}",
                    runtimePolicy.describeRuntimeMode(), config.gatewayHost);
        } catch (Exception e) {
            log.error("支付宝SDK初始化失败: {}", e.getMessage());
        }
    }
}
