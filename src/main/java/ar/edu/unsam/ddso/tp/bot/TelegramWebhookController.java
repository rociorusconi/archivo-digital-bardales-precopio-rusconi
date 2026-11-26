package ar.edu.unsam.ddso.tp.bot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Entry point of the bot in webhook mode: Telegram sends each message here. Calls without the
 * secret given to Telegram (or with the bot in polling mode) are rejected with 401.
 */
@RestController
public class TelegramWebhookController {

    private static final ObjectMapper TELEGRAM_JSON =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final TelegramBot telegramBot;

    public TelegramWebhookController(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @PostMapping(TelegramBot.WEBHOOK_PATH)
    public ResponseEntity<Void> receive(
            @RequestHeader(name = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody String body) {
        if (!telegramBot.isFromTelegram(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Update update;
        try {
            update = TELEGRAM_JSON.readValue(body, Update.class);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }
        telegramBot.consume(update);
        return ResponseEntity.ok().build();
    }
}
