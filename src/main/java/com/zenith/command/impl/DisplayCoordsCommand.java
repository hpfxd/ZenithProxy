package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DisplayCoordsCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "displayCoords",
            CommandCategory.MANAGE,
            "Sets whether proxy status commands should display coordinates. Only usable by account owner(s).",
            asList("on/off"),
            asList("coords")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("displayCoords").requires(Command::validateAccountOwner)
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.reportCoords = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Coordinates " + (CONFIG.discord.reportCoords ? "On!" : "Off!"))
                    .color(Color.CYAN);
                return 1;
            }));
    }
}
