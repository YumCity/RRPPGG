package com.mengcraft.rpg;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

/**
 * Created by zmy on 14-9-1.
 * GPLv2 licence.
 */

public class SimWorker extends JavaPlugin {
    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        new SendAdvert().runTaskLater(this, 120);
        WorkerListener listener = new WorkerListener();
        Bukkit.getServer().getPluginManager().registerEvents(listener, this);
        listener.runTaskTimer(this, 600, 600);
    }

    private class SendAdvert extends BukkitRunnable {
        @Override
        public void run() {
            String[] strings = {
                    ChatColor.GREEN + "梦梦家高性能服务器出租"
                    , ChatColor.GREEN + "淘宝店 http://shop105595113.taobao.com"
            };
            Bukkit.getConsoleSender().sendMessage(strings);
        }
    }

    private class WorkerListener extends BukkitRunnable implements Listener {

        private final HashMap<String, Integer> pointMap;

        public WorkerListener() {
            pointMap = new HashMap<>();
        }

        @Override
        public void run() {
            for (String key : pointMap.keySet()) {
                if (pointMap.get(key) > getConfig().getInt("Config.ActTime", 30)) {
                    effect(key);
                }
            }
            pointMap.clear();
        }

        private void effect(String key) {
            Player player = getServer().getPlayerExact(key);
            if (player.isOnline() && player.getFoodLevel() > 1) {
                player.setFoodLevel(player.getFoodLevel() - 1);
            }
        }

        private void addPoint(String name) {
            Integer value = pointMap.get(name);
            if (value != null) {
                value = value + 1;
                pointMap.put(name, value);
            } else {
                pointMap.put(name, 1);
            }
        }

        @EventHandler
        private void onAttack(EntityDamageByEntityEvent event) {
            if (event.getDamager() instanceof Player) {
                Player damager = (Player) event.getDamager();
                addPoint(damager.getName());
                event.setDamage(event.getDamage() * damager.getFoodLevel() / 20);
            }
            if (event.getEntity() instanceof Player) {
                Player entity = (Player) event.getEntity();
                addPoint(entity.getName());
                event.setDamage(event.getDamage() * (2 - entity.getFoodLevel() / 20));
            }
        }

        @EventHandler
        private void onMine(BlockBreakEvent event) {
            if (event.getPlayer().getFoodLevel() > getConfig().getInt("Config.LowFood", 5)) {
                String name = event.getPlayer().getName();
                addPoint(name);
            } else {
                event.getPlayer().sendMessage(ChatColor.RED + "你已经没力气破坏方块了");
                event.setCancelled(true);
            }
        }

        @EventHandler
        private void onPlace(BlockPlaceEvent event) {
            if (event.getPlayer().getFoodLevel() > getConfig().getInt("Config.LowFood", 5)) {
                String name = event.getPlayer().getName();
                addPoint(name);
            } else {
                event.getPlayer().sendMessage(ChatColor.RED + "你已经没力气放置方块了");
                event.setCancelled(true);
            }
        }
    }
}
