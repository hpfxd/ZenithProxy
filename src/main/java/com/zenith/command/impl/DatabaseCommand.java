package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DATABASE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DatabaseCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("database",
                                 CommandCategory.MANAGE,
                                 "Configures what 2b2t server data is collected by the proxy. No database logs personal data.",
                                 asList(
                                     "queueWait on/off",
                                     "queueLength on/off",
                                     "publicChat on/off",
                                     "joinLeave on/off",
                                     "deathMessages on/off",
                                     "restarts on/off",
                                     "playerCount on/off",
                                     "tablist on/off"
                                 ),
                                 asList("db")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("database")
            .then(literal("queueWait")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.queueWait.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.queueWait.enabled) DATABASE_MANAGER.startQueueWaitDatabase();
                            else DATABASE_MANAGER.stopQueueWaitDatabase();
                            c.getSource().getEmbed()
                                .title("Queue Wait Database " + (CONFIG.database.queueWait.enabled ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("queueLength")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.queueLength.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.queueLength.enabled) DATABASE_MANAGER.startQueueLengthDatabase();
                            else DATABASE_MANAGER.stopQueueLengthDatabase();
                            c.getSource().getEmbed()
                                .title("Queue Length Database " + (CONFIG.database.queueLength.enabled ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("publicChat")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.chats.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.chats.enabled) DATABASE_MANAGER.startChatsDatabase();
                            else DATABASE_MANAGER.stopChatsDatabase();
                            c.getSource().getEmbed()
                                .title("Public Chat Database " + (CONFIG.database.chats.enabled ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("joinLeave")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.connections.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.connections.enabled) DATABASE_MANAGER.startConnectionsDatabase();
                            else DATABASE_MANAGER.stopConnectionsDatabase();
                            c.getSource().getEmbed()
                                .title("Connections Database " + (CONFIG.database.connections.enabled ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("deathMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.deaths.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.deaths.enabled) DATABASE_MANAGER.startDeathsDatabase();
                            else DATABASE_MANAGER.stopDeathsDatabase();
                            c.getSource().getEmbed()
                                .title("Death Messages Database " + (CONFIG.database.deaths.enabled ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("restarts")
                        .then(argument("toggle", toggle()).executes(c -> {
                                CONFIG.database.restarts.enabled = getToggle(c, "toggle");
                                if (CONFIG.database.restarts.enabled) DATABASE_MANAGER.startRestartsDatabase();
                                else DATABASE_MANAGER.stopRestartsDatabase();
                                c.getSource().getEmbed()
                                    .title("Restarts Database " + (CONFIG.database.restarts.enabled ? "On!" : "Off!"));
                                return 1;
                        })))
            .then(literal("playerCount")
                        .then(argument("toggle", toggle()).executes(c -> {
                                CONFIG.database.playerCount.enabled = getToggle(c, "toggle");
                                if (CONFIG.database.playerCount.enabled) DATABASE_MANAGER.startPlayerCountDatabase();
                                else DATABASE_MANAGER.stopPlayerCountDatabase();
                                c.getSource().getEmbed()
                                    .title("Player Count Database " + (CONFIG.database.playerCount.enabled ? "On!" : "Off!"));
                                return 1;
                        })))
            .then(literal("tablist")
                        .then(argument("toggle", toggle()).executes(c -> {
                                CONFIG.database.tablist.enabled = getToggle(c, "toggle");
                                if (CONFIG.database.tablist.enabled) DATABASE_MANAGER.startTablistDatabase();
                                else DATABASE_MANAGER.stopTablistDatabase();
                                c.getSource().getEmbed()
                                    .title("Tablist Database " + (CONFIG.database.tablist.enabled ? "On!" : "Off!"));
                                return 1;
                        })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Queue Wait", toggleStr(CONFIG.database.queueWait.enabled), false)
            .addField("Queue Length", toggleStr(CONFIG.database.queueLength.enabled), false)
            .addField("Public Chat", toggleStr(CONFIG.database.chats.enabled), false)
            .addField("Join/Leave", toggleStr(CONFIG.database.connections.enabled), false)
            .addField("Death Messages", toggleStr(CONFIG.database.deaths.enabled), false)
            .addField("Restarts", toggleStr(CONFIG.database.restarts.enabled), false)
            .addField("Player Count", toggleStr(CONFIG.database.playerCount.enabled), false)
            .addField("Tablist", toggleStr(CONFIG.database.tablist.enabled), false)
            .color(Color.CYAN);
    }
}
