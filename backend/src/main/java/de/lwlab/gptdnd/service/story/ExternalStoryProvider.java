package de.lwlab.gptdnd.service.story;

import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExternalStoryProvider {
    private final HttpClient httpClient;
    private final String endpoint;
    private final String apiKey;

    public ExternalStoryProvider(
            @Value("${adventure.ai.endpoint:}") String endpoint,
            @Value("${adventure.ai.api-key:}") String apiKey) {
        this.httpClient = HttpClient.newHttpClient();
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public boolean configured() {
        return !endpoint.isBlank() && !apiKey.isBlank();
    }

    public HttpClient httpClient() {
        return httpClient;
    }

    public String endpoint() {
        return endpoint;
    }
}
