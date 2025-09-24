package com.foxxite.RedGrid.listeners;

import com.foxxite.RedGrid.RedGrid;
import com.foxxite.RedGrid.events.ChannelActivationChangeEvent;
import com.foxxite.RedGrid.models.Channel;
import com.foxxite.RedGrid.models.Transponder;
import com.foxxite.RedGrid.utils.SignType;
import com.foxxite.RedGrid.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WirelessListener implements Listener {

    @EventHandler
    public void onWirelessEvent(ChannelActivationChangeEvent event) {
        Channel channel = event.getChannel();
        int newActivations = event.getNewActivations();
        int oldActivations = event.getOldActivations();

        // We are powered if the new activations are greater than 0
        boolean powered = newActivations > 0;
        boolean wasPowered = oldActivations > 0;

        // Only proceed if the powered state has changed
        if (powered == wasPowered)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(RedGrid.getInstance(), () -> {
            try {
                channel.getTransponders().forEach(transponder -> {
                    if (transponder.isTransmitter())
                        return;

                    Location loc = transponder.getLocation();

                    Bukkit.getScheduler().runTask(RedGrid.getInstance(), () -> {
                        if (powered) {
                            loc.getBlock().setType(Material.REDSTONE_BLOCK);
                        } else {
                            placeSignBack(loc, transponder);
                        }
                    });
                });
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    void placeSignBack(Location loc, Transponder transponder) {
        // Determine sign type
        SignType type = transponder.isTransmitter() ? SignType.TRANSMITTER : SignType.RECEIVER;

        // Set the block to a wall sign or standing sign depending on the transponder
        Material signMaterial = transponder.isWallSign() ?
                Material.OAK_WALL_SIGN :
                Material.OAK_SIGN;
        loc.getBlock().setType(signMaterial);

        // Schedule sync task to modify the sign after it's placed
        Bukkit.getScheduler().runTask(RedGrid.getInstance(), () -> {
            if (!(loc.getBlock().getState() instanceof Sign))
                return;

            // Set facing direction
            Sign sign = placeOakSignWithRotation(loc, transponder.getBlockFace(),
                                                 transponder.isWallSign());

            // Colorize and set channel name
            String channelName = transponder.getChannel().getName();
            Utils.colorizeSign(sign, type, channelName);
        });
    }

    Sign placeOakSignWithRotation(final Location location, final BlockFace face, boolean wallSign) {
        final Block signBlock = location.getBlock();
        final BlockData data = Bukkit.createBlockData(wallSign ?
                                                              Material.OAK_WALL_SIGN :
                                                              Material.OAK_SIGN);

        if (data instanceof Rotatable rotatable) {
            rotatable.setRotation(face);
            signBlock.setBlockData(data);
        } else {
            final Directional directional = (Directional) data;
            directional.setFacing(face);
            signBlock.setBlockData(data);
        }

        return (Sign) signBlock.getState();
    }
}
