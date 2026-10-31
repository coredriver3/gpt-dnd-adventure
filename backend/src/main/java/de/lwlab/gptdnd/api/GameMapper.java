package de.lwlab.gptdnd.api;

import de.lwlab.gptdnd.domain.Consequence;
import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.domain.GameSetting;
import de.lwlab.gptdnd.domain.StoryNode;

final class GameMapper {
    private GameMapper() {
    }

    static SettingResponse setting(GameSetting setting) {
        return new SettingResponse(setting.id(), setting.label());
    }

    private static SettingResponse setting(String setting) {
        return new SettingResponse(setting, GameSetting.label(setting));
    }

    static GameSessionResponse session(GameSession session) {
        return new GameSessionResponse(
                session.id(),
                new CharacterResponse(
                        session.character().name(),
                        session.character().concept(),
                        setting(session.character().setting())),
                session.language().name().toLowerCase(),
                session.images(),
                session.dead(),
                session.turn(),
                session.health(),
                session.maxHealth(),
                session.resolve(),
                session.maxResolve(),
                session.location(),
                session.resources(),
                session.inventory(),
                session.effects(),
                session.suggestedActions(),
                session.nodes().stream().map(GameMapper::node).toList(),
                session.createdAt());
    }

    static SessionSummaryResponse summary(GameSession session) {
        return new SessionSummaryResponse(
                session.id(),
                session.character().name(),
                session.character().concept(),
                setting(session.character().setting()),
                session.location(),
                session.turn(),
                session.health(),
                session.maxHealth(),
                session.dead(),
                session.createdAt(),
                session.lastPlayedAt());
    }

    private static StoryNodeResponse node(StoryNode node) {
        return new StoryNodeResponse(
                node.id(),
                node.type(),
                node.action(),
                node.text(),
                node.rollText(),
                node.roll() == null ? null : new DiceRollResponse(
                        node.roll().label(),
                        node.roll().natural(),
                        node.roll().modifier(),
                        node.roll().difficulty(),
                        node.roll().outcome(),
                        node.roll().tone().name().toLowerCase()),
                node.imagePrompt(),
                node.consequences().stream().map(GameMapper::consequence).toList(),
                node.createdAt());
    }

    private static ConsequenceResponse consequence(Consequence consequence) {
        return new ConsequenceResponse(consequence.text(), consequence.tone().name().toLowerCase());
    }
}
