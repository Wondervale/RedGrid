package com.foxxite.RedGrid.listeners;

import com.foxxite.RedGrid.RedGrid;
import com.foxxite.RedGrid.models.Channel;
import com.foxxite.RedGrid.utils.SignType;
import com.foxxite.RedGrid.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import static com.foxxite.RedGrid.utils.Utils.colorizeSign;
import static com.foxxite.RedGrid.utils.Utils.componentToString;

public class SignListener implements Listener {

    @EventHandler (priority = EventPriority.MONITOR)
    void onSignChange(SignChangeEvent event)
    {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Sign sign = (Sign) event.getBlock().getState();

        player.sendMessage("Changed a sign!");
        for (int i = 0; i < event.lines().size(); i++) {
            Component line = event.line(i);
            player.sendMessage("Line " + i + ": " + componentToString(line));
        }

        String firstLine = componentToString(event.line(0));
        if (Utils.isInvalidSignType(firstLine)) return;

        String channelName = componentToString(event.line(1)).toLowerCase();
        player.sendMessage("Channel name: " + channelName);
        if (channelName.isEmpty()) {
            sendPlayerMessage(player, "<red>You must specify a channel name on line 2!");
            return;
        }

        SignType signType = Utils.getSignType(firstLine);
        if (signType == SignType.OTHER_PLUGIN) {
            player.sendMessage("Probably another plugin's sign, ignoring.");
            return;
        }

        if (signType == SignType.INVALID) {
            sendPlayerMessage(player, "<red>Invalid sign type!");
            return;
        }

        RedGrid.getInstance().getDatabaseManager().runAsync(() -> {
                Channel channel = RedGrid.getInstance().getDatabaseManager().getOrCreateChannel(
                channelName, player.getUniqueId());

                if (channel == null) {
                    sendPlayerMessage(player, "<red>Failed to create or retrieve channel!</red>\n\n<gray>Check the server console for errors.");
                    return;
                }

                registerTransponder(player, channel, signType, sign, channelName);
        });
    }

    @EventHandler (priority = EventPriority.MONITOR)
    void onBlockBreak(BlockBreakEvent event)
    {
        if (event.isCancelled()) return;
        if (!isSign(event.getBlock())) return;

        Player player = event.getPlayer();
        Sign sign = (Sign) event.getBlock().getState();

        String firstLine = componentToString(sign.getSide(Side.FRONT).line(0));
        if (Utils.isInvalidSignType(firstLine)) return;

        String channelName = componentToString(sign.getSide(Side.FRONT).line(1));
        if (channelName.isEmpty()) return;

        // Destroy the sign from the database
        RedGrid.getInstance().getDatabaseManager().runAsync(() -> {
            Channel channel = RedGrid.getInstance().getDatabaseManager().getOrCreateChannel(
                    channelName, player.getUniqueId());
            if (channel == null) {
                sendPlayerMessage(player, "<red>Failed to retrieve channel!</red>\n\n<gray>Check the server console for errors.");
                return;
            }
            boolean success = RedGrid.getInstance().getDatabaseManager().deleteTransponder(sign);
            if (success) {
                sendPlayerMessage(player, String.format("<green>Transponder on channel <white>%s</white> removed successfully!</green>", channelName));
            } else {
                sendPlayerMessage(player, String.format("<red>Failed to remove transponder on channel <white>%s</white>!</red>\n\n<gray>Check the server console for errors.", channelName));
            }
        });
    }

    private boolean isSign(Block block)
    {
        return block.getState() instanceof Sign;
    }

    private void sendPlayerMessage(Player player, String message) {
        player.sendMessage(RedGrid.getInstance().getPrefix()
                                  .append(RedGrid.getInstance().getMiniMessage().deserialize(message)));
    }

    private void registerTransponder(Player player, Channel channel, SignType signType, Sign sign, String channelName) {
        boolean success = RedGrid.getInstance().getDatabaseManager().createTransponder(channel, signType, sign);

        if (success) {
            sendPlayerMessage(player, String.format("<green>%s registered on channel <white>%s</white></green>",
                                                    capitalize(signType.name().toLowerCase()), channelName));

            RedGrid.getInstance().getServer().getScheduler().runTask(RedGrid.getInstance(),
                () -> {
                    colorizeSign(sign, signType, channelName);
                    RedstoneListener.removeFromCache(sign.getLocation());
                });
        } else {
            sendPlayerMessage(player, String.format("<red>Failed to register %s!</red>\n\n<gray>Check the server console for errors.",
                                                    signType.name().toLowerCase()));
        }
    }

    private String capitalize(String str) {
        if (str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
