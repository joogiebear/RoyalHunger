package com.mystipixel.royalhunger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Pins the food level on {@link FoodLevelChangeEvent}. The join, respawn and world-change top-offs
 * cover a player who arrives low, since a still player fires no food event.
 */
public final class HungerListener implements Listener {

    private static final int FULL_FOOD = 20;
    private static final float FULL_SATURATION = 20f;

    private final RoyalHungerPlugin plugin;

    public HungerListener(RoyalHungerPlugin plugin) {
        this.plugin = plugin;
    }

    // HIGH so we override what most plugins set; a cancelled event means the bar isn't changing anyway.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!plugin.hungerDisabledIn(player.getWorld())) {
            return;
        }
        event.setFoodLevel(FULL_FOOD);
        if (plugin.fullSaturation()) {
            player.setSaturation(FULL_SATURATION);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        topOff(event.getPlayer());
    }

    // During the event the player is still in the world they died in, and vanilla resets their food
    // once it returns, so top off a tick later, when they are standing in the respawn world.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                topOff(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        topOff(event.getPlayer());
    }

    void topOff(Player player) {
        if (!plugin.hungerDisabledIn(player.getWorld())) {
            return;
        }
        player.setFoodLevel(FULL_FOOD);
        if (plugin.fullSaturation()) {
            player.setSaturation(FULL_SATURATION);
        }
        player.setExhaustion(0f);
    }
}
