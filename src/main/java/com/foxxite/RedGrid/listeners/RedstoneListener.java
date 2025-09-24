package com.foxxite.RedGrid.listeners;

import com.foxxite.RedGrid.RedGrid;
import com.foxxite.RedGrid.utils.SignType;
import com.foxxite.RedGrid.utils.Utils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.concurrent.TimeUnit;

public class RedstoneListener implements Listener {

    // Caffeine cache storing locations to ignore
    private static final Cache<Location, Boolean> ignoreCache = Caffeine.newBuilder()
                                                                        .expireAfterWrite(10, TimeUnit.MINUTES) // optional expiration
                                                                        .maximumSize(10_000)
                                                                        .build();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRedstoneChange(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        // Ignore this block if cached
        if (ignoreCache.getIfPresent(loc) != null) return;

        boolean foundSign = false;

        // Check all neighbors for signs
        for (Block relative : new Block[]{
                block.getRelative(1, 0, 0),
                block.getRelative(-1, 0, 0),
                block.getRelative(0, 1, 0),
                block.getRelative(0, -1, 0),
                block.getRelative(0, 0, 1),
                block.getRelative(0, 0, -1)}) {

            if (relative.getState() instanceof Sign sign) {
                if (Utils.getSignType(sign.getSide(Side.FRONT).lines()) != SignType.TRANSMITTER &&
                    Utils.getSignType(sign.getSide(Side.BACK).lines()) != SignType.TRANSMITTER) {
                    continue; // Not a RedGrid transmitter sign
                }

                foundSign = true;
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

        // If no sign was found, cache this location to ignore in the future
        if (!foundSign) {
            ignoreCache.put(loc, true);
        }
    }

    private void handleSignPower(Sign sign, boolean powered) {
        Component message = Component.text(String.format(
                "Sign at %s %s!", sign.getBlock().getLocation(), powered ? "powered" : "unpowered"
        ));
        RedGrid.getInstance().getServer().broadcast(message);

        // 👇 put your actual shared logic here
    }

    // Static method to remove a location from the cache
    public static void removeFromCache(Location loc) {
        if (ignoreCache.getIfPresent(loc) != null) return;

        ignoreCache.invalidate(loc);
    }
}
