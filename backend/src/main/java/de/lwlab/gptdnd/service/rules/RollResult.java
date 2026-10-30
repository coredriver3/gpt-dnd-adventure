package de.lwlab.gptdnd.service.rules;

public record RollResult(String skill, int natural, int modifier, int difficulty, Degree degree) {
    public int total() {
        return natural + modifier;
    }

    public String describe() {
        String mod = modifier == 0 ? "" : modifier > 0 ? " +" + modifier : " " + modifier;
        return "%s: d20 %d%s = %d vs DC %d / %s".formatted(skill, natural, mod, total(), difficulty, degree.label());
    }
}
