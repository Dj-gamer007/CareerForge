package com.careerforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "careerforge.jwt")
public class JwtConfigProperties {

    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;
}
