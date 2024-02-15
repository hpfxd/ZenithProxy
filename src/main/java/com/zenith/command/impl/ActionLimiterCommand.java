package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.ActionLimiter;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ActionLimiterCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "actionLimiter",
            CommandCategory.MODULE,
            "Limits player actions",
            asList(
                "on/off",
                "allowMovement on/off",
                "movementDistance <distance>",
                "movementHome <x> <z>",
                "movementMinY <y>",
                "allowInventory on/off",
                "allowBlockBreaking on/off",
                "allowInteract on/off",
                "allowEnderChest on/off",
                "allowUseItem on/off",
                "allowChat on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("actionLimiter").requires(Command::validateAccountOwner)
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.get(ActionLimiter.class).syncEnabledFromConfig();
                return 1;
            }))
            .then(literal("allowMovement").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowMovement = getToggle(c, "toggle");
                return 1;
            })))
            .then(literal("movementDistance").then(argument("distance", integer(0)).executes(c -> {
                CONFIG.client.extra.actionLimiter.movementDistance = getInteger(c, "distance");
                return 1;
            })))
            .then(literal("movementHome").then(argument("x", integer()).then(argument("z", integer()).executes(c -> {
                CONFIG.client.extra.actionLimiter.movementHomeX = getInteger(c, "x");
                CONFIG.client.extra.actionLimiter.movementHomeZ = getInteger(c, "z");
                return 1;
            }))))
            .then(literal("movementMinY").then(argument("y", integer(-64, 400)).executes(c -> {
                CONFIG.client.extra.actionLimiter.movementMinY = getInteger(c, "y");
                return 1;
            })))
            .then(literal("allowInventory").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowInventory = getToggle(c, "toggle");
                return 1;
            })))
            .then(literal("allowBlockBreaking").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowBlockBreaking = getToggle(c, "toggle");
                return 1;
            })))
            .then(literal("allowInteract").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowInteract = getToggle(c, "toggle");
                return 1;
            })))
            .then(literal("allowEnderChest").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowEnderChest = getToggle(c, "toggle");
                return 1;
            })))
            .then(literal("allowUseItem").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowUseItem = getToggle(c, "toggle");
                return 1;
            })))
            .then(literal("allowChat").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowChat = getToggle(c, "toggle");
                return 1;
            })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .title("Action Limiter")
            .addField("Action Limiter", toggleStr(CONFIG.client.extra.actionLimiter.enabled), true)
            .addField("Allow Movement", toggleStr(CONFIG.client.extra.actionLimiter.allowMovement), true)
            .addField("Movement Distance", String.valueOf(CONFIG.client.extra.actionLimiter.movementDistance), true)
            .addField("Movement Home", String.format("%d, %d", CONFIG.client.extra.actionLimiter.movementHomeX, CONFIG.client.extra.actionLimiter.movementHomeZ), true)
            .addField("Movement Min Y", String.valueOf(CONFIG.client.extra.actionLimiter.movementMinY), true)
            .addField("Allow Inventory", toggleStr(CONFIG.client.extra.actionLimiter.allowInventory), true)
            .addField("Allow Block Breaking", toggleStr(CONFIG.client.extra.actionLimiter.allowBlockBreaking), true)
            .addField("Allow Interact", toggleStr(CONFIG.client.extra.actionLimiter.allowInteract), true)
            .addField("Allow Ender Chest", toggleStr(CONFIG.client.extra.actionLimiter.allowEnderChest), true)
            .addField("Allow Use Item", toggleStr(CONFIG.client.extra.actionLimiter.allowUseItem), true)
            .addField("Allow Chat", toggleStr(CONFIG.client.extra.actionLimiter.allowChat), true)
            .color(Color.CYAN);
    }
}
