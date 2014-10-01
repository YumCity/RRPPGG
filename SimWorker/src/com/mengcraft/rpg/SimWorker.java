package com.mengcraft.rpg;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.mcstats.Metrics;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by zmy on 14-9-1.
 * GPLv2 licence.
 */

public class SimWorker extends JavaPlugin {

    private static SimWorker simWorker;

    public static SimWorker getInstance() {
        return simWorker;
    }

    @Override
    public void onLoad() {
        simWorker = this;
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        new Thread(new MetricsThread(this)).start();
        new SendAdvert().runTaskLater(this, 120);
        WorkerListener listener = new WorkerListener();
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getScheduler().runTaskTimer(this, listener
                , getConfig().getInt("Config.TimeOutByTick", 1200)
                , getConfig().getInt("Config.TimeOutByTick", 1200)
        );
    }

    private class SendAdvert extends BukkitRunnable {
        @Override
        public void run() {
            String[] strings = {
                    ChatColor.GREEN + "梦梦家高性能服务器出租"
                    , ChatColor.GREEN + "淘宝店 http://shop105595113.taobao.com"
            };
            getServer().getConsoleSender().sendMessage(strings);
        }
    }

    private class WorkerListener implements Listener, Runnable {

        private final HashMap<String, Integer> deathMap;

        public WorkerListener() {
            this.deathMap = new HashMap<String, Integer>();
        }

        @Override
        public void run() {
            for (Player player : getServer().getOnlinePlayers()) {
                player.setFoodLevel(player.getFoodLevel() - 1);
            }
        }

        @EventHandler
        private void onReSpawn(PlayerRespawnEvent event) {
            if (deathMap.containsKey(event.getPlayer().getName())) {
                getServer().getScheduler().runTaskLater(getInstance(), new SetFoodTask(event.getPlayer()), 1);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onDead(PlayerDeathEvent event) {
            if (event.getEntity().getHealth() < 1) {
                deathMap.put(event.getEntity().getName(), event.getEntity().getFoodLevel());
            }
        }

        @EventHandler(ignoreCancelled = true)
        private void onAttack(EntityDamageByEntityEvent event) {
            if (event.getDamager() instanceof Player) {
                Player attacker = (Player) event.getDamager();
                event.setDamage(event.getDamage() * attacker.getFoodLevel() / 20);
            }
            if (event.getEntity() instanceof Player) {
                Player entity = (Player) event.getEntity();
                event.setDamage(event.getDamage() * (2 - entity.getFoodLevel() / 20));
            }
        }

        @EventHandler(ignoreCancelled = true)
        private void onMine(BlockBreakEvent event) {
            if (event.getPlayer().getFoodLevel() < getConfig().getInt("Config.LowFoodsLevel", 5)) {
                event.getPlayer().sendMessage(ChatColor.RED + "你已经没力气破坏方块了");
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        private void onPlace(BlockPlaceEvent event) {
            if (event.getPlayer().getFoodLevel() < getConfig().getInt("Config.LowFoodsLevel", 5)) {
                event.getPlayer().sendMessage(ChatColor.RED + "你已经没力气放置方块了");
                event.setCancelled(true);
            }
        }

        private class SetFoodTask implements Runnable {
            private final Player player;

            public SetFoodTask(Player player) {
                this.player = player;
            }

            @Override
            public void run() {
                player.setFoodLevel(deathMap.remove(player.getName()));
                player.setHealth(getConfig().getDouble("Config.ReSpawnHealth", 5));
            }
        }
    }

    private class MetricsThread implements Runnable {
        private final Plugin plugin;

        public MetricsThread(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            try {
                new Metrics(plugin).start();
            } catch (IOException e) {
                getLogger().warning("Can not link to Metrics server!");
            }
        }
    }
}
