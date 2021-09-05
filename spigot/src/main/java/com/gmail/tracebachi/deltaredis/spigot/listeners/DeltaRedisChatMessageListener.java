/*
 * This file is part of DeltaRedis.
 *
 * DeltaRedis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaRedis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaRedis.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.deltaredis.spigot.listeners;

import com.gmail.tracebachi.deltaredis.shared.DeltaRedisChannels;
import com.gmail.tracebachi.deltaredis.shared.PluginSource;
import com.gmail.tracebachi.deltaredis.spigot.DeltaRedis;
import com.gmail.tracebachi.deltaredis.spigot.events.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.List;

import static com.gmail.tracebachi.deltaredis.shared.structure.SplitPatterns.NEWLINE;

/**
 * Created by Trace Bachi (tracebachi@gmail.com) on 10/18/15.
 */
public class DeltaRedisChatMessageListener implements Listener, PluginSource {
    private DeltaRedis plugin;

    public DeltaRedisChatMessageListener(DeltaRedis plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void shutdown() {
        unregister();
        plugin = null;
    }

    @EventHandler
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event) {
        String channel = event.getChannel();
        List<String> messageParts = event.getMessageParts();

        if (channel.equals(DeltaRedisChannels.SEND_ANNOUNCEMENT)) {
            String permission = messageParts.get(0);
            String[] lines = NEWLINE.split(messageParts.get(1));

            for (String line : lines) {
                if (permission.equals("")) {
                    Bukkit.broadcastMessage(line);
                } else {
                    Bukkit.broadcast(line, permission);
                }
            }
        }

        if (channel.equals(DeltaRedisChannels.SEND_MESSAGE)) {
            String receiverName = messageParts.get(0);
            String[] lines = NEWLINE.split(messageParts.get(1));

            if (receiverName.equalsIgnoreCase("console")) {
                for (String line : lines) {
                    Bukkit.getConsoleSender().sendMessage(line);
                }
            } else {
                Player receiver = Bukkit.getPlayerExact(receiverName);

                if (receiver == null) {
                    return;
                }

                for (String line : lines) {
                    receiver.sendMessage(line);
                }
            }
        }

        if (channel.equals(DeltaRedisChannels.RUN_CMD)) {
            String sender = messageParts.get(0);
            String command = messageParts.get(1);

            plugin.info("[RunCmd] sendingServer: " + event.getSendingServer() +
                    ", sender: " + sender +
                    ", command: /" + command);

            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}
