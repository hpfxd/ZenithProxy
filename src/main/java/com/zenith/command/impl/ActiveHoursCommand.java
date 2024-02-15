package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.*;
import com.zenith.discord.Embed;
import com.zenith.util.Config.Client.Extra.Utility.ActiveHours.ActiveTime;
import discord4j.rest.util.Color;

import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ActiveHoursCommand extends Command {
    private static final Pattern TIME_PATTERN = Pattern.compile("[0-9]{1,2}:[0-9]{2}");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "activeHours",
            CommandCategory.MODULE,
            "Set active hours for the proxy to automatically be logged in at."
                + "\nBy default, 2b2t's queue wait ETA is used to determine when to log in."
                + "\nThe connect will occur when the current time plus the ETA is equal to a time set."
                + "\nQueue ETA calc can be disabled with a command, which would mean connects would occur exactly at the set times."
                + "\n Time zone Ids (\"TZ identifier\" column): https://w.wiki/8Yif"
                + "\n Time format: XX:XX, e.g.: 1:42, 14:42, 14:01",
            asList("on/off",
                   "timezone <timezone ID>",
                   "add/del <time>",
                   "status",
                   "forceReconnect on/off",
                   "queueEtaCalc on/off")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("activeHours")
            .then(argument("toggle", toggle()).executes(c -> {
                boolean toggle = getToggle(c, "toggle");
                CONFIG.client.extra.utility.actions.activeHours.enabled = toggle;
                c.getSource().getEmbed()
                    .title("Active Hours " + (toggle ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("timezone").then(argument("tz", wordWithChars()).executes(c -> {
                final String timeZoneId = CustomStringArgumentType.getString(c, "tz");
                if (ZoneId.getAvailableZoneIds().stream().noneMatch(id -> id.equals(timeZoneId))) {
                    return -1;
                } else {
                    CONFIG.client.extra.utility.actions.activeHours.timeZoneId = ZoneId.of(timeZoneId).getId();
                    c.getSource().getEmbed()
                        .title("Set timezone: " + timeZoneId);
                    return 1;
                }
            })))
            .then(literal("add").then(argument("time", wordWithChars()).executes(c -> {
                final String time = StringArgumentType.getString(c, "time");
                if (!timeMatchesRegex(time)) {
                    return -1;
                } else {
                    final ActiveTime activeTime = ActiveTime.fromString(time);
                    if (!CONFIG.client.extra.utility.actions.activeHours.activeTimes.contains(activeTime)) {
                        CONFIG.client.extra.utility.actions.activeHours.activeTimes.add(activeTime);
                    }
                    c.getSource().getEmbed()
                                 .title("Added time: " + time);
                    return 1;
                }
            })))
            .then(literal("del").then(argument("time", wordWithChars()).executes(c -> {
                final String time = StringArgumentType.getString(c, "time");
                if (!timeMatchesRegex(time)) {
                    return -1;
                } else {
                    final ActiveTime activeTime = ActiveTime.fromString(time);
                    CONFIG.client.extra.utility.actions.activeHours.activeTimes.removeIf(s -> s.equals(activeTime));
                    c.getSource().getEmbed()
                        .title("Removed time: " + time);
                    return 1;
                }
            })))
            .then(literal("status").executes(c -> {
                c.getSource().getEmbed()
                    .title("Active Hours Status");
            }))
            .then(literal("forceReconnect")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.utility.actions.activeHours.forceReconnect = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Force Reconnect Set!");
                          return 1;
                      })))
            .then(literal("queueEtaCalc")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.utility.actions.activeHours.queueEtaCalc = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Queue ETA Calc Set!");
                          return 1;
                      })));
    }

    private boolean timeMatchesRegex(final String arg) {
        final Matcher matcher = TIME_PATTERN.matcher(arg);
        boolean matchesRegex = matcher.matches();
        if (!matchesRegex) return false;
        final ActiveTime activeTime = ActiveTime.fromString(arg);
        return activeTime.hour <= 23 && activeTime.minute <= 59;
    }

    private String activeTimeListToString(final List<ActiveTime> activeTimes) {
        return activeTimes.stream()
                .sorted((a, b) -> {
                    if (a.hour == b.hour) {
                        return a.minute - b.minute;
                    } else {
                        return a.hour - b.hour;
                    }
                })
                .map(ActiveTime::toString)
                .collect(Collectors.joining(", "));
    }

    @Override
    public void postPopulate(Embed builder) {
        builder
            .addField("ActiveHours", toggleStr(CONFIG.client.extra.utility.actions.activeHours.enabled), false)
            .addField("Time Zone", CONFIG.client.extra.utility.actions.activeHours.timeZoneId, false)
            .addField("Active Hours", (CONFIG.client.extra.utility.actions.activeHours.activeTimes.isEmpty()
                ? "None set!"
                : activeTimeListToString(CONFIG.client.extra.utility.actions.activeHours.activeTimes)), false)
            .addField("Force Reconnect", toggleStr(CONFIG.client.extra.utility.actions.activeHours.forceReconnect), false)
            .addField("Queue ETA Calc", toggleStr(CONFIG.client.extra.utility.actions.activeHours.queueEtaCalc), false)
            .color(Color.CYAN);
    }
}
