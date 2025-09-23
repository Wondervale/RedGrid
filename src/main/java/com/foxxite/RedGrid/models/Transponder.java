package com.foxxite.RedGrid.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

@DatabaseTable(tableName = "transponders")
public class Transponder {

    @DatabaseField(id = true)
    @Getter
    private String id;

    @DatabaseField(canBeNull = false)
    @Getter
    private boolean isTransmitter;

    @DatabaseField(canBeNull = false)
    @Getter
    private boolean isWallSign;

    @DatabaseField(canBeNull = false)
    @Getter
    private String facing;

    @DatabaseField(canBeNull = false)
    @Getter
    private String world;

    @DatabaseField(canBeNull = false)
    @Getter
    private int x;

    @DatabaseField(canBeNull = false)
    @Getter
    private int y;

    @DatabaseField(canBeNull = false)
    @Getter
    private int z;

    // Many-to-one relationship with Channel
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    @Getter
    private Channel channel;

    // No-arg constructor required by ORMLite
    public Transponder() {}

    public Transponder(Channel channel, boolean isTransmitter, boolean isWallSign, BlockFace facing,
                       Location location) {
        this.world = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
        this.id = generateId(location);
        this.channel = channel;
        this.isTransmitter = isTransmitter;
        this.isWallSign = isWallSign;
        this.facing = facing.name(); // store as String
    }

    public static String generateId(Location location) {
        return String.format("%s_%d_%d_%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    public BlockFace getBlockFace() {
        return BlockFace.valueOf(facing);
    }
}
