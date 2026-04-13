package com.yelshod.diagnosticserviceai.ws;

import com.yelshod.diagnosticserviceai.auth.JwtService;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null || token.isBlank()) {
            log.warn("Websocket auth rejected reason=missing-token uri={}", request.getURI().getPath());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String subject = jwtService.extractSubject(token);
            Optional<UserEntity> user = userRepository.findById(UUID.fromString(subject));
            if (user.isEmpty() || !jwtService.isTokenValid(token, subject)) {
                log.warn("Websocket auth rejected reason=invalid-token userId={}", subject);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            attributes.put("userId", subject);
            log.info("Websocket auth accepted userId={}", subject);
            return true;
        } catch (ExpiredJwtException ex) {
            String expiredAt = ex.getClaims() != null && ex.getClaims().getExpiration() != null
                    ? ex.getClaims().getExpiration().toInstant().toString()
                    : "unknown";
            log.warn("Websocket auth rejected reason=token-expired uri={} expiredAt={}",
                    request.getURI().getPath(), expiredAt);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Websocket auth rejected reason=token-parse-failed uri={} message={}",
                    request.getURI().getPath(), ex.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}
