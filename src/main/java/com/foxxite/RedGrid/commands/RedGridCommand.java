package com.foxxite.RedGrid.commands;

import java.sql.SQLException;

import com.foxxite.RedGrid.RedGrid;
import com.foxxite.RedGrid.models.Channel;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@Permission("redgrid.admin")
@Command(name = "redgrid", aliases = {"rg", "redg", "grid"})
public class RedGridCommand {
    RedGrid instance = RedGrid.getInstance();

    @Execute(name = "channels", aliases = {"chs", "list"})
    @Async
    void listChannels(@Context CommandSender sender) {
        sender.sendMessage(instance.getPrefix()
                                   .append(instance.getMiniMessage()
                                                   .deserialize("<green>Channels:</green>")));
        try {
            for (Channel channel : instance.getDatabaseManager()
                                           .getChannelDao()
                                           .queryForAll()) {
                boolean active = channel.getActivations() > 0;
                sender.sendMessage(instance.getMiniMessage().deserialize(String.format("<gray" +
                                                                                               ">-</gray> <white>%s</white> <gray>Status: %s Activations: %d)</gray>\n", channel.getName(),
                                                                                       active ?
                                                                                               "<green>Active</green>" :
                                                                                               "<red>Inactive</red>", channel.getActivations()))
                                           .append(Component.text("[Activate] ")
                                                            .color(NamedTextColor.GREEN)
                                                            .clickEvent(ClickEvent.runCommand(String.format("/redgrid channel %s %s", channel.getName(), ChannelActions.ACTIVATE)))
                                                            .hoverEvent(HoverEvent.showText(Component.text("Activate this channel")))
                                           )
                                           .append(Component.text("[Deactivate] ")
                                                            .color(NamedTextColor.RED)
                                                            .clickEvent(ClickEvent.runCommand(String.format("/redgrid channel %s %s", channel.getName(), ChannelActions.DEACTIVATE)))
                                                            .hoverEvent(HoverEvent.showText(Component.text("Deactivate this channel"))))
                                           .append(Component.text("[RESET]")
                                                            .color(NamedTextColor.DARK_RED)
                                                            .clickEvent(ClickEvent.runCommand(String.format("/redgrid channel %s %s", channel.getName(), ChannelActions.RESET)))
                                                            .hoverEvent(HoverEvent.showText(Component.text("Reset this channel"))))
                );
            }
        }
        catch (SQLException e) {
            sender.sendMessage(instance.getPrefix()
                                       .append(instance.getMiniMessage()
                                                       .deserialize("<red>Error fetching channels from database.</red>")));
            e.printStackTrace();
        }
    }

    @Execute(name = "channel", aliases = {"ch"})
    @Async
    void channelCommand(
            @Context CommandSender sender,
            @Arg @Async Channel channel, @Arg ChannelActions action) {
        if (channel == null) {
            sender.sendMessage(instance.getPrefix()
                                       .append(instance.getMiniMessage()
                                                       .deserialize("<red>Channel not found.</red>")));
            return;
        }

        switch (action) {
            case ChannelActions.ACTIVATE -> {
                instance.getDatabaseManager().incrementChannelActivations(channel);
                sender.sendMessage(instance.getPrefix()
                                           .append(instance.getMiniMessage()
                                                           .deserialize(String.format("<green>Channel %s activated.</green>", channel.getName()))));
            }
            case ChannelActions.DEACTIVATE -> {
                if (channel.getActivations() > 0) {
                    instance.getDatabaseManager().decrementChannelActivations(channel);
                    sender.sendMessage(instance.getPrefix()
                                               .append(instance.getMiniMessage()
                                                               .deserialize(String.format("<yellow>Channel %s deactivated.</yellow>", channel.getName()))));
                } else {
                    sender.sendMessage(instance.getPrefix()
                                               .append(instance.getMiniMessage()
                                                               .deserialize(String.format("<red>Channel %s is already inactive.</red>", channel.getName()))));
                }
            }
            case ChannelActions.RESET -> {
                Bukkit.getScheduler().runTask(RedGrid.getInstance(), () -> {
                    instance.getDatabaseManager().resetChannelActivations(channel);
                });

                sender.sendMessage(instance.getPrefix()
                                           .append(instance.getMiniMessage()
                                                           .deserialize(String.format("<green" +
                                                                                              ">Channel %s reset to inactive.</green>", channel.getName()))));
            }
            default -> sender.sendMessage(instance.getPrefix()
                                                  .append(instance.getMiniMessage()
                                                                  .deserialize("<red>Unknown action. Use activate, deactivate, or reset.</red>")));
        }
    }
}
