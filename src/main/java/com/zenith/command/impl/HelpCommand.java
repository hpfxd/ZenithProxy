package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.*;
import discord4j.rest.util.Color;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.COMMAND_MANAGER;
import static java.util.Arrays.asList;

public class HelpCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "help",
            CommandCategory.CORE,
            "ZenithProxy command list",
            asList(
                "",
                "<category>",
                "<command>"
            ),
            asList("h")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("help").executes(c -> {
                c.getSource().getEmbed()
                    .title("Commands")
                    .color(Color.CYAN);
                final String commandUsages = getCommandUsages(c.getSource().getSource(), CommandCategory.CORE);
                final String prefix = COMMAND_MANAGER.getCommandPrefix(c.getSource().getSource());
                c.getSource().getEmbed()
                    .description("**More info:** "
                                     + "\n  `" + prefix + "help <command>` or `" + prefix + "help <category>`"
                                     + "\n\n**Categories**\n"
                                     + Arrays.stream(CommandCategory.values())
                                            .map(CommandCategory::getName)
                                            .collect(Collectors.joining(", "))
                                     + "\n"
                                     + "\n**Core Commands**"
                                     + "\n" + commandUsages
                    );
            })
            .then(argument("commandName", string()).executes(c -> {
                final String commandName = StringArgumentType.getString(c, "commandName");
                c.getSource().getEmbed()
                    .title("Command Usage")
                    .color(Color.CYAN);
                Arrays.stream(CommandCategory.values())
                    .filter(category -> category.getName().equalsIgnoreCase(commandName))
                    .findFirst()
                    .ifPresentOrElse(
                        category -> populateCategory(c.getSource(), category),
                        () -> populateCommand(c.getSource(), commandName));
                return 1;
            }));
    }

    private String getCommandUsages(final CommandSource src, final CommandCategory category) {
        return COMMAND_MANAGER.getCommands(category).stream()
            .sorted((c1, c2) -> c1.commandUsage().getName().compareToIgnoreCase(c2.commandUsage().getName()))
            .map(command -> command.commandUsage().shortSerialize(src))
            .collect(Collectors.joining("\n"));
    }

    private void populateCategory(final CommandContext c, final CommandCategory category) {
        final String commandUsages = getCommandUsages(c.getSource(), category);
        final String prefix = COMMAND_MANAGER.getCommandPrefix(c.getSource());
        c.getEmbed()
            .description("**More info:** "
                             + "\n  `" + prefix + "help <command>` or `" + prefix + "help <category>`"
                             + "\n"
                             + "\n**" + category.getName() + " Commands**"
                             + "\n" + commandUsages
            );
    }

    private void populateCommand(final CommandContext c, final String commandName) {
        final Optional<Command> foundCommand = COMMAND_MANAGER.getCommands().stream()
            .filter(command -> command.commandUsage().getName().equalsIgnoreCase(commandName)
                || command.commandUsage().getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(commandName)))
            .findFirst();
        if (foundCommand.isPresent()) {
            c.getEmbed().description(foundCommand.get().commandUsage().serialize(c.getSource()));
        } else {
            c.getEmbed().description("Unknown command or category");
        }
    }
}
