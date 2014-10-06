package com.mengcraft.rpg;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by zmy on 14-7-16.
 * GPLv2 licence
 */

public class RedName extends JavaPlugin {

    private static RedName plugin;
    private static Chat chat = null;

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        setupChat();
        getServer().getPluginManager().registerEvents(new Listener(), this);
        new RedNameTask().runTaskTimer(this, 3600, 3600);
    }

    private boolean setupChat() {
        try {
            RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
            if (chatProvider != null) {
                chat = chatProvider.getProvider();
            }
        } catch (Exception e) {
            String message = e.getMessage();
            getLogger().warning(message);
        }
        return (chat != null);
    }

    public static RedName getInstance() {
        return plugin;
    }

    private void checkPoint(Player player) {
        int point = getPoint(player);
        if (point % 10 == 0) {
            String message = ChatColor.RED + "玩家 " + player.getName() + " 的罪恶值达到 " + point + " 点!!!";
            getServer().broadcastMessage(message);
        }
    }

    private void setPoint(Player player, int i) {
        getConfig().set("player." + player.getName(), i > 0 ? i : null);
        String message = ChatColor.RED + "你当前罪恶值为 " + (i > 0 ? i : 0) + " 点";
        player.sendMessage(message);
        if (chat != null) {
            if (i < 1) {
                chat.setPlayerSuffix(player, null);
            } else {
                chat.setPlayerSuffix(player, "[" + i + "]");
            }
        }
    }

    private int getPoint(Player player) {
        return getConfig().getInt("player." + player.getName(), 0);
    }

    private class RedNameTask extends BukkitRunnable {

        @Override
        public void run() {
            Player[] players = getServer().getOnlinePlayers();
            for (Player player : players) {
                if (getPoint(player) > 0) {
                    int point = getPoint(player);
                    setPoint(player, point - 1);
                }
            }
            saveConfig();
        }
    }

    private class Listener implements org.bukkit.event.Listener {

        private final HashMap<String, ItemStack[]> inventoryMap;
        private final HashMap<String, ItemStack[]> armorMap;

        private Listener() {
            inventoryMap = new HashMap<String, ItemStack[]>();
            armorMap = new HashMap<String, ItemStack[]>();
        }

        @EventHandler
        public void changeWorld(PlayerChangedWorldEvent event) {
            if (chat != null) {
                int i = getPoint(event.getPlayer());
                if (i < 1) {
                    chat.setPlayerSuffix(event.getPlayer(), null);
                } else {
                    chat.setPlayerSuffix(event.getPlayer(), "[" + i + "]");
                }
            }
        }

        @EventHandler
        public void attack(EntityDamageByEntityEvent event) {
            boolean isPlayer = event.getEntity() instanceof Player
                    && event.getDamager() instanceof Player;
            if (isPlayer) {
                Player player = (Player) event.getEntity();
                Player attacker = (Player) event.getDamager();
                isPlayer = getPoint(player) < 1
                        && getPoint(attacker) < 1;
                if (isPlayer) {
                    String message = ChatColor.RED + "你攻击善良的玩家, 获得 +1 罪恶值";
                    attacker.sendMessage(message);
                    setPoint(attacker, 1);
                }
            }
        }

        @EventHandler
        public void death(PlayerDeathEvent event) {
            Random random = new Random();
            event.getDrops().clear();
            Player player = event.getEntity();
            int point = getPoint(player);
            ItemStack[] inventory = player.getInventory().getContents();
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (int i = 0; i < inventory.length; i = i + 1) {
                if (random.nextInt(100) < point) {
                    event.getDrops().add(inventory[i]);
                    inventory[i] = new ItemStack(Material.AIR);
                }
            }
            for (int i = 0; i < armor.length; i = i + 1) {
                if (random.nextInt(100) < point) {
                    event.getDrops().add(armor[i]);
                    armor[i] = new ItemStack(Material.AIR);
                }
            }
            inventoryMap.put(player.getName(), inventory);
            armorMap.put(player.getName(), armor);
            if (player.getKiller() != null) {
                Player killer = player.getKiller();
                int _point = getPoint(killer);
                if (point > _point) setPoint(killer, _point - 3);
                else {
                    setPoint(killer, _point + 1);
                    checkPoint(killer);
                }
            }
            setPoint(player, point - 2);
            new ReSpawnPlayer(player).runTaskLater(getInstance(), 60);
        }

        @EventHandler
        public void spawn(PlayerRespawnEvent event) {
            Player player = event.getPlayer();
            String name = player.getName();
            ItemStack[] inventory = inventoryMap.remove(name);
            if (inventory != null) {
                ItemStack[] armor = armorMap.remove(name);
                player.getInventory().setContents(inventory);
                player.getInventory().setArmorContents(armor);
            }
        }

        @EventHandler
        public void playerQuit(PlayerQuitEvent event) {
            String name = event.getPlayer().getName();
            inventoryMap.remove(name);
            armorMap.remove(name);
        }

        private class ReSpawnPlayer extends BukkitRunnable {
            private final String name;

            public ReSpawnPlayer(Player player) {
                this.name = player.getName();
            }

            @Override
            public void run() {
                try {
                    getServer().getPlayerExact(name).spigot().respawn();
                } catch (Exception e) {
                    getLogger().info("Not support auto re spawn on this server!");
                }
            }
        }
    }
}
