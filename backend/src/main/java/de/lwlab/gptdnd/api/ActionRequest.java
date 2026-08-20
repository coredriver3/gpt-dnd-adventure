package de.lwlab.gptdnd.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActionRequest(@NotBlank @Size(max = 500) String text) {
}
