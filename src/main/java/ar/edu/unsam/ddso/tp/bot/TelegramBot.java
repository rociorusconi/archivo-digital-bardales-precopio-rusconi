package ar.edu.unsam.ddso.tp.bot;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.description.SetMyDescription;
import org.telegram.telegrambots.meta.api.methods.description.SetMyShortDescription;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Adapter between Telegram and {@link BotCommandHandler}: receives the messages, passes the text to
 * the handler and sends back the answer. It starts with the application only when the token is
 * configured (TELEGRAM_BOT_TOKEN), in one of two modes (telegram.bot.mode):
 *
 * <ul>
 *   <li>{@code polling} (default): the application asks Telegram for new messages. Works locally,
 *       without a public URL.
 *   <li>{@code webhook}: Telegram sends each message to {@value #WEBHOOK_PATH} (see {@link
 *       TelegramWebhookController}). Needs a public HTTPS URL (telegram.bot.webhook-url), as in a
 *       deployed server, and wakes up servers that sleep when idle.
 * </ul>
 */
@Component
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

    static final String WEBHOOK_PATH = "/telegram/webhook";

    private static final Logger LOG = LoggerFactory.getLogger(TelegramBot.class);

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final BotCommandHandler commandHandler;
    private final String token;
    private final String mode;
    private final String webhookUrl;
    private final String webhookSecret;
    private TelegramBotsLongPollingApplication botsApplication;
    private TelegramClient telegramClient;

    public TelegramBot(
            BotCommandHandler commandHandler,
            @Value("${telegram.bot.token:}") String token,
            @Value("${telegram.bot.mode:polling}") String mode,
            @Value("${telegram.bot.webhook-url:}") String webhookUrl,
            @Value("${telegram.bot.webhook-secret:}") String webhookSecret) {
        this.commandHandler = commandHandler;
        this.token = token;
        this.mode = mode.trim().toLowerCase(Locale.ROOT);
        this.webhookUrl = webhookUrl.trim().replaceAll("/+$", "");
        this.webhookSecret =
                webhookSecret.isBlank() ? UUID.randomUUID().toString().replace("-", "") : webhookSecret;
    }

    // ---------- Lifecycle ----------

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (token == null || token.isBlank()) {
            LOG.warn("TELEGRAM_BOT_TOKEN is not set: the Telegram bot will not start");
            return;
        }
        if (isWebhookMode() && webhookUrl.isBlank()) {
            LOG.error("telegram.bot.mode is webhook but TELEGRAM_WEBHOOK_URL is not set: the bot will not start");
            return;
        }
        try {
            telegramClient = new OkHttpTelegramClient(token);
            registerCommandsMenu();
            registerDescription();
            if (isWebhookMode()) {
                registerWebhook();
                LOG.info("Telegram bot started (webhook at {}{})", webhookUrl, WEBHOOK_PATH);
            } else {
                botsApplication = new TelegramBotsLongPollingApplication();
                botsApplication.registerBot(token, this);
                LOG.info("Telegram bot started (long polling)");
            }
        } catch (TelegramApiException e) {
            telegramClient = null;
            LOG.error("The Telegram bot could not be started", e);
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        if (botsApplication != null) {
            botsApplication.close();
        }
    }

    // ---------- Updates ----------

    @Override
    public void consume(Update update) {
        if (telegramClient == null) {
            return;
        }
        // An edited message is answered too, so fixing a typo in a command repeats the search.
        Message received = update.hasMessage() ? update.getMessage() : update.getEditedMessage();
        if (received == null || !received.hasText()) {
            return;
        }
        String reply = commandHandler.handle(received.getText());
        if (reply.length() > MAX_MESSAGE_LENGTH) {
            reply = reply.substring(0, MAX_MESSAGE_LENGTH) + "\n…";
        }
        SendMessage answer = SendMessage.builder()
                .chatId(received.getChatId())
                .text(reply)
                .build();
        try {
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            LOG.error("Could not send the answer to chat {}", received.getChatId(), e);
        }
    }

    public boolean isFromTelegram(String secretHeader) {
        return isWebhookMode()
                && secretHeader != null
                && MessageDigest.isEqual(
                        secretHeader.getBytes(StandardCharsets.UTF_8),
                        webhookSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ---------- Private ----------

    private boolean isWebhookMode() {
        return "webhook".equals(mode);
    }

    private void registerWebhook() throws TelegramApiException {
        telegramClient.execute(SetWebhook.builder()
                .url(webhookUrl + WEBHOOK_PATH)
                .secretToken(webhookSecret)
                .allowedUpdates(List.of("message", "edited_message"))
                .build());
    }

    // Texts Telegram shows before the user presses "Start": the description in the empty chat and
    // the short description in the bot profile and in shared links.
    private void registerDescription() throws TelegramApiException {
        telegramClient.execute(SetMyDescription.builder()
                .description("Buscá piezas del Archivo Digital (cartas, fotografías, diarios…) por nombre, "
                        + "descripción, tags o fechas, y mirá sus digitalizaciones. Tocá Iniciar para empezar.")
                .build());
        telegramClient.execute(SetMyShortDescription.builder()
                .shortDescription("Buscá piezas del Archivo Digital y sus digitalizaciones.")
                .build());
    }

    // Commands shown in the Telegram menu ("/").
    private void registerCommandsMenu() throws TelegramApiException {
        telegramClient.execute(SetMyCommands.builder()
                .commands(List.of(
                        new BotCommand("buscar", "Buscar piezas (por nombre, descripción, tags o fechas)"),
                        new BotCommand("pieza", "Ver una pieza con sus digitalizaciones"),
                        new BotCommand("tags", "Ver los tags que existen"),
                        new BotCommand("ayuda", "Cómo usar el bot")))
                .build());
    }
}
