package com.foxxite.RedGrid.listeners;

import java.util.concurrent.TimeUnit;

import com.foxxite.RedGrid.DatabaseManager;
import com.foxxite.RedGrid.RedGrid;
import com.foxxite.RedGrid.models.Channel;
import com.foxxite.RedGrid.utils.SignType;
import com.foxxite.RedGrid.utils.Utils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import org.jetbrains.annotations.NotNull;

public class RedstoneListener implements Listener {

    // Caffeine cache storing locations to ignore
    private static final Cache<@NotNull Location, Boolean> ignoreCache = Caffeine.newBuilder()
                                                                                 .expireAfterWrite(10, TimeUnit.MINUTES) // optional expiration
                                                                                 .maximumSize(10_000)
                                                                                 .build();

    // Static method to remove a location from the cache
    public static void removeFromCache(Location loc) {
        if (ignoreCache.getIfPresent(loc) != null)
            return;

        ignoreCache.invalidate(loc);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRedstoneChange(BlockRedstoneEvent event) {
        if (!RedGrid.getInstance().isListenToWorld())
            return;

        Block block = event.getBlock();
        Location loc = block.getLocation();

        // Ignore this block if cached
        if (ignoreCache.getIfPresent(loc) != null)
            return;

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
                        Utils.getSignType(sign.getSide(Side.BACK)
                                              .lines()) != SignType.TRANSMITTER) {
                    continue; // Not a RedGrid transmitter sign
                }

                foundSign = true;
                BlockData data = relative.getBlockData();

                Bukkit.getScheduler().runTask(RedGrid.getInstance(), () -> {
                    boolean powered = relative.isBlockPowered() || relative.isBlockIndirectlyPowered();

                    if (data instanceof Directional dir) {
                        // Wall sign: direct check
                        Block attached = relative.getRelative(dir.getFacing().getOppositeFace());

                        if (!powered) {
                            // Double-check for wall signs, as they can be a bit glitchy
                            powered = attached.isBlockPowered() || attached.isBlockIndirectlyPowered();
                        }
                    }

                    handleSignPower(sign, powered);
                });
            }
        }

        // If no sign was found, cache this location to ignore in the future
        if (!foundSign) {
            ignoreCache.put(loc, true);
        }
    }

    private void handleSignPower(Sign sign, boolean powered) {
        Bukkit.getScheduler().runTaskAsynchronously(RedGrid.getInstance(), () -> {
            Channel channel = Utils.getSignChannel(sign);
            if (channel == null)
                return;

            DatabaseManager db = RedGrid.getInstance().getDatabaseManager();

            if (powered) {
                db.incrementChannelActivations(channel); // returns updated value
            } else {
                db.decrementChannelActivations(channel); // returns updated value
            }
        });
    }
}
