package com.insipidknight.toarrive.push;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    private static final Long USER_ID = 1L;
    private static final WsMessage MESSAGE =
            new WsMessage("ORDER_ACCEPTED", "TA202601011200000001", "ACCEPTED", null);

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private WebSocketSession session;

    private PushService pushService;

    @BeforeEach
    void setUp() {
        pushService = new PushService(sessionRegistry, JsonMapper.builder().build());
    }

    @Test
    @DisplayName("对端无会话 → 静默跳过，不抛异常")
    void noSession_doesNotThrow() {
        when(sessionRegistry.get(USER_ID)).thenReturn(null);

        assertThatCode(() -> pushService.sendToUser(USER_ID, MESSAGE)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("会话已关闭 → 不发送")
    void closedSession_skips() throws Exception {
        when(sessionRegistry.get(USER_ID)).thenReturn(session);
        when(session.isOpen()).thenReturn(false);

        pushService.sendToUser(USER_ID, MESSAGE);

        verify(session, never()).sendMessage(any());
    }

    @Test
    @DisplayName("会话在线 → 发送 JSON 载荷")
    void openSession_sendsJson() throws Exception {
        when(sessionRegistry.get(USER_ID)).thenReturn(session);
        when(session.isOpen()).thenReturn(true);

        pushService.sendToUser(USER_ID, MESSAGE);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload())
                .contains("ORDER_ACCEPTED")
                .contains("TA202601011200000001")
                .contains("ACCEPTED");
    }
}
