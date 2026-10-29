package com.ibmteam02.backend_consultation.consultation.controller;

import com.ibmteam02.backend_consultation.consultation.config.StompAuthChannelInterceptor;
import com.ibmteam02.backend_consultation.consultation.domain.SenderType;
import com.ibmteam02.backend_consultation.consultation.dto.ChatMessageDto;
import com.ibmteam02.backend_consultation.consultation.service.ConsultationService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessageSendingOperations simpMessageSendingOperations;
    private final ConsultationService consultationService;

    //메세지 발송(Publish) (최종 주소: /app/consult/message)
    @MessageMapping("/consult/message")
    public void message(ChatMessageDto chatMessageDto, Principal principal){

        // 보낸 사람 정보는 클라이언트 값이 아닌 CONNECT 시 검증된 토큰 기준으로 설정 (발신자 위조 방지)
        Long senderId = StompAuthChannelInterceptor.getUserId(principal);
        String role = StompAuthChannelInterceptor.getRole(principal);
        consultationService.validateParticipant(chatMessageDto.getRoomId(), senderId);

        chatMessageDto = chatMessageDto.toBuilder()
                .senderId(senderId)
                .senderType(toSenderType(role))
                .build();

        if(ChatMessageDto.MessageType.ENTER.equals(chatMessageDto.getType())) {
            chatMessageDto = chatMessageDto.toBuilder()
                    .message(senderId + "님이 1:1 상담방에 입장하셨습니다.")
                    .build();
        } else if (ChatMessageDto.MessageType.TALK.equals(chatMessageDto.getType())) {
            // TALK 메시지일 경우에만 DB 저장
            consultationService.saveMessage(chatMessageDto);
        }

        //(최종 주소: /topic/room/{roomId})
        simpMessageSendingOperations.convertAndSend("/topic/room/" + chatMessageDto.getRoomId(), chatMessageDto);
    }

    private SenderType toSenderType(String role) {
        if ("PHARMACIST".equals(role) || "ROLE_PHARMACIST".equals(role)) {
            return SenderType.PHARMACIST;
        }
        return SenderType.USER;
    }
}
