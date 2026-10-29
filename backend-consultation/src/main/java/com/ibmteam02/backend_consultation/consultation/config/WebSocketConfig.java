package com.ibmteam02.backend_consultation.consultation.config;

import com.ibmteam02.backend_consultation.global.config.AllowedOrigins;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker //웹소캣 서버 활성화 , STOMP 프로토콜을 사용하겠다는 선언
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry stompEndpointRegistry) {
        //프론트엔드에서 localhost:8082/ws-consult 연결 요청
        stompEndpointRegistry.addEndpoint("/api/ws-stomp")
                .setAllowedOriginPatterns(AllowedOrigins.PATTERNS.toArray(String[]::new)) // CORS와 동일한 출처만 허용
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        //클라이언트가 보내는 STOMP 메시지(CONNECT, SUBSCRIBE 등)에 인증 인터셉터 적용
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry) {
        //메세지 받을 때 ex./topic/room/12 같은 주소 구독 , 해당 메세지 실시간 수신
        messageBrokerRegistry.enableSimpleBroker("/topic");

        //메세지 보낼 때 (/app/chat/message)
        messageBrokerRegistry.setApplicationDestinationPrefixes("/app");
    }
}
