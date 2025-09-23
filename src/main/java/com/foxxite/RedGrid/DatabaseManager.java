package com.foxxite.RedGrid;

import com.foxxite.RedGrid.models.Channel;
import com.foxxite.RedGrid.models.Transponder;
import com.foxxite.RedGrid.utils.SignType;
import com.foxxite.RedGrid.utils.Utils;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.Getter;

import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;

import java.io.File;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {

    private final @Getter ConnectionSource connectionSource;
    private final @Getter Dao<Channel, String> channelDao;
    private final @Getter Dao<Transponder, String> transponderDao;

    public DatabaseManager() {
        try {
            File dbFile = new File(RedGrid.getInstance().getDataFolder(), "database.db");
            if (!dbFile.exists()) dbFile.getParentFile().mkdirs();

            // Create ORMLite connection source
            connectionSource = new JdbcConnectionSource("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // Create tables if they don't exist
            TableUtils.createTableIfNotExists(connectionSource, Channel.class);
            TableUtils.createTableIfNotExists(connectionSource, Transponder.class);

            // Create DAOs
            channelDao = DaoManager.createDao(connectionSource, Channel.class);
            transponderDao = DaoManager.createDao(connectionSource, Transponder.class);

            RedGrid.getInstance().getLogger().info("Database initialized successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Properly closes the database connection.
     * Call this in your plugin's onDisable().
     */
    public void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
                RedGrid.getInstance().getLogger().info("Database closed successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Example async executor for database operations to prevent blocking the main thread.
     */
    public void runAsync(Runnable task) {
        RedGrid.getInstance().getServer().getScheduler().runTaskAsynchronously(RedGrid.getInstance(), task);
    }

    public Channel getOrCreateChannel(String id, UUID creator) {
        try {
            Channel channel = channelDao.queryForId(id);
            if (channel == null) {
                channel = new Channel(id, creator);
                channelDao.create(channel);
            }
            return channel;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean createTransponder(Channel channel, SignType signType, Sign sign) {
        try {
            Transponder transponder = new Transponder(
                    channel,
                    signType == SignType.TRANSMITTER,
                    Utils.isWallSign(sign),
                    Utils.getSignFacingDirection(sign),
                    sign.getBlock().getLocation()
            );

            transponderDao.create(transponder);

            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
