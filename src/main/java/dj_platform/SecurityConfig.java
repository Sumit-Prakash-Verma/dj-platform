
package dj_platform;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .securityContext(context -> context
                        .securityContextRepository(
                                securityContextRepository()))

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) -> response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Authentication required")))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/style.css",
                                "/script.js",
                                "/api/rooms",
                                "/api/auth/**")
                        .permitAll()

                        .requestMatchers("/api/memberships/**")
                        .authenticated()

                        .anyRequest().permitAll())

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}