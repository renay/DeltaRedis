package com.gmail.tracebachi.deltaredis.nukkit.event;

import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import com.gmail.tracebachi.deltaredis.shared.DeltaRedisApi;
import com.google.common.base.Preconditions;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;

public class DeltaRedisMessageEvent extends Event {
    private final List<String> messageParts;
    private final String sendingServer;
    private final String channel;

    public DeltaRedisMessageEvent(@NonNull String sendingServer, @NonNull String channel, @NonNull List<String> messageParts) {
        Preconditions.checkArgument(!sendingServer.isEmpty(), "Empty sendingServer");
        Preconditions.checkArgument(!channel.isEmpty(), "Empty channel");

        this.channel = channel;
        this.sendingServer = sendingServer;
        this.messageParts = Collections.unmodifiableList(messageParts);
    }

    /**
     * @return Name of the server that sent the message.
     */
    public String getSendingServer() {
        return sendingServer;
    }

    /**
     * @return Name of the channel that the message is targeted at.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @return The message parts/data received.
     */
    public List<String> getMessageParts() {
        return messageParts;
    }

    /**
     * @return True if the message was sent by the current server or false
     */
    public boolean isSendingServerSelf() {
        return DeltaRedisApi.instance().getServerName().equals(sendingServer);
    }

    /**
     * @return Comma separated string of the sendingServer, channel, and message.
     */
    @Override
    public String toString() {
        return "(" + sendingServer + ", " + channel + ", " + String.join(", ", messageParts) + ")";
    }

    private static final HandlerList handlers = new HandlerList();

    /**
     * Used by the Nukkit event system
     */
    @NonNull
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Used by the Nukkit event system
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
