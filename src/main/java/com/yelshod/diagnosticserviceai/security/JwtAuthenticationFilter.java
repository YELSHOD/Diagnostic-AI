package com.yelshod.diagnosticserviceai.security;

import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.UserRepository;
import com.yelshod.diagnosticserviceai.auth.JwtService;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7);
            String subject = jwtService.extractSubject(token);
            UserEntity user = userRepository.findById(java.util.UUID.fromString(subject))
                    .orElse(null);
            if (user == null || !jwtService.isTokenValid(token, user.getId().toString())) {
                filterChain.doFilter(request, response);
                return;
            }

            var authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                    .collect(Collectors.toSet());
            var authentication = new UsernamePasswordAuthenticationToken(user.getId().toString(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }
}
