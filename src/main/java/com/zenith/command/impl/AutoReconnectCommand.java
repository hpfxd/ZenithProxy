package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoReconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "autoReconnect",
            CommandCategory.MODULE,
            "Configure the AutoReconnect feature",
            asList("on/off", "delay <seconds>", "maxAttempts <number>"));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoReconnect")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoReconnect.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("AutoReconnect " + (CONFIG.client.extra.autoReconnect.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("delay")
                      .then(argument("delaySec", integer(0, 1000)).executes(c -> {
                          CONFIG.client.extra.autoReconnect.delaySeconds = IntegerArgumentType.getInteger(c, "delaySec");
                          c.getSource().getEmbed()
                              .title("AutoReconnect Delay Updated!");
                          return 1;
                      })))
            .then(literal("maxAttempts")
                      .then(argument("maxAttempts", integer(1)).executes(c -> {
                          CONFIG.client.extra.autoReconnect.maxAttempts = IntegerArgumentType.getInteger(c, "maxAttempts");
                          c.getSource().getEmbed()
                              .title("AutoReconnect Max Attempts Updated!");
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("AutoReconnect", toggleStr(CONFIG.client.extra.autoReconnect.enabled), false)
            .addField("Delay", CONFIG.client.extra.autoReconnect.delaySeconds, true)
            .addField("Max Attempts", CONFIG.client.extra.autoReconnect.maxAttempts, true)
            .color(Color.CYAN);
    }
}
