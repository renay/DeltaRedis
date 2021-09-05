# DeltaRedis

DeltaRedis uses the in-memory data structure store, [Redis](http://redis.io/), to create a new communication
system for BungeeCord-linked Spigot servers.

BungeeCord uses player connections and special Bukkit/Spigot channels to communicate which makes it heavily dependent on
players. By using DeltaRedis, BungeeCord and servers can communicate, faster and easier, than the provided BungeeCord methods.

# APIs
There are API: 
[Spigot/NukkitX/BungeeCord](https://github.com/loperd/DeltaRedis/blob/master/shared/src/main/java/com/gmail/tracebachi/deltaredis/shared/DeltaRedisApi.java)

## How to include the API with Maven

```xml
    <repositories>
        <repository>
            <id>delta-repo</id>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        </repository>
    </repositories>
```

```xml
    <dependencies>
        <dependency>
            <groupId>com.gmail.tracebachi</groupId>
            <artifactId>deltaredis</artifactId>
            <version>3.1.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

## Using the API Publish System
```java
public void sendHelloToZee() {
  DeltaRedisApi api = DeltaRedisApi.instance();
  
  String serverName = "Creative";
  String channel = "Talk";
  String message = "Hi Zee!";
  
  // Publish the message
  api.publish(serverName, channel, message);
}

// Use the event to listen for messages
@EventHandler
public void onMessage(DeltaRedisMessageEvent event) {
  if(event.getChannel().equals("Talk")) {
    Player player = Bukkit.getPlayerExact("Zee");
    
    if(player != null) {
        player.sendMessage(event.getMessage());
    }
  }
}
```

## Finding a Player with the API 
```java
public void findZee() {
  DeltaRedisApi api = DeltaRedisApi.instance();
  
  api.findPlayer("Zee", (cachedPlayer) -> {
    if(cachedPlayer == null) {
    
      // :( No Zee found
      
    } else {
      
      // Found Zee on server!
      String server = cachedPlayer.getServer();
      
      // Send an announcement to everyone on the network
      api.sendAnnouncementToServer(Servers.SPIGOT, "I found Zee on " + server + "!");
      
      // Ask for a prize
      api.sendMessageToPlayer("Zee", "Do I win?\nWhat's my prize?");
    }
  }
}
```

# Licence ([GPLv3](http://www.gnu.org/licenses/gpl-3.0.en.html))
```
DeltaRedis - BungeeCord and Spigot plugin for multi-server communication.
Copyright (C) 2015  Trace Bachi (tracebachi@gmail.com)

DeltaRedis is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

DeltaRedis is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with DeltaRedis.  If not, see <http://www.gnu.org/licenses/>.
```
