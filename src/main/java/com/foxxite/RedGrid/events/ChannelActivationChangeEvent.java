package com.foxxite.RedGrid.events;

import com.foxxite.RedGrid.models.Channel;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ChannelActivationChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Channel channel;

    private final int oldActivations;
    private final int newActivations;

    public ChannelActivationChangeEvent(Channel channel, int oldActivations, int newActivations) {
        this.channel = channel;
        this.oldActivations = oldActivations;
        this.newActivations = newActivations;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
