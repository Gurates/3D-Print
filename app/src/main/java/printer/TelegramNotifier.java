package printer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TelegramNotifier {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void sendMessage(String message) {
        // YENİ: Ayarları anlık olarak SQLite'dan çekiyoruz
        String botToken = DatabaseManager.getSetting("telegram_bot_token", "");
        String chatId = DatabaseManager.getSetting("telegram_chat_id", "");

        if (botToken.isEmpty() || chatId.isEmpty()) {
            System.out.println("[Telegram] Ayarlar eksik, mesaj gönderilmedi (Ayarlar panelini kontrol edin).");
            return;
        }

        try {
            String encodedMessage = java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
            String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + encodedMessage + "&parse_mode=HTML";

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            System.out.println("[Telegram Hata] Bildirim gönderilemedi. Kod: " + response.statusCode());
                        } else {
                            System.out.println("[Telegram] Bildirim başarıyla iletildi!");
                        }
                    });

        } catch (Exception e) {
            System.out.println("[Telegram Kritik Hata] " + e.getMessage());
        }
    }
}