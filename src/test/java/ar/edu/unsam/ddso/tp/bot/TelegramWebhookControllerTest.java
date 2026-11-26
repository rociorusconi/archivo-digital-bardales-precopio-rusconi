package ar.edu.unsam.ddso.tp.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.telegram.telegrambots.meta.api.objects.Update;

@ExtendWith(MockitoExtension.class)
class TelegramWebhookControllerTest {

    private static final String UPDATE_JSON =
            """
            {"update_id": 7, "message": {"message_id": 1, "date": 1700000000,
             "chat": {"id": 42, "type": "private"}, "text": "/buscar carta"}}""";

    @Mock private TelegramBot telegramBot;

    private TelegramWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new TelegramWebhookController(telegramBot);
    }

    @Test
    void updateWithTheRightSecretIsPassedToTheBot() {
        // Given
        when(telegramBot.isFromTelegram("secret")).thenReturn(true);

        // When
        HttpStatus status = HttpStatus.valueOf(controller.receive("secret", UPDATE_JSON).getStatusCode().value());

        // Then
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(telegramBot).consume(update.capture());
        assertEquals(HttpStatus.OK, status);
        assertEquals("/buscar carta", update.getValue().getMessage().getText());
    }

    @Test
    void callWithoutTheSecretIsRejectedAndNotAnswered() {
        // Given
        when(telegramBot.isFromTelegram(null)).thenReturn(false);

        // When
        HttpStatus status = HttpStatus.valueOf(controller.receive(null, UPDATE_JSON).getStatusCode().value());

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, status);
        verify(telegramBot, never()).consume(any(Update.class));
    }
}
