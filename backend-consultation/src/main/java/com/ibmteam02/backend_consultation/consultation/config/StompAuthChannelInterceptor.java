package com.ibmteam02.backend_consultation.consultation.config;

import com.ibmteam02.backend_consultation.consultation.repository.ConsultationSessionRepository;
import com.ibmteam02.backend_consultation.global.auth.JwtProvider;
import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

// STOMP 메시지가 컨트롤러에 도달하기 전에 인증/인가를 처리
// CONNECT: JWT 검증 후 세션에 사용자 정보 저장
// SUBSCRIBE: 상담방 참여자만 해당 방 구독 가능
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/(?:chat/)?room/(\\d+)$");

    private final JwtProvider jwtProvider;
    private final ConsultationSessionRepository consultationSessionRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            checkRoomSubscription(accessor);
        }

        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("인증 토큰이 없습니다");
        }

        String token = bearerToken.substring(BEARER_PREFIX.length());
        if (!jwtProvider.validateToken(token)) {
            throw new AccessDeniedException("유효하지 않은 토큰입니다");
        }

        Long userId = jwtProvider.getUserIdFromToken(token);
        String role = jwtProvider.getRoleFromToken(token);

        return new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority(role)));
    }

    private void checkRoomSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = ROOM_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        Long roomId = Long.valueOf(matcher.group(1));
        Long userId = getUserId(accessor.getUser());

        boolean isParticipant = consultationSessionRepository.findById(roomId)
                .map(session -> session.isParticipant(userId))
                .orElse(false);

        if (!isParticipant) {
            log.warn("상담방 구독 거부 - 방번호: {}, 사용자ID: {}", roomId, userId);
            throw new AccessDeniedException("참여 중인 상담방만 구독할 수 있습니다");
        }
    }

    public static Long getUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new AccessDeniedException("인증되지 않은 사용자입니다");
    }

    public static String getRole(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication) {
            return authentication.getAuthorities().stream()
                    .findFirst()
                    .map(authority -> authority.getAuthority())
                    .orElse(null);
        }
        throw new AccessDeniedException("인증되지 않은 사용자입니다");
    }
}
