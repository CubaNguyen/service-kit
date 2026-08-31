package com.servicekit.security.config;

import com.servicekit.security.token.JwtTokenProvider;
import com.servicekit.security.token.RsaKeyProvider;
import com.servicekit.security.token.TokenClaimsCustomizer;
import com.servicekit.security.token.TokenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

/**
 * Auto-Configuration nạp các bean liên quan đến mã hóa RSA và JWT Token Provider.
 */
@Configuration
public class TokenProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RsaKeyProvider rsaKeyProvider(SecurityProperties securityProperties, ResourceLoader resourceLoader) {
        return new RsaKeyProvider(securityProperties, resourceLoader);
    }

    @Bean
    @ConditionalOnMissingBean(TokenProvider.class)
    public TokenProvider jwtTokenProvider(
            SecurityProperties securityProperties,
            RsaKeyProvider rsaKeyProvider,
            List<TokenClaimsCustomizer> customizers) {
        return new JwtTokenProvider(securityProperties, rsaKeyProvider, customizers);
    }
}
