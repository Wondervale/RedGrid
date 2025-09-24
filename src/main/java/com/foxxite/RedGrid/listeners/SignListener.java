package com.foxxite.RedGrid.listeners;

import java.util.List;

import com.foxxite.RedGrid.RedGrid;
import com.foxxite.RedGrid.models.Channel;
import com.foxxite.RedGrid.utils.SignType;
import com.foxxite.RedGrid.utils.Utils;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import static com.foxxite.RedGrid.utils.Utils.colorizeSign;
import static com.foxxite.RedGrid.utils.Utils.componentToString;

public class SignListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    void onSignChange(SignChangeEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        Sign sign = (Sign) event.getBlock().getState();

        String firstLine = componentToString(event.line(0));
        if (Utils.isInvalidSignType(firstLine))
            return;

        String channelName = componentToString(event.line(1)).toLowerCase();
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

    @EventHandler(priority = EventPriority.MONITOR)
    void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        if (!isSign(event.getBlock()))
            return;

        handleSignRemoval(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (isSign(block)) {
                handleSignRemoval(block, null);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (isSign(block)) {
                handleSignRemoval(block, null);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPistonRetract(BlockPistonRetractEvent event) {
        List<Block> blocks = event.getBlocks();
        for (Block block : blocks) {
            if (isSign(block)) {
                handleSignRemoval(block, null);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign))
            return;

        Sign sign = (Sign) block.getState();
        // For wall signs, check if the block they are attached to is gone
        Block attachedBlock = block.getRelative(getAttachedFace(sign));
        if (attachedBlock.getType().isAir()) {
            handleSignRemoval(block, null);
        }
    }

    // Determines which block a wall sign is attached to
    private BlockFace getAttachedFace(Sign sign) {
        if (Tag.WALL_SIGNS.isTagged(sign.getBlockData().getMaterial())) {
            org.bukkit.block.data.type.WallSign wallSign = (org.bukkit.block.data.type.WallSign) sign.getBlockData();
            return wallSign.getFacing().getOppositeFace(); // The block it’s attached to
        } else {
            return BlockFace.DOWN; // Default for non-wall signs
        }
    }

    private void handleSignRemoval(Block block, Player player) {
        Sign sign = (Sign) block.getState();
        String firstLine = componentToString(sign.getSide(Side.FRONT).line(0));
        if (Utils.isInvalidSignType(firstLine))
            return;

        String channelName = componentToString(sign.getSide(Side.FRONT).line(1));
        if (channelName.isEmpty())
            return;

        RedGrid.getInstance().getDatabaseManager().runAsync(() -> {
            Channel channel = RedGrid.getInstance().getDatabaseManager().getOrCreateChannel(
                    channelName, player != null ? player.getUniqueId() : null);
            if (channel == null)
                return;

            RedGrid.getInstance().getDatabaseManager().deleteTransponder(sign);
        });

        if (player != null) {
            sendPlayerMessage(player, String.format("<green>Transponder on channel <white>%s</white> removed successfully!</green>", channelName));
        } else {
            RedGrid.getInstance()
                   .getLogger()
                   .info(String.format("Transponder on channel %s removed by non-player means.", channelName));
        }
    }

    private boolean isSign(Block block) {
        return block.getState() instanceof Sign;
    }

    private void sendPlayerMessage(Player player, String message) {
        player.sendMessage(RedGrid.getInstance().getPrefix()
                                  .append(RedGrid.getInstance()
                                                 .getMiniMessage()
                                                 .deserialize(message)));
    }

    private void registerTransponder(Player player, Channel channel, SignType signType, Sign sign, String channelName) {
        boolean success = RedGrid.getInstance()
                                 .getDatabaseManager()
                                 .createTransponder(channel, signType, sign);

        if (success) {
            sendPlayerMessage(player, String.format("<green>%s registered on channel <white>%s</white></green>",
                                                    capitalize(signType.name()
                                                                       .toLowerCase()), channelName));

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
        if (str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
