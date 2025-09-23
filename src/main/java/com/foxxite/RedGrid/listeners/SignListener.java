package com.foxxite.RedGrid.listeners;

import com.foxxite.RedGrid.RedGrid;
import com.foxxite.RedGrid.models.Channel;
import com.foxxite.RedGrid.utils.SignType;
import com.foxxite.RedGrid.utils.Utils;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerInteract(PlayerInteractEvent event) {
        if (!isRightClickOnSign(event)) return;

        Player player = event.getPlayer();
        Sign sign = (Sign) event.getClickedBlock().getState();

        String firstLine = sign.getLine(0);
        if (!Utils.isValidSignType(firstLine)) return;

        String channelName = sign.getLine(1);
        if (channelName.isEmpty()) {
            sendPlayerMessage(player, "<red>You must specify a channel name on line 2!");
            return;
        }

        SignType signType = Utils.getSignType(firstLine);
        if (signType == SignType.INVALID) {
            sendPlayerMessage(player, "<red>Invalid sign type!");
            return;
        }

        Channel channel = RedGrid.getInstance().getDatabaseManager().getOrCreateChannel(
                channelName, player.getUniqueId());

        if (channel == null) {
            sendPlayerMessage(player, "<red>Failed to create or retrieve channel!</red>\n\n<gray>Check the server console for errors.");
            return;
        }

        registerTransponder(player, channel, signType, sign, channelName);
    }

    @EventHandler (priority = EventPriority.MONITOR)
    void onSignChange(SignChangeEvent event)
    {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Sign sign = (Sign) event.getBlock().getState();

        String firstLine = sign.getLine(0);
        if (!Utils.isValidSignType(firstLine)) return;

        String channelName = sign.getLine(1);
        if (channelName.isEmpty()) {
            sendPlayerMessage(player, "<red>You must specify a channel name on line 2!");
            return;
        }

        SignType signType = Utils.getSignType(firstLine);
        if (signType == SignType.INVALID) {
            sendPlayerMessage(player, "<red>Invalid sign type!");
            return;
        }

        Channel channel = RedGrid.getInstance().getDatabaseManager().getOrCreateChannel(
                channelName, player.getUniqueId());

        if (channel == null) {
            sendPlayerMessage(player, "<red>Failed to create or retrieve channel!</red>\n\n<gray>Check the server console for errors.");
            return;
        }

        registerTransponder(player, channel, signType, sign, channelName);
    }

    private boolean isRightClickOnSign(PlayerInteractEvent event) {
        if (event.isCancelled()) return false;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        if (event.getClickedBlock() == null) return false;

        return event.getClickedBlock().getState() instanceof Sign;
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
