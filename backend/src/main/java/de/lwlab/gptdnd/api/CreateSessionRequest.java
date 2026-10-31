package de.lwlab.gptdnd.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank @Size(max = 28) String name,
        @NotBlank @Size(max = 80) String concept,
        @NotBlank @Size(max = 120) String setting,
        @Pattern(regexp = "de|en") String language) {
}
