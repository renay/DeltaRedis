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

import com.gmail.tracebachi.deltaredis.shared.DeltaRedisApi;
import com.gmail.tracebachi.deltaredis.shared.PluginSource;
import com.gmail.tracebachi.deltaredis.shared.structure.Channel;
import com.gmail.tracebachi.deltaredis.spigot.DeltaRedis;
import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.gmail.tracebachi.deltaredis.shared.ChatMessageHelper.format;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/28/15.
 */
public class RunCmdCommand implements CommandExecutor, PluginSource {
    private DeltaRedis plugin;

    public RunCmdCommand(DeltaRedis plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        Objects.requireNonNull(plugin.getCommand("runcmd")).setExecutor(this);
    }

    @Override
    public void unregister() {
        Objects.requireNonNull(plugin.getCommand("runcmd")).setExecutor(null);
    }

    @Override
    public void shutdown() {
        unregister();
        plugin = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NonNull Command command, @NonNull String s, @NonNull String[] args) {
        if (!sender.hasPermission("DeltaRedis.RunCmd")) {
            sender.sendMessage(format(
                    "NoPerm",
                    "DeltaRedis.RunCmd"));
            return true;
        }

        if (args.length <= 1) {
            sender.sendMessage(format(
                    "Usage",
                    "/runcmd server[,server,...] command"));
            sender.sendMessage(format(
                    "Usage",
                    "/runcmd ALL command"));
            return true;
        }

        DeltaRedisApi deltaApi = plugin.getApi();
        Set<String> servers = new HashSet<>(Arrays.asList(args[0].split(",")));
        Set<String> cachedServers = deltaApi.getCachedServers();
        String senderName = sender.getName();
        String commandStr = joinArgsForCommand(args);

        if (doesSetContain(servers, "PROXY")) {
            if (deltaApi.isBungeeCordOnline()) {
                deltaApi.sendServerCommand(Channel.PROXY, commandStr, senderName);

                sender.sendMessage(format("DeltaRedis.CommandSent", "PROXY"));
            } else {
                sender.sendMessage(format("DeltaRedis.BungeeNotAvailable"));
            }

            return true;
        }

        if (doesSetContain(servers, "ALL")) {
            deltaApi.sendServerCommand(Channel.SPIGOT, commandStr, senderName);

            sender.sendMessage(format("DeltaRedis.CommandSent", "ALL"));

            return true;
        }

        for (String serv : servers) {
            String correctedDest = getMatchInSet(cachedServers, serv);

            if (correctedDest != null) {
                deltaApi.sendServerCommand(correctedDest, commandStr, senderName);

                sender.sendMessage(format("DeltaRedis.CommandSent", serv));
            } else {
                sender.sendMessage(format("DeltaRedis.ServerNotFound", serv));
            }
        }

        return true;
    }

    private String joinArgsForCommand(String[] args) {
        return String.join(" ", Arrays.copyOfRange(args, 1, args.length));
    }

    private boolean doesSetContain(Set<String> set, String source) {
        for (String item : set) {
            if (item.equalsIgnoreCase(source)) {
                return true;
            }
        }
        return false;
    }

    private String getMatchInSet(Set<String> set, String source) {
        for (String item : set) {
            if (item.equalsIgnoreCase(source)) {
                return item;
            }
        }
        return null;
    }
}
