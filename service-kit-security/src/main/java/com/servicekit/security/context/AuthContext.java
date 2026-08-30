package com.servicekit.security.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthContext implements Serializable {

    private String userId;
    private String email;
    private String username;
    private List<String> roles;
    private List<String> permissions;

    private String token;
    private String siteKey; // Tenant Identifier
    private String correlationId;
    private String ipAddress;
    private String userAgent;
    private String platform;
    private String environment;
}
