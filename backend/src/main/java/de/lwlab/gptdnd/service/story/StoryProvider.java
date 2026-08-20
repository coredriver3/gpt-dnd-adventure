package de.lwlab.gptdnd.service.story;

public interface StoryProvider {
    StoryDraft intro(StoryPrompt prompt);

    StoryDraft action(StoryPrompt prompt);
}
