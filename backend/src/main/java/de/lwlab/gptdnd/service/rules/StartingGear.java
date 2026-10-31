package de.lwlab.gptdnd.service.rules;

import de.lwlab.gptdnd.domain.Consequence;

/** What the d20 roll at character creation grants: how many starting items and of what quality. */
public record StartingGear(int natural, int itemCount, String quality, Consequence.Tone tone) {
    public static StartingGear of(int natural) {
        if (natural == 1) {
            return new StartingGear(natural, 1, "a single nearly worthless or damaged item", Consequence.Tone.BAD);
        }
        if (natural <= 7) {
            return new StartingGear(natural, 2, "poor, worn items", Consequence.Tone.BAD);
        }
        if (natural <= 14) {
            return new StartingGear(natural, 3, "ordinary, useful items", Consequence.Tone.NEUTRAL);
        }
        if (natural <= 19) {
            return new StartingGear(natural, 4, "good, well-kept items", Consequence.Tone.GOOD);
        }
        return new StartingGear(natural, 4, "good items, one of them remarkable but not magical or overpowered", Consequence.Tone.GOOD);
    }

    public String outcome() {
        return itemCount == 1 ? "1 item" : itemCount + " items";
    }
}
