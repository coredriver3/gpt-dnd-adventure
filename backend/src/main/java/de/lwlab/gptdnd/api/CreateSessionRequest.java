package de.lwlab.gptdnd.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank @Size(max = 28) String name,
        @NotBlank @Size(max = 80) String concept,
        @NotBlank String setting) {
}
