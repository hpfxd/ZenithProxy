package com.zenith.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.entity.RestChannel;
import lombok.Data;

@Data
public class CommandContext {
    private final EmbedCreateSpec.Builder embedBuilder;
    private final MessageCreateEvent messageCreateEvent;
    private final CommandManager commandManager;
    private final RestChannel restChannel;
}