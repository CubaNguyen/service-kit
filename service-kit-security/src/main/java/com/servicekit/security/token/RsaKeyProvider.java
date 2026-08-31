package com.servicekit.security.token;

import com.servicekit.security.config.SecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Provider phụ trách load và phân tích RSA Key Pair (Public & Private Keys)
 * phục vụ cho việc ký sinh JWT (ở chế độ FULL) hoặc verify JWT (mọi chế độ).
 */
@Slf4j
@RequiredArgsConstructor
public class RsaKeyProvider {

    private final SecurityProperties securityProperties;
    private final ResourceLoader resourceLoader;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    @PostConstruct
    public void initKeys() {
        try {
            loadPublicKey();
            if (securityProperties.getAuthMode() == SecurityProperties.AuthMode.FULL) {
                loadPrivateKey();
            }
        } catch (Exception e) {
            log.error("[RsaKeyProvider] Failed to initialize cryptographic keys", e);
            throw new IllegalStateException("RSA keys loading failed", e);
        }
    }

    public RSAPublicKey getPublicKey() {
        if (publicKey == null) {
            throw new IllegalStateException("RSA Public key is not loaded");
        }
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        if (securityProperties.getAuthMode() != SecurityProperties.AuthMode.FULL) {
            throw new UnsupportedOperationException("Private key is not available in VERIFY_ONLY mode");
        }
        if (privateKey == null) {
            throw new IllegalStateException("RSA Private key is not loaded");
        }
        return privateKey;
    }

    private void loadPublicKey() throws Exception {
        String path = securityProperties.getJwt().getPublicKeyPath();
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("JWT Public Key path is not configured");
        }

        log.info("[RsaKeyProvider] Loading RSA Public Key from path: {}", path);
        String rawPem = readResourceToString(path);
        String cleanPem = cleanPem(rawPem, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
        byte[] keyBytes = Base64.getDecoder().decode(cleanPem);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.publicKey = (RSAPublicKey) kf.generatePublic(spec);
        log.info("[RsaKeyProvider] RSA Public Key loaded successfully");
    }

    private void loadPrivateKey() throws Exception {
        String path = securityProperties.getJwt().getPrivateKeyPath();
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("JWT Private Key path is required when auth-mode is FULL");
        }

        log.info("[RsaKeyProvider] Loading RSA Private Key from path: {}", path);
        String rawPem = readResourceToString(path);
        String cleanPem = cleanPem(rawPem, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
        byte[] keyBytes = Base64.getDecoder().decode(cleanPem);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = (RSAPrivateKey) kf.generatePrivate(spec);
        log.info("[RsaKeyProvider] RSA Private Key loaded successfully");
    }

    private String readResourceToString(String location) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream is = resource.getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
    }

    private String cleanPem(String raw, String beginHeader, String endHeader) {
        return raw.replace(beginHeader, "")
                .replace(endHeader, "")
                .replaceAll("\\s", "");
    }
}
