package de.lwlab.gptdnd.service.story;

import de.lwlab.gptdnd.domain.CharacterProfile;
import de.lwlab.gptdnd.domain.Language;
import org.springframework.stereotype.Component;

@Component
public class LocalStoryProvider implements StoryProvider {
    @Override
    public StoryDraft intro(StoryPrompt prompt) {
        CharacterProfile character = prompt.session().character();
        String text = prompt.session().language() == Language.DE
                ? "%s, %s, erreicht %s. Die Luft schmeckt nach Regen und Metall. Deine Vorräte sind knapp, und jede falsche Entscheidung kann bleibende Folgen haben."
                        .formatted(character.name(), character.concept(), prompt.place())
                : "%s, %s, reaches %s. The air tastes of rain and metal. Supplies are thin, and bad decisions can leave permanent scars."
                        .formatted(character.name(), character.concept(), prompt.place());
        return new StoryDraft(text, "%s, %s, cinematic adventure concept art"
                .formatted(character.setting().id(), prompt.place()));
    }

    @Override
    public StoryDraft action(StoryPrompt prompt) {
        String description = describe(prompt.outcome().grade(), prompt.session().language());
        String deathLine = prompt.session().dead()
                ? prompt.session().language() == Language.DE
                        ? " Das ist kein Rückschlag mehr: Deine Geschichte endet hier."
                        : " This is no longer a setback: your story ends here."
                : "";
        String text = prompt.session().language() == Language.DE
                ? "Du willst: \"%s\". In %s kippt die Lage in Richtung %s. Gute Würfe öffnen nicht automatisch Schatzkammern; schlechte Würfe schaffen Probleme, die zur Szene passen. %s%s"
                        .formatted(prompt.action(), prompt.place(), prompt.outcome().grade(), description, deathLine)
                : "You attempt: \"%s\". At %s, the situation turns into a %s. Good rolls do not automatically create treasure; bad rolls create problems that fit the scene. %s%s"
                        .formatted(prompt.action(), prompt.place(), prompt.outcome().grade(), description, deathLine);
        return new StoryDraft(text, "%s, %s, player action: %s, %s, cinematic fantasy"
                .formatted(prompt.session().character().setting().id(), prompt.place(), prompt.action(), prompt.outcome().grade()));
    }

    private String describe(String grade, Language language) {
        if (language == Language.EN) {
            return switch (grade) {
                case "critical failure" -> "You get what you wanted only if you bleed for it; otherwise the world takes something important.";
                case "failure" -> "The attempt fails and forces you into a worse position.";
                case "mixed result" -> "You make progress, but pay for it immediately.";
                case "success with cost" -> "You reach your goal, yet the situation stays dangerous.";
                case "clean success" -> "You clearly prevail and take control of the moment.";
                case "critical success" -> "You dominate the scene, but the reward stays plausible.";
                default -> "You gather detail without escalating the situation directly.";
            };
        }
        return switch (grade) {
            case "critical failure" -> "Du bekommst, was du wolltest, nur wenn du dafür blutest; sonst nimmt dir die Welt etwas Wichtiges.";
            case "failure" -> "Der Versuch scheitert und zwingt dich in eine schlechtere Position.";
            case "mixed result" -> "Du machst Fortschritt, aber bezahlst sofort dafür.";
            case "success with cost" -> "Du erreichst dein Ziel, doch die Situation bleibt gefährlich.";
            case "clean success" -> "Du setzt dich klar durch und gewinnst Kontrolle über den Moment.";
            case "critical success" -> "Du dominierst die Szene, aber die Belohnung bleibt plausibel.";
            default -> "Du sammelst Details, ohne die Lage direkt zu eskalieren.";
        };
    }
}
