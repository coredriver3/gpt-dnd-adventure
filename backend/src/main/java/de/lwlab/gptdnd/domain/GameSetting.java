package de.lwlab.gptdnd.domain;

import java.util.List;

public enum GameSetting {
    DARK_FANTASY(
            "dark-fantasy",
            "Dark fantasy borderlands",
            List.of(
                    "a drowned chapel whose bell rings under the marsh water",
                    "a toll bridge held by deserters with clean boots and dirty blades",
                    "a lantern-lit crypt where the walls remember names",
                    "a ruined watchtower leaning over a road of black glass")),
    ARCANE_CITY(
            "arcane-city",
            "Arcane city intrigue",
            List.of(
                    "a masked auction beneath the old observatory",
                    "a canal district where reflections answer before people speak",
                    "a locked archive guarded by contract-bound statues",
                    "a rooftop chase across copper rain collectors")),
    POST_EMPIRE(
            "post-empire",
            "Post empire ruins",
            List.of(
                    "a collapsed imperial relay station humming with leftover commands",
                    "a salt flat crossed by broken war machines",
                    "a village built inside the ribs of a dead skyship",
                    "a sealed bunker with fresh footprints in the dust")),
    GERMAN_FOLKLORE(
            "german-folklore",
            "German folklore woods",
            List.of(
                    "a fir forest where every path returns to the same shrine",
                    "a millpond that shows tomorrow's injuries",
                    "a charcoal burner who trades in secrets instead of coin",
                    "a hilltop ruin where the moonlight has weight"));

    private final String id;
    private final String label;
    private final List<String> places;

    GameSetting(String id, String label, List<String> places) {
        this.id = id;
        this.label = label;
        this.places = places;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public List<String> places() {
        return places;
    }

    public static GameSetting fromId(String id) {
        for (GameSetting setting : values()) {
            if (setting.id.equals(id)) {
                return setting;
            }
        }
        throw new IllegalArgumentException("Unknown setting: " + id);
    }
}
