package de.lwlab.gptdnd.domain;

/** The kind of story moment a turn should deliver, chosen by the backend to keep the pacing varied. */
public enum Beat {
    DISCOVERY,
    COMPLICATION,
    ENCOUNTER,
    BREATHER,
    REVELATION
}
