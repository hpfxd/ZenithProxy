package com.zenith.feature.deathmessages;

import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class DeathMessagesParser {
    private final List<DeathMessageSchemaInstance> deathMessageSchemaInstances;
    private final List<String> mobs;

    public DeathMessagesParser() {
        List<String> mobsTemp = Collections.emptyList();
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("death_message_mobs.schema")))) {
                mobsTemp = br.lines()
                        .filter(l -> !l.isEmpty()) //any empty lines
                        .filter(l -> !l.startsWith("#")) //comments
                        .sorted(Comparator.comparingInt(String::length).reversed())
                        .collect(Collectors.toList());
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing mobs for death message parsing", e);
        }
        mobs = mobsTemp;
        List<DeathMessageSchemaInstance> schemaInstancesTemp = Collections.emptyList();
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("death_messages.schema")))) {
                schemaInstancesTemp = br.lines()
                        .filter(l -> !l.isEmpty()) //any empty lines
                        .filter(l -> !l.startsWith("#")) //comments
                        .sorted(Comparator.comparingInt(String::length).reversed())
                        .map(l -> new DeathMessageSchemaInstance(l, mobs))
                        .collect(Collectors.toList());
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing death message schemas", e);
        }
        deathMessageSchemaInstances = schemaInstancesTemp;
    }

    public Optional<DeathMessageParseResult> parse(final String rawInput, final boolean verifyPlayers) {
        if (nonNull(deathMessageSchemaInstances)) {
            for (final DeathMessageSchemaInstance instance : deathMessageSchemaInstances) {
                final Optional<DeathMessageParseResult> parse = instance.parse(rawInput, verifyPlayers);
                if (parse.isPresent()) {
                    return parse;
                }
            }
        }
        if (CONFIG.database.deaths.enabled && CONFIG.database.deaths.unknownDeathDiscordMsg && DISCORD_BOT.isRunning()) {
            DISCORD_BOT.sendEmbedMessage(Embed.builder()
                                                 .title("Unknown death message")
                                                 .description(rawInput)
                                                 .color(Color.RUBY));
        }
        DEFAULT_LOG.warn("No death message schema found for '{}'", rawInput);
        return Optional.empty();
    }
}
