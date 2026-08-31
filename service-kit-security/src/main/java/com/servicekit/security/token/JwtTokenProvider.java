package com.servicekit.security.token;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.servicekit.security.config.SecurityProperties;
import com.servicekit.security.context.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;

/**
 * Triển khai {@link TokenProvider} sử dụng JSON Web Token (JWT) mã hóa bất đối xứng RS256.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {

    private final SecurityProperties securityProperties;
    private final RsaKeyProvider rsaKeyProvider;
    private final List<TokenClaimsCustomizer> customizers;

    private static final String ROLES_CLAIM = "roles";
    private static final String PERMISSIONS_CLAIM = "permissions";
    private static final String TENANT_ID_CLAIM = "tenantId";

    @Override
    public String generateToken(AuthContext context) {
        if (securityProperties.getAuthMode() != SecurityProperties.AuthMode.FULL) {
            throw new UnsupportedOperationException("Token generation is disabled in VERIFY_ONLY mode");
        }

        try {
            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(securityProperties.getJwt().getExpirationSeconds());
            String jti = context.jti() != null ? context.jti() : UUID.randomUUID().toString();

            // 1. Build standard JWT claims set
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(context.userId().toString())
                    .issueTime(Date.from(context.issuedAt() != null ? context.issuedAt() : now))
                    .expirationTime(Date.from(expiry))
                    .jwtID(jti);

            if (context.roles() != null) {
                claimsBuilder.claim(ROLES_CLAIM, new ArrayList<>(context.roles()));
            }
            if (context.permissions() != null) {
                claimsBuilder.claim(PERMISSIONS_CLAIM, new ArrayList<>(context.permissions()));
            }
            if (context.tenantId() != null) {
                claimsBuilder.claim(TENANT_ID_CLAIM, context.tenantId());
            }

            // 2. Apply custom claims via registered customizers
            Map<String, Object> claimsMap = new HashMap<>(claimsBuilder.build().getClaims());
            if (customizers != null) {
                for (TokenClaimsCustomizer customizer : customizers) {
                    customizer.customize(claimsMap, context);
                }
            }

            // Rebuild claims set with custom claims included
            JWTClaimsSet.Builder finalClaimsBuilder = new JWTClaimsSet.Builder();
            for (Map.Entry<String, Object> entry : claimsMap.entrySet()) {
                finalClaimsBuilder.claim(entry.getKey(), entry.getValue());
            }
            JWTClaimsSet finalClaims = finalClaimsBuilder.build();

            // 3. Create JWS header and sign with private key
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
            SignedJWT signedJWT = new SignedJWT(header, finalClaims);
            JWSSigner signer = new RSASSASigner(rsaKeyProvider.getPrivateKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("[JwtTokenProvider] Failed to generate JWT token", e);
            throw new IllegalStateException("JWT generation failed", e);
        }
    }

    @Override
    public AuthContext parseToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(rsaKeyProvider.getPublicKey());

            if (!signedJWT.verify(verifier)) {
                throw new IllegalArgumentException("JWT signature verification failed");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
                throw new IllegalArgumentException("JWT token has expired");
            }

            UUID userId = UUID.fromString(claims.getSubject());
            String tenantId = claims.getStringClaim(TENANT_ID_CLAIM);

            List<String> rawRoles = claims.getStringListClaim(ROLES_CLAIM);
            Set<String> roles = rawRoles != null ? new HashSet<>(rawRoles) : Collections.emptySet();

            List<String> rawPermissions = claims.getStringListClaim(PERMISSIONS_CLAIM);
            Set<String> permissions = rawPermissions != null ? new HashSet<>(rawPermissions) : Collections.emptySet();

            String jti = claims.getJWTID();
            Instant issuedAt = claims.getIssueTime() != null ? claims.getIssueTime().toInstant() : null;

            return new AuthContext(userId, tenantId, roles, permissions, jti, issuedAt);
        } catch (Exception e) {
            log.debug("[JwtTokenProvider] Token parsing failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(rsaKeyProvider.getPublicKey());
            return signedJWT.verify(verifier) && signedJWT.getJWTClaimsSet().getExpirationTime().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
