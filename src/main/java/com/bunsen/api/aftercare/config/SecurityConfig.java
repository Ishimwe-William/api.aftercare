package com.bunsen.api.aftercare.config;

import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.security.jwt.AuthTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;

    public SecurityConfig(AuthTokenFilter authTokenFilter) {
        this.authTokenFilter = authTokenFilter;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers("/api/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/roles/**").hasRole("ADMIN")

                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/bikes/**").hasAnyRole("ADMIN", "TECHNICIAN", "STAFF")

                        .requestMatchers("/api/service-cases/**").hasAnyRole("ADMIN", "TECHNICIAN", "CUSTOMER")
                        .requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "TECHNICIAN")
                        .requestMatchers("/api/parts/**").hasAnyRole("ADMIN", "TECHNICIAN", "STAFF")

                        .requestMatchers("/api/invoices/**").hasAnyRole("ADMIN", "TECHNICIAN", "CUSTOMER")
                        .requestMatchers("/api/activity-logs/**").hasAnyRole("ADMIN", "TECHNICIAN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}