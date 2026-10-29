package com.ibmteam02.backend_consultation.consultation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ibmteam02.backend_consultation.consultation.domain.ConsultationSession;
import com.ibmteam02.backend_consultation.consultation.repository.ConsultationSessionRepository;
import com.ibmteam02.backend_consultation.global.auth.JwtProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ConsultationSessionRepository consultationSessionRepository;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    @Test
    void CONNECT_유효한_토큰이면_사용자_정보를_세션에_저장한다() {
        given(jwtProvider.validateToken("valid")).willReturn(true);
        given(jwtProvider.getUserIdFromToken("valid")).willReturn(1L);
        given(jwtProvider.getRoleFromToken("valid")).willReturn("USER");

        Message<?> result = interceptor.preSend(connectMessage("Bearer valid"), null);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(StompAuthChannelInterceptor.getUserId(accessor.getUser())).isEqualTo(1L);
        assertThat(StompAuthChannelInterceptor.getRole(accessor.getUser())).isEqualTo("USER");
    }

    @Test
    void CONNECT_토큰이_없으면_연결을_거부한다() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void CONNECT_유효하지_않은_토큰이면_연결을_거부한다() {
        given(jwtProvider.validateToken("invalid")).willReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer invalid"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void SUBSCRIBE_상담방_참여자는_구독할_수_있다() {
        given(consultationSessionRepository.findById(10L)).willReturn(Optional.of(matchedSession(1L, 2L)));

        Message<?> message = subscribeMessage("/topic/room/10", 2L);

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    @Test
    void SUBSCRIBE_다른_사람의_상담방은_구독할_수_없다() {
        given(consultationSessionRepository.findById(10L)).willReturn(Optional.of(matchedSession(1L, 2L)));

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/topic/chat/room/10", 99L), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private ConsultationSession matchedSession(Long customerId, Long pharmacistId) {
        ConsultationSession session = ConsultationSession.createSession(customerId);
        session.matchPharmacist(pharmacistId);
        return session;
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(String destination, Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("USER"))));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
