package com.fluffytime.config;

import com.fluffytime.auth.jwt.exception.CustomAuthenticationEntryPoint;
import com.fluffytime.auth.jwt.filter.JwtAuthenticationFilter;
import com.fluffytime.auth.jwt.util.JwtTokenizer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final JwtTokenizer jwtTokenizer;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login",
                    "/join/**",
                    "/api/users/**",
                    "/api/auth/**",
                    "/error",
                    "/static/**",
                    "/js/**",
                    "/css/**",
                    "/image/**"
                ).permitAll()
                .anyRequest().authenticated()

            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenizer),
                UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form.disable())
            .sessionManagement(
                sessionManagement -> sessionManagement.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling( exception ->
                exception
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedPage("/error.html")
            )
        ;

        return http.build();
    }

    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedMethod("*");
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "PATCH", "OPTION", "PUT"));
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
