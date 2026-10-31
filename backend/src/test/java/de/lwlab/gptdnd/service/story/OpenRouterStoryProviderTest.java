package de.lwlab.gptdnd.service.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.lwlab.gptdnd.domain.CharacterProfile;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.service.rules.StartingGear;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenRouterStoryProviderTest {
    private MockRestServiceServer server;
    private OpenRouterStoryProvider provider;
    private final GameSession session = new GameSession("s1",
            new CharacterProfile("Mara", "smuggler", "dark-fantasy"), Instant.now());

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openrouter.test/api/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OpenRouterStoryProvider(builder.build(), "nvidia/nemotron-3.5-super-vl:free", new GameMasterPrompts(40));
    }

    @Test
    void sendsSchemaAndParsesFencedJson() {
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("nvidia/nemotron-3.5-super-vl:free"))
                .andExpect(jsonPath("$.response_format.json_schema.schema.required").isArray())
                .andRespond(withSuccess(completion("```json\\n{\\\"valid\\\": true, \\\"rejection\\\": \\\"\\\", "
                        + "\\\"rollNeeded\\\": true, \\\"skill\\\": \\\"Stealth\\\", \\\"difficulty\\\": 14, \\\"modifier\\\": 2}\\n```"),
                        MediaType.APPLICATION_JSON));

        ActionPlan plan = provider.plan(session, "I sneak past the guard");

        assertThat(plan.rollNeeded()).isTrue();
        assertThat(plan.skill()).isEqualTo("Stealth");
        assertThat(plan.difficulty()).isEqualTo(14);
        server.verify();
    }

    @Test
    void retriesOnceOnMalformedJson() {
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andRespond(withSuccess(completion("{not json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andRespond(withSuccess(completion("{\\\"narration\\\": \\\"You arrive.\\\", \\\"location\\\": \\\"Mill\\\", "
                        + "\\\"suggestedActions\\\": [\\\"a\\\"], \\\"imagePrompt\\\": \\\"mill\\\"}"), MediaType.APPLICATION_JSON));

        SceneIntro intro = provider.intro(session, StartingGear.of(10));

        assertThat(intro.location()).isEqualTo("Mill");
        server.verify();
    }

    @Test
    void reportsRateLimits() {
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.intro(session, StartingGear.of(10)))
                .isInstanceOf(StoryProviderException.class)
                .hasMessageContaining("rate limit");
    }

    private static String completion(String content) {
        return "{\"choices\": [{\"message\": {\"role\": \"assistant\", \"content\": \"" + content + "\"}}]}";
    }
}
