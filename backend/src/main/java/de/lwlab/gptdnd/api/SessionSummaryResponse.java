package de.lwlab.gptdnd.api;

import java.time.Instant;

public record SessionSummaryResponse(
        String id,
        String name,
        String concept,
        SettingResponse setting,
        String location,
        int turn,
        int health,
        int maxHealth,
        boolean dead,
        Instant createdAt,
        Instant lastPlayedAt) {
}
