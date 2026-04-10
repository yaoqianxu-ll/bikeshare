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

    @Value("${alipay.app-id:}")
    private String appId;

    @Value("${alipay.private-key:}")
    private String privateKey;

    @Value("${alipay.alipay-public-key:}")
    private String alipayPublicKey;

    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    @Value("${alipay.sandbox:false}")
    private boolean sandbox;

    @PostConstruct
    public void init() {
        try {
            Config config = new Config();
            config.protocol = "https";
            config.gatewayHost = sandbox ? "openapi-sandbox.dl.alipaydev.com" : "openapi.alipay.com";
            config.signType = "RSA2";
            config.appId = appId;
            config.merchantPrivateKey = privateKey;
            config.alipayPublicKey = alipayPublicKey;
            config.notifyUrl = notifyUrl;

            Factory.setOptions(config);
            log.info("支付宝SDK初始化完成，运行环境: {}", sandbox ? "沙箱环境" : "生产环境");
        } catch (Exception e) {
            log.error("支付宝SDK初始化失败: {}", e.getMessage());
        }
    }
}
