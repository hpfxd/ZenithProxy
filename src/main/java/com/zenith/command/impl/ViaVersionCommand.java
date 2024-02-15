package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ViaVersionCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "via",
            CommandCategory.MODULE,
            "Configure ViaVersion. 'Client' = ZenithProxy connecting to the MC server. 'Server' = players connecting to ZenithProxy",
            asList(
                "client on/off",
                "client autoConfig on/off",
                "client version <MC version>",
                "server on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("via")
            .then(literal("client")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.viaversion.enabled = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Client ViaVersion " + (CONFIG.client.viaversion.enabled ? "On!" : "Off!"));
                    return 1;
                }))
                .then(literal("autoConfig")
                          .then(argument("toggle", toggle()).executes(c -> {
                                CONFIG.client.viaversion.autoProtocolVersion = getToggle(c, "toggle");
                                c.getSource().getEmbed()
                                    .title("Client ViaVersion AutoConfig " + (CONFIG.client.viaversion.autoProtocolVersion ? "On!" : "Off!"));
                                return 1;
                          })))
                .then(literal("version")
                          .then(argument("version", wordWithChars()).executes(c -> {
                              final String version = StringArgumentType.getString(c, "version");
                              ProtocolVersion closest = ProtocolVersion.getClosest(version);
                              if (closest == null) {
                                  c.getSource().getEmbed()
                                      .title("Invalid Version!")
                                      .description("Please select a valid version. Example: 1.19.4")
                                      .color(Color.RED);
                              } else {
                                  CONFIG.client.viaversion.protocolVersion = closest.getVersion();
                                  c.getSource().getEmbed()
                                      .title("Client ViaVersion Version Updated!");
                              }
                              return 1;
                          }))))
            .then(literal("server")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.server.viaversion.enabled = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Server ViaVersion " + (CONFIG.server.viaversion.enabled ? "On!" : "Off!"));
                            return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed embedBuilder) {
        embedBuilder
            .addField("Client ViaVersion", toggleStr(CONFIG.client.viaversion.enabled), false)
            .addField("Client AutoConfig", toggleStr(CONFIG.client.viaversion.autoProtocolVersion), false)
            .addField("Client Version", ProtocolVersion.getProtocol(CONFIG.client.viaversion.protocolVersion).getName(), false)
            .addField("Server ViaVersion", toggleStr(CONFIG.server.viaversion.enabled), false)
            .color(Color.CYAN);
    }}
