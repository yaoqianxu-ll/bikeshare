package com.example.bickdemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class AlipayRuntimePolicyTest {

    @Test
    void shouldRejectSandboxFallbackWhenProdProfileIsActive() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        AlipayRuntimePolicy policy = new AlipayRuntimePolicy(environment);
        policy.setSandbox(true);

        assertThat(policy.allowSandboxFallback()).isFalse();
        assertThat(policy.allowClientSideConfirmation()).isFalse();
        assertThat(policy.isProductionSandboxMisconfigured()).isTrue();
    }

    @Test
    void shouldAllowSandboxFallbackOutsideProd() {
        MockEnvironment environment = new MockEnvironment();

        AlipayRuntimePolicy policy = new AlipayRuntimePolicy(environment);
        policy.setSandbox(true);

        assertThat(policy.allowSandboxFallback()).isTrue();
        assertThat(policy.allowClientSideConfirmation()).isTrue();
        assertThat(policy.isProductionSandboxMisconfigured()).isFalse();
    }
}
