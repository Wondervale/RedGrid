package com.foxxite.RedGrid;

import java.util.List;

import com.foxxite.RedGrid.listeners.RedstoneListener;
import com.foxxite.RedGrid.listeners.SignListener;
import com.foxxite.RedGrid.listeners.WirelessListener;
import com.foxxite.RedGrid.models.Channel;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.adventure.LiteAdventureExtension;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class RedGrid extends JavaPlugin {

    @Getter
    static RedGrid instance;

    @Getter
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Getter
    private DatabaseManager databaseManager;

    @Getter
    private Component prefix = miniMessage.deserialize("<red>[<bold>RedGrid</bold>]</red> <gray>");

    @Getter
    private boolean listenToWorld = false;

    private LiteCommands<CommandSender> liteCommands;

    @Override
    public void onEnable() {
        // Plugin startup logic

        RedGrid.instance = this;

        databaseManager = new DatabaseManager();

        liteCommands = LiteBukkitFactory.builder("redgrid", this)
                                        //.commands(
                                        //        // your commands
                                        //        InteractCommand.class
                                        //)
                                        .extension(new LiteAdventureExtension<>(), config -> config
                                                .miniMessage(true)
                                                .legacyColor(true)
                                                .colorizeArgument(true)
                                                .serializer(miniMessage)
                                        )
                                        .build();

        getServer().getPluginManager().registerEvents(new SignListener(), this);
        getServer().getPluginManager().registerEvents(new RedstoneListener(), this);
        getServer().getPluginManager().registerEvents(new WirelessListener(), this);

        listenToWorld = true;

        getLogger().info(getPluginMeta().getName() + " " + getPluginMeta().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        deactivateAllChannels();

        if (liteCommands != null) {
            liteCommands.unregister();
        }

        if (databaseManager != null) {
            databaseManager.close(); // flush and safely close database
        }

        getLogger().info(getPluginMeta().getName() + " " + getPluginMeta().getVersion() + " has been disabled!");
    }

    void deactivateAllChannels() {
        try {
            // DB call synchronously
            List<Channel> channels = databaseManager.getChannelDao().queryForAll();

            // Apply changes to world (safe, we're still on main thread)
            for (Channel channel : channels) {
                databaseManager.resetChannelActivations(channel);
            }
        }
        catch (Exception e) {
            getLogger().severe("Failed to deactivate all channels on shutdown!");
            e.printStackTrace();
        }
    }
}
