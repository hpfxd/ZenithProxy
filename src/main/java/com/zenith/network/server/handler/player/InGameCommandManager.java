package com.zenith.network.server.handler.player;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandOutputHelper;
import com.zenith.command.CommandSource;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

import java.util.regex.Pattern;

import static com.zenith.Shared.*;
import static java.util.Arrays.asList;

public class InGameCommandManager {

    // this is specific to CONTROLLING account commands - not spectator player commands!
    public void handleInGameCommand(final String message, final @NonNull ServerConnection session) {
        TERMINAL_LOG.info(session.getProfileCache().getProfile().getName() + " executed in-game command: " + message);
        final String command = message.split(" ")[0]; // first word is the command
        switch (command) {
            case "help" -> {
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&9&lIn Game commands"), false));
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&2Prefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
                session.sendAsync(new ClientboundSystemChatPacket(Component.text(""), false));
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&7&cm &7- &8Sends a message to spectators"), false));
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&7&csync &7- &8Syncs current player inventory with server"), false));
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&7&cchunksync &7- &8Syncs server chunks to the current player"), false));
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&7&ccleareffects &7- &8Clears all effects from the current player"), false));
                executeInGameCommand(message, session);
            }
            case "m" -> Proxy.getInstance().getActiveConnections().forEach(connection -> {
                connection.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&c" + session.getProfileCache().getProfile().getName() + " > " + message.substring(1).trim() + "&r"), false));
            });
            case "sync" -> {
                PlayerCache.sync();
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&7[&9ZenithProxy&7]&r &cSync inventory complete"), false));
            }
            case "chunksync" -> {
                ChunkCache.sync();
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&7[&9ZenithProxy&7]&r &cSync chunks complete"), false));
            }
            case "cleareffects" -> {
                CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().clear();
                asList(Effect.values()).forEach(effect -> {
                    session.sendAsync(new ClientboundRemoveMobEffectPacket(CACHE.getPlayerCache().getEntityId(), effect));
                });
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&9Cleared effects&r"), false));
            }
            default -> executeInGameCommand(message, session);
        }
    }

    // todo: we could memoize this as long as we update it when the prefix is updated
    public Pattern getCommandPattern() {
        return Pattern.compile("[" + CONFIG.inGameCommands.prefix + "]\\w+");
    }

    private void executeInGameCommand(final String command, final ServerConnection session) {
        final CommandContext commandContext = CommandContext.create(command, CommandSource.IN_GAME_PLAYER);
        COMMAND_MANAGER.execute(commandContext);
        final EmbedCreateSpec embed = commandContext.getEmbedBuilder().build();
        CommandOutputHelper.logEmbedOutputToInGame(embed, session);
        CommandOutputHelper.logMultiLineOutputToInGame(commandContext, session);
        if (!embed.isTitlePresent() && commandContext.getMultiLineOutput().isEmpty())
            session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse(
                "&7[&9ZenithProxy&7]&r &cUnknown command!"), false));
        if (CONFIG.inGameCommands.logToDiscord && DISCORD_BOT.isRunning()) {
            // will also log to terminal
            CommandOutputHelper.logInputToDiscord(command, CommandSource.IN_GAME_PLAYER);
            CommandOutputHelper.logEmbedOutputToDiscord(embed);
            CommandOutputHelper.logMultiLineOutputToDiscord(commandContext);
        } else {
            CommandOutputHelper.logEmbedOutputToTerminal(embed);
            CommandOutputHelper.logMultiLineOutputToTerminal(commandContext);
        }
    }
}
