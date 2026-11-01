package de.lwlab.gptdnd.service.rules;

public enum Degree {
    CRITICAL_FAILURE("critical failure", -4, 0),
    FAILURE("failure", -3, 0),
    PARTIAL_SUCCESS("success with cost", -2, 0),
    SUCCESS("success", -1, 1),
    CRITICAL_SUCCESS("critical success", 0, 2),
    AUTOMATIC_SUCCESS("automatic success", 0, 1),
    AUTOMATIC_FAILURE("automatic failure", -1, 0);

    private static final int THREAT_HARM = 2;

    private final String label;
    private final int minHealth;
    private final int maxHealth;

    Degree(String label, int minHealth, int maxHealth) {
        this.label = label;
        this.minHealth = minHealth;
        this.maxHealth = maxHealth;
    }

    public String label() {
        return label;
    }

    /** {@code threatStrikes} allows extra harm: the threat can hurt the character even when the action went well. */
    public int clampHealth(int delta, boolean threatStrikes) {
        return Math.max(minHealth - (threatStrikes ? THREAT_HARM : 0), Math.min(maxHealth, delta));
    }

    public boolean allowsDeath() {
        return this == CRITICAL_FAILURE || this == FAILURE;
    }

    public boolean requiresCost() {
        return this == PARTIAL_SUCCESS;
    }

    public String limits(boolean threatStrikes) {
        return "healthDelta %d..%d%s%s".formatted(minHealth - (threatStrikes ? THREAT_HARM : 0), maxHealth,
                allowsDeath() ? ", characterDied may be true only if the scene is truly lethal" : ", characterDied must be false",
                requiresCost() ? ". This result must carry a real cost that the narration shows: harm, a lost item, spent coins or a lasting effect" : "");
    }

    public static Degree of(int natural, int total, int difficulty) {
        if (natural == 1) {
            return CRITICAL_FAILURE;
        }
        if (natural == 20) {
            return CRITICAL_SUCCESS;
        }
        if (total >= difficulty) {
            return SUCCESS;
        }
        return total >= difficulty - 3 ? PARTIAL_SUCCESS : FAILURE;
    }
}
