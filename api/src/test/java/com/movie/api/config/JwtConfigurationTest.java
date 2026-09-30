package com.movie.api.config;

import com.movie.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JwtTokenUtil.class);

    @Test
    void missingAndWeakExternalSecretsFailStartup() {
        contextRunner.run(context -> assertThat(context).hasFailed());
        contextRunner.withPropertyValues("jwt.secret=short")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void configuredExternalSecretStartsNormally() {
        contextRunner.withPropertyValues("jwt.secret=test-only-signing-key-with-at-least-32-bytes")
                .run(context -> assertThat(context).hasSingleBean(JwtTokenUtil.class));
    }
}
