package com.cb.th.claims.cmxuploader.config;
// SecurityConfig.java (Spring Boot 3 / Security 6)

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(c -> {
        }).csrf(c -> c.disable())
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/api/fnol/**").permitAll()
                        .anyRequest().permitAll());
        return http.build();
    }
}