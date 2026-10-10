package com.mystipixel.royalhunger;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

/**
 * Keeps the hunger bar full per world. No vanilla gamerule does this (peaceful also stops hostile
 * mob spawning), so {@link HungerListener} pins the food level on the food-change event.
 */
public final class RoyalHungerPlugin extends JavaPlugin {

    // identifies the plugin's bStats project, not the server, so it is fixed rather than configurable
    private static final int BSTATS_PLUGIN_ID = 32732;

    private static final List<String> SUBCOMMANDS = List.of("reload", "status");

    // Replaced wholesale on reload; volatile because the bStats charts read it off the main thread.
    private volatile HungerSettings settings = HungerSettings.of("blacklist", true, List.of());
    private HungerListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        listener = new HungerListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        // Covers players already online when the plugin is enabled after startup (e.g. by a plugin manager).
        topOffOnlinePlayers();
        setupMetrics();
        getLogger().info("RoyalHunger enabled: hunger disabled in " + settings.describeScope() + ".");
    }

    public void reloadSettings() {
        reloadConfig();
        String mode = getConfig().getString("mode", "blacklist");
        if (!HungerSettings.isKnownMode(mode)) {
            getLogger().warning("mode '" + mode + "' is not 'blacklist' or 'whitelist', defaulting to blacklist.");
        }
        this.settings = HungerSettings.of(mode,
                getConfig().getBoolean("full-saturation", true),
                getConfig().getStringList("worlds"));
    }

    /** Full saturation also grants fast saturated regen permanently, so it is a combat-balance choice. */
    public boolean fullSaturation() {
        return settings.fullSaturation();
    }

    public boolean hungerDisabledIn(World world) {
        return world != null && settings.hungerDisabledIn(world.getName());
    }

    // a standing-still player gets no food event, so one who was low at reload or late enable stays low
    private void topOffOnlinePlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            listener.topOff(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !SUBCOMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            sender.sendMessage("§eRoyalHunger§7: /" + label + " §ereload§7|§estatus");
            return true;
        }
        if (!sender.hasPermission("royalhunger.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadSettings();
            topOffOnlinePlayers();
            sender.sendMessage("§aRoyalHunger reloaded: hunger disabled in " + settings.describeScope() + ".");
        } else {
            status(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission("royalhunger.admin")) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
    }

    // the listed/not-listed line exposes the usual culprit: a world-name mismatch
    private void status(CommandSender sender) {
        HungerSettings s = settings;
        sender.sendMessage("§eRoyalHunger§7, mode: §f" + s.modeName()
                + "§7, " + s.listedCount() + " world(s) listed, saturation "
                + (s.fullSaturation() ? "§fpinned §7(fast regen)" : "§fnot pinned §7(vanilla regen)") + ".");
        if (!(sender instanceof Player player)) {
            return;
        }
        World world = player.getWorld();
        boolean disabled = s.hungerDisabledIn(world.getName());
        sender.sendMessage("§7This world (§f" + world.getName() + "§7): hunger is "
                + (disabled ? "§adisabled" : "§cactive") + "§7, "
                + (s.isListed(world.getName()) ? "listed" : "not listed") + " in the " + s.modeName() + ".");
    }

    // opt-out is global (plugins/bStats/config.yml); charts read parsed settings so typos don't skew them
    private void setupMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("mode", () -> settings.modeName()));
        metrics.addCustomChart(new SimplePie("full_saturation", () -> String.valueOf(settings.fullSaturation())));
        metrics.addCustomChart(new SimplePie("worlds_listed", () -> String.valueOf(settings.listedCount())));
    }

}
