package de.lwlab.gptdnd.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GameSessionResponse(
        String id,
        CharacterResponse character,
        String language,
        boolean images,
        boolean dead,
        int turn,
        int health,
        int maxHealth,
        String location,
        String goal,
        String layout,
        List<String> people,
        Map<String, Integer> resources,
        List<String> inventory,
        List<String> effects,
        List<String> suggestedActions,
        List<StoryNodeResponse> nodes,
        Instant createdAt) {
}
