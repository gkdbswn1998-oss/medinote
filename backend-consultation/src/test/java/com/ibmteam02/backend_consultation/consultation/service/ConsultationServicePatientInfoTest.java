package com.ibmteam02.backend_consultation.consultation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ibmteam02.backend_consultation.consultation.domain.ConsultationSession;
import com.ibmteam02.backend_consultation.consultation.dto.PatientInfoResponse;
import com.ibmteam02.backend_consultation.consultation.repository.ConsultationSessionRepository;
import com.ibmteam02.backend_consultation.global.auth.AuthUserClient;
import com.ibmteam02.backend_consultation.medication.client.MedicationClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ConsultationServicePatientInfoTest {

    private static final Long ROOM_ID = 10L;
    private static final Long CUSTOMER_ID = 1L;
    private static final Long ASSIGNED_PHARMACIST_ID = 2L;

    @Mock
    private ConsultationSessionRepository consultationSessionRepository;

    @Mock
    private AuthUserClient authUserClient;

    @Mock
    private MedicationClient medicationClient;

    @InjectMocks
    private ConsultationService consultationService;

    @Test
    void 담당_약사는_환자_정보를_조회할_수_있다() {
        given(consultationSessionRepository.findById(ROOM_ID)).willReturn(Optional.of(matchedSession()));
        given(authUserClient.getPatientInfo(CUSTOMER_ID))
                .willReturn(PatientInfoResponse.builder().username("환자").build());

        PatientInfoResponse response =
                consultationService.getPatientInfoByRoomId(ROOM_ID, ASSIGNED_PHARMACIST_ID, "PHARMACIST");

        assertThat(response.getUsername()).isEqualTo("환자");
    }

    @Test
    void 담당이_아닌_약사는_환자_정보를_조회할_수_없다() {
        given(consultationSessionRepository.findById(ROOM_ID)).willReturn(Optional.of(matchedSession()));

        assertThatThrownBy(() -> consultationService.getPatientInfoByRoomId(ROOM_ID, 99L, "PHARMACIST"))
                .isInstanceOf(AccessDeniedException.class);

        // 권한이 없으면 다른 서버로 의료 정보를 요청하지 않아야 한다
        verify(authUserClient, never()).getPatientInfo(anyLong());
        verify(medicationClient, never()).getPatientSchedules(anyLong());
    }

    @Test
    void 약사가_아니면_환자_정보를_조회할_수_없다() {
        assertThatThrownBy(() -> consultationService.getPatientInfoByRoomId(ROOM_ID, CUSTOMER_ID, "USER"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void 상담방_참여자가_아니면_메시지를_보낼_수_없다() {
        given(consultationSessionRepository.findById(ROOM_ID)).willReturn(Optional.of(matchedSession()));

        assertThatThrownBy(() -> consultationService.validateParticipant(ROOM_ID, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private ConsultationSession matchedSession() {
        ConsultationSession session = ConsultationSession.createSession(CUSTOMER_ID);
        session.matchPharmacist(ASSIGNED_PHARMACIST_ID);
        return session;
    }
}
