package com.foxxite.RedGrid.listeners;

import com.foxxite.RedGrid.RedGrid;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class RedstoneListener implements Listener {

    @EventHandler (priority = EventPriority.MONITOR)
    public void onRedstoneChange(BlockRedstoneEvent event) {
        Block block = event.getBlock();

        // Check all neighbors for signs
        for (Block relative : new Block[]{
                block.getRelative(1, 0, 0),
                block.getRelative(-1, 0, 0),
                block.getRelative(0, 1, 0),
                block.getRelative(0, -1, 0),
                block.getRelative(0, 0, 1),
                block.getRelative(0, 0, -1)}) {

            if (relative.getState() instanceof Sign sign) {
                BlockData data = relative.getBlockData();

                if (data instanceof Directional dir) {
                    // Wall sign: direct check
                    Block attached = relative.getRelative(dir.getFacing().getOppositeFace());
                    if (attached.equals(block)) {
                        handleSignPower(sign, event.getNewCurrent() > 0);
                    }
                } else {
                    // Standing sign: schedule delayed check
                    Bukkit.getScheduler().runTask(RedGrid.getInstance(), () -> {
                        boolean powered = relative.isBlockPowered() || relative.isBlockIndirectlyPowered();
                        handleSignPower(sign, powered);
                    });
                }
            }
        }
    }

    private void handleSignPower(Sign sign, boolean powered) {
        if (powered) {
            RedGrid.getInstance().getServer().broadcast(Component.text(String.format(
                    "Sign at %s powered!", sign.getBlock().getLocation())));
        } else {
            RedGrid.getInstance().getServer().broadcast(Component.text(String.format(
                    "Sign at %s unpowered!", sign.getBlock().getLocation())));
        }

        // 👇 put your actual shared logic here
    }
}
