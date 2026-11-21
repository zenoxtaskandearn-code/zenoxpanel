package com.gallery.platform.websocket;

import com.gallery.platform.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final List<String> allowedOrigins;

    public WebSocketConfig(JwtService jwtService, @Value("${app.websocket.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.jwtService = jwtService;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.isEmpty() ? new String[]{"http://localhost:5173"} : allowedOrigins.toArray(new String[0]);
        registry.addEndpoint("/ws").setAllowedOriginPatterns(origins);
        registry.addEndpoint("/ws").setAllowedOriginPatterns(origins).withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String auth = accessor.getFirstNativeHeader("Authorization");
                    if (!StringUtils.hasText(auth)) auth = accessor.getFirstNativeHeader("authorization");
                    String token = null;
                    if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) token = auth.substring(7);
                    else if (StringUtils.hasText(accessor.getFirstNativeHeader("token"))) token = accessor.getFirstNativeHeader("token");
                    if (token == null || !jwtService.isValid(token)) {
                        throw new IllegalArgumentException("Unauthorized WebSocket connection");
                    }
                    Claims claims = jwtService.parse(token);
                    String typ = claims.get("typ", String.class);
                    String principalName;
                    if ("ADMIN".equals(typ)) {
                        principalName = "admin:" + claims.get("aid");
                    } else if ("DEVICE".equals(typ)) {
                        principalName = "device:" + claims.get("did");
                    } else {
                        throw new IllegalArgumentException("Unauthorized WebSocket connection");
                    }
                    final String name = principalName;
                    accessor.setUser(() -> name);
                }
                return message;
            }
        });
    }
}
