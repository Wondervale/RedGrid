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
            RedGrid.getInstance().getLogger().severe("Failed to close database connection!");
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
            RedGrid.getInstance().getLogger().severe(String.format(
                    "Failed to create or retrieve channel '%s' from database", id));
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

            transponderDao.createOrUpdate(transponder);

            return true;
        }
        catch (SQLException e) {
            RedGrid.getInstance().getLogger().severe("Failed to create transponder in database!");
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteTransponder(Sign sign) {
        try {
            String transponderId = Transponder.generateId(sign.getBlock().getLocation());
            Transponder transponder = transponderDao.queryForId(transponderId);

            if (transponder != null) {
                Channel channel = transponder.getChannel();

                // Delete the transponder
                transponderDao.delete(transponder);

                // If channel exists, check if it has any transponders left
                if (channel != null) {
                    long count = transponderDao.queryBuilder()
                                               .where()
                                               .eq("channel_id", channel.getName())
                                               .countOf();

                    if (count == 0) {
                        channelDao.delete(channel);

                        RedGrid.getInstance()
                               .getLogger()
                               .info(String.format("Deleted channel '%s' because it had no more transponders.", channel.getName()));
                    }
                }

                return true;
            } else {
                RedGrid.getInstance()
                       .getLogger()
                       .warning("Transponder not found in database for deletion.");
                return false;
            }
        }
        catch (SQLException e) {
            RedGrid.getInstance().getLogger().severe("Failed to delete transponder from database!");
            e.printStackTrace();
            return false;
        }
    }
}
