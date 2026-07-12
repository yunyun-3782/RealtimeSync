/*
 * CaelLab BY-SA Code License
 * Copyright (c) 2025 Yunyun(云云)
 * Source: https://github.com/yunyun-3782/RealtimeSync
 */
package com.caellab.realtimesync;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RealtimeSyncListener implements Listener {

    private final RealtimeSyncPlugin plugin;

    public RealtimeSyncListener(RealtimeSyncPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isAnnounceOnJoin()) return;

        Player player = event.getPlayer();
        String time = plugin.getRealTime();
        String prefix = plugin.getPrefix();

        player.sendMessage(RealtimeSyncPlugin.colorize(
                prefix + "&7当前真实时间: &e" + time + " &7(UTC" + plugin.getTimezoneOffset().getId() + ")"));
    }
}
