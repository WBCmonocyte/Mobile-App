package com.easysport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MailjetSender implements EmailVerificationSender {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper json;
    private final String apiKey;
    private final String secret;
    private final String sender;

    public MailjetSender(ObjectMapper json,
                         @Value("${MAILJET_API_KEY:}") String apiKey,
                         @Value("${MAILJET_SECRET_KEY:}") String secret,
                         @Value("${MAILJET_SENDER_EMAIL:}") String sender) {
        this.json = json;
        this.apiKey = apiKey;
        this.secret = secret;
        this.sender = sender;
    }

    public void sendRegistrationCode(String email, String code) {
        if (apiKey.isBlank() || secret.isBlank() || sender.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Hệ thống chưa cấu hình gửi email.");
        }
        try {
            String basic = Base64.getEncoder().encodeToString((apiKey + ":" + secret).getBytes(StandardCharsets.UTF_8));
            String body = json.writeValueAsString(Map.of("Messages", List.of(Map.of(
                    "From", Map.of("Email", sender, "Name", "EasySport"),
                    "To", List.of(Map.of("Email", email)),
                    "Subject", "EasySport - Mã xác nhận đăng ký",
                    "TextPart", "Mã xác nhận EasySport của bạn là " + code + ". Mã có hiệu lực trong 5 phút."
            ))));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mailjet.com/v3.1/send"))
                    .timeout(Duration.ofSeconds(7))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) {
                JsonNode messages = json.readTree(response.body()).path("Messages");
                if ("success".equals(messages.path(0).path("Status").asText())) {
                    return;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // Provider response details may include recipient data.
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không gửi được mã xác nhận. Vui lòng thử lại.");
    }
}
