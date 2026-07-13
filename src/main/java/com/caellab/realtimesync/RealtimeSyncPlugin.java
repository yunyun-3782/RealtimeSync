/*
 * CaelLab BY-SA Code License
 * Copyright (c) 2025 Yunyun(云云)
 * Source: https://github.com/yunyun-3782/RealtimeSync
 */
package com.caellab.realtimesync;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RealtimeSyncPlugin extends JavaPlugin {

    private ZoneOffset timezoneOffset;
    private long syncInterval;
    private boolean announceOnJoin;
    private String prefix;
    private final Set<String> disabledWorlds = new HashSet<>();
    private ScheduledTask syncTask;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        // 启动时在全局区域设一次 doDaylightCycle = false，防止自然流逝干扰同步
        disableDaylightCycle();

        startSyncTask();
        getServer().getPluginManager().registerEvents(new RealtimeSyncListener(this), this);
        getLogger().info("RealtimeSync v" + getDescription().getVersion()
                + " (Folia) | timezone=UTC" + timezoneOffset.getId()
                + " | interval=" + syncInterval + " ticks");
    }

    @Override
    public void onDisable() {
        if (syncTask != null) {
            syncTask.cancel();
        }
        getLogger().info("RealtimeSync disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("realtimesync-reload")) {
            loadConfig();
            disableDaylightCycle();
            if (syncTask != null) syncTask.cancel();
            startSyncTask();
            syncAll();
            sender.sendMessage(colorize(prefix + "&a配置已重载！时区: &eUTC" + timezoneOffset.getId()));
            return true;
        }

        if (command.getName().equalsIgnoreCase("time")) {
            syncAll();
            String now = getRealTime();
            sender.sendMessage(colorize(prefix + "&7当前真实时间: &e" + now + " &7(UTC" + timezoneOffset.getId() + ")"));
            if (sender instanceof Player) {
                Player player = (Player) sender;
                World world = player.getWorld();
                long gameTicks = world.getTime();
                int gameHour = (int) ((gameTicks / 1000 + 6) % 24);
                int gameMin = (int) ((gameTicks % 1000) * 60 / 1000);
                sender.sendMessage(colorize(prefix + "&7游戏时间: &e" +
                        String.format("%02d:%02d", gameHour, gameMin)));
            }
            return true;
        }

        return false;
    }

    /**
     * 启动时在全局区域设一次 doDaylightCycle = false
     * 只调一次，不用每次同步都设（RegionScheduler里设gameRule在Folia下不稳）
     */
    private void disableDaylightCycle() {
        Bukkit.getGlobalRegionScheduler().run(this, task -> {
            for (World world : Bukkit.getWorlds()) {
                if (disabledWorlds.contains(world.getName().toLowerCase())) continue;
                world.setGameRuleValue("doDaylightCycle", "false");
            }
            getLogger().info("doDaylightCycle disabled for all worlds");
        });
    }

    private void startSyncTask() {
        syncTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            syncAll();
        }, 1L, syncInterval);
    }

    private void syncAll() {
        long gameTicks = calculateGameTicks();

        for (World world : Bukkit.getWorlds()) {
            if (disabledWorlds.contains(world.getName().toLowerCase())) {
                continue;
            }
            // Folia: use RegionScheduler to run on the correct thread
            // 只设时间，不再设 gameRule（已在 onEnable 里设过）
            Bukkit.getRegionScheduler().run(this, world.getSpawnLocation(), task -> {
                world.setTime(gameTicks);
            });
        }
    }

    private long calculateGameTicks() {
        LocalTime localTime = LocalTime.now(timezoneOffset);
        int hour = localTime.getHour();
        int minute = localTime.getMinute();
        int second = localTime.getSecond();

        long ticks = ((hour - 6 + 24) % 24) * 1000L
                + minute * 1000L / 60
                + second * 1000L / 3600;

        return ticks % 24000;
    }

    public String getRealTime() {
        Instant now = Instant.now();
        ZonedDateTime zoned = now.atZone(timezoneOffset);
        return zoned.format(TIME_FMT);
    }

    public void loadConfig() {
        reloadConfig();

        String tzStr = getConfig().getString("timezone-offset", "+08:00:00");
        try {
            timezoneOffset = ZoneOffset.of(tzStr);
        } catch (Exception e) {
            getLogger().warning("Invalid timezone-offset: " + tzStr + ", falling back to +08:00:00");
            timezoneOffset = ZoneOffset.ofHours(8);
        }

        syncInterval = getConfig().getLong("sync-interval", 1200);
        announceOnJoin = getConfig().getBoolean("announce-on-join", true);
        prefix = colorize(getConfig().getString("prefix", "&a&l[RealtimeSync] &r"));

        disabledWorlds.clear();
        List<String> disabled = getConfig().getStringList("disabled-worlds");
        for (String world : disabled) {
            disabledWorlds.add(world.toLowerCase());
        }

        getLogger().info("Config loaded: timezone=UTC" + timezoneOffset.getId()
                + " interval=" + syncInterval
                + " disabled-worlds=" + disabledWorlds);
    }

    public static String colorize(String msg) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
    }

    public ZoneOffset getTimezoneOffset() { return timezoneOffset; }
    public long getSyncInterval() { return syncInterval; }
    public boolean isAnnounceOnJoin() { return announceOnJoin; }
    public String getPrefix() { return prefix; }
    public Set<String> getDisabledWorlds() { return disabledWorlds; }
}
