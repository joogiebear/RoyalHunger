package com.mystipixel.royalhunger;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A tiny standalone plugin that keeps players' hunger bar full so food never drains — with per-world
 * control. There is no vanilla gamerule for this (peaceful difficulty disables hunger but also stops
 * hostile mob spawning), so {@link HungerListener} pins the food level on the food-change event.
 *
 * <p>Config is deliberately minimal: a {@code mode} (blacklist/whitelist) and a {@code worlds} list.
 * Blacklist (the default) disables hunger everywhere except the listed worlds — so a hardcore world
 * can be listed to keep normal vanilla hunger there. Whitelist disables hunger only in the listed worlds.
 */
public final class RoyalHungerPlugin extends JavaPlugin {

    /** bStats project id. Identifies the plugin, not the server, so it is fixed rather than configurable. */
    private static final int BSTATS_PLUGIN_ID = 32732;

    private boolean whitelist;                       // true = whitelist mode, false = blacklist
    private boolean fullSaturation;                  // also pin saturation (fast regen) — see config
    private final Set<String> worlds = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        getServer().getPluginManager().registerEvents(new HungerListener(this), this);
        setupMetrics();
        getLogger().info("RoyalHunger enabled — hunger disabled in " + describeScope() + ".");
    }

    /** Re-read config.yml and rebuild the world set. Safe to call at runtime (used by /royalhunger reload). */
    public void reloadSettings() {
        reloadConfig();
        String mode = getConfig().getString("mode", "blacklist").toLowerCase(Locale.ROOT);
        this.whitelist = mode.equals("whitelist");
        if (!whitelist && !mode.equals("blacklist")) {
            getLogger().warning("mode '" + mode + "' is not 'blacklist' or 'whitelist' — defaulting to blacklist.");
        }
        this.fullSaturation = getConfig().getBoolean("full-saturation", true);
        worlds.clear();
        for (String w : getConfig().getStringList("worlds")) {
            if (w != null && !w.isBlank()) {
                worlds.add(w.toLowerCase(Locale.ROOT));
            }
        }
    }

    /**
     * Whether saturation is pinned too. Full saturation is not just "no hunger" — it grants the fast
     * saturated health regeneration permanently, which is a combat-balance decision; false keeps the
     * food bar pinned while health regenerates at vanilla's slower food-level rate.
     */
    public boolean fullSaturation() {
        return fullSaturation;
    }

    /** Whether hunger should be held full for players in this world, per the configured mode + list. */
    public boolean hungerDisabledIn(World world) {
        if (world == null) {
            return false;
        }
        boolean listed = worlds.contains(world.getName().toLowerCase(Locale.ROOT));
        return whitelist ? listed : !listed;
    }

    private String describeScope() {
        if (whitelist) {
            return worlds.isEmpty() ? "no worlds (whitelist is empty)" : worlds.size() + " whitelisted world(s)";
        }
        return worlds.isEmpty() ? "all worlds" : "all worlds except " + worlds.size() + " blacklisted";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("royalhunger.admin")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            reloadSettings();
            sender.sendMessage("§aRoyalHunger reloaded — hunger disabled in " + describeScope() + ".");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            if (!sender.hasPermission("royalhunger.admin")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            status(sender);
            return true;
        }
        sender.sendMessage("§eRoyalHunger §7— /" + label + " reload§7|§estatus");
        return true;
    }

    /**
     * Answer "why is hunger (not) draining here" without making the admin re-derive it from config:
     * the mode, the list, and — for a player — the verdict for the world they are standing in,
     * including whether that world matched the list. The usual culprit is a world-name mismatch,
     * which is exactly what the listed/not-listed line makes visible.
     */
    private void status(CommandSender sender) {
        sender.sendMessage("§eRoyalHunger §7— mode: §f" + (whitelist ? "whitelist" : "blacklist")
                + "§7, " + worlds.size() + " world(s) listed, saturation "
                + (fullSaturation ? "§fpinned §7(fast regen)" : "§fnot pinned §7(vanilla regen)") + ".");
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            return;
        }
        World world = player.getWorld();
        boolean listed = worlds.contains(world.getName().toLowerCase(Locale.ROOT));
        boolean disabled = hungerDisabledIn(world);
        sender.sendMessage("§7This world (§f" + world.getName() + "§7): hunger is "
                + (disabled ? "§adisabled" : "§cactive") + "§7 — "
                + (listed ? "listed" : "not listed") + " in the " + (whitelist ? "whitelist" : "blacklist") + ".");
    }
    /**
     * Anonymous usage reporting via bStats.
     *
     * <p>Server owners who want no reporting disable it globally in plugins/bStats/config.yml, which
     * is the mechanism bStats provides; the id itself is fixed because it names this plugin's project.
     */
    private void setupMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("mode", () -> getConfig().getString("mode", "blacklist")));
        metrics.addCustomChart(new SimplePie("full_saturation",
                () -> String.valueOf(getConfig().getBoolean("full-saturation", true))));
        metrics.addCustomChart(new SimplePie("worlds_listed",
                () -> String.valueOf(getConfig().getStringList("worlds").size())));
    }

}
