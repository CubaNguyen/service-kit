package com.servicekit.security.token;

import com.servicekit.security.context.AuthContext;

/**
 * Lớp giữ chỗ cho việc triển khai PASETO Token Provider trong tương lai.
 */
public class PasetoTokenProvider implements TokenProvider {

    @Override
    public String generateToken(AuthContext context) {
        throw new UnsupportedOperationException("PASETO token support is not implemented yet");
    }

    @Override
    public AuthContext parseToken(String token) {
        throw new UnsupportedOperationException("PASETO token support is not implemented yet");
    }

    @Override
    public boolean validateToken(String token) {
        throw new UnsupportedOperationException("PASETO token support is not implemented yet");
    }
}
