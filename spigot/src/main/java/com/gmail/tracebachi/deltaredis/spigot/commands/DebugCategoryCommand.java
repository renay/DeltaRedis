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
package com.gmail.tracebachi.deltaredis.spigot.commands;

import com.gmail.tracebachi.deltaredis.shared.DeltaRedisChannels;
import com.gmail.tracebachi.deltaredis.shared.PluginSource;
import com.gmail.tracebachi.deltaredis.shared.structure.Channel;
import com.gmail.tracebachi.deltaredis.spigot.DeltaRedis;
import com.gmail.tracebachi.deltaredis.spigot.events.DebugCategoryChangeEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Objects;

import static com.gmail.tracebachi.deltaredis.shared.ChatMessageHelper.format;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/28/15.
 */
public class DebugCategoryCommand implements CommandExecutor, PluginSource {
    private DeltaRedis plugin;

    public DebugCategoryCommand(DeltaRedis plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        Objects.requireNonNull(plugin.getCommand("setdebug")).setExecutor(this);
    }

    @Override
    public void unregister() {
        Objects.requireNonNull(plugin.getCommand("setdebug")).setExecutor(null);
    }

    @Override
    public void shutdown() {
        unregister();
        plugin = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!sender.hasPermission("DeltaRedis.DebugCategory")) {
            sender.sendMessage(format(
                    "NoPerm",
                    "DeltaRedis.DebugCategory"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(format(
                    "Usage",
                    "/setdebug <category> <on|off>"));
            return true;
        }

        if (args[1].equalsIgnoreCase("on")) {
            DebugCategoryChangeEvent event = new DebugCategoryChangeEvent(args[0], true);
            plugin.getServer().getPluginManager().callEvent(event);

            if (event.shouldForwardToBungee()) {
                plugin.getApi().publish(
                        Channel.PROXY,
                        DeltaRedisChannels.DEBUG_CATEGORY_CHANGE,
                        sender.getName(),
                        args[0],
                        "1");
            }

            sender.sendMessage(format(
                    "DeltaRedis.DebugCategoryChange",
                    args[0],
                    "on"));
        } else if (args[1].equalsIgnoreCase("off")) {
            DebugCategoryChangeEvent event = new DebugCategoryChangeEvent(args[0], false);
            plugin.getServer().getPluginManager().callEvent(event);

            if (event.shouldForwardToBungee()) {
                plugin.getApi().publish(
                        Channel.PROXY,
                        DeltaRedisChannels.DEBUG_CATEGORY_CHANGE,
                        sender.getName(),
                        args[0],
                        "0");
            }

            sender.sendMessage(format(
                    "DeltaRedis.DebugCategoryChange",
                    args[0],
                    "off"));
        } else {
            sender.sendMessage(format(
                    "Usage",
                    "/setdebug <category> <on|off>"));
        }

        return true;
    }
}
