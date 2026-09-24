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
        getLogger().info("RoyalHunger enabled — hunger disabled in " + settings.describeScope() + ".");
    }

    /** Re-read config.yml and rebuild the world set. Safe to call at runtime (used by /royalhunger reload). */
    public void reloadSettings() {
        reloadConfig();
        String mode = getConfig().getString("mode", "blacklist");
        if (!HungerSettings.isKnownMode(mode)) {
            getLogger().warning("mode '" + mode + "' is not 'blacklist' or 'whitelist' — defaulting to blacklist.");
        }
        this.settings = HungerSettings.of(mode,
                getConfig().getBoolean("full-saturation", true),
                getConfig().getStringList("worlds"));
    }

    /**
     * Whether saturation is pinned too. Full saturation is not just "no hunger" — it grants the fast
     * saturated health regeneration permanently, which is a combat-balance decision; false keeps the
     * food bar pinned while health regenerates at vanilla's slower food-level rate.
     */
    public boolean fullSaturation() {
        return settings.fullSaturation();
    }

    /** Whether hunger should be held full for players in this world, per the configured mode + list. */
    public boolean hungerDisabledIn(World world) {
        return world != null && settings.hungerDisabledIn(world.getName());
    }

    /**
     * A player standing still generates no food event, so without this a player who was already low
     * when their world became hunger-free (reload, late enable) would stay low until they next moved.
     */
    private void topOffOnlinePlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            listener.topOff(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !SUBCOMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            sender.sendMessage("§eRoyalHunger §7— /" + label + " §ereload§7|§estatus");
            return true;
        }
        if (!sender.hasPermission("royalhunger.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadSettings();
            topOffOnlinePlayers();
            sender.sendMessage("§aRoyalHunger reloaded — hunger disabled in " + settings.describeScope() + ".");
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

    /**
     * Answer "why is hunger (not) draining here" without making the admin re-derive it from config:
     * the mode, the list, and — for a player — the verdict for the world they are standing in,
     * including whether that world matched the list. The usual culprit is a world-name mismatch,
     * which is exactly what the listed/not-listed line makes visible.
     */
    private void status(CommandSender sender) {
        HungerSettings s = settings;
        sender.sendMessage("§eRoyalHunger §7— mode: §f" + s.modeName()
                + "§7, " + s.listedCount() + " world(s) listed, saturation "
                + (s.fullSaturation() ? "§fpinned §7(fast regen)" : "§fnot pinned §7(vanilla regen)") + ".");
        if (!(sender instanceof Player player)) {
            return;
        }
        World world = player.getWorld();
        boolean disabled = s.hungerDisabledIn(world.getName());
        sender.sendMessage("§7This world (§f" + world.getName() + "§7): hunger is "
                + (disabled ? "§adisabled" : "§cactive") + "§7 — "
                + (s.isListed(world.getName()) ? "listed" : "not listed") + " in the " + s.modeName() + ".");
    }

    /**
     * Anonymous usage reporting via bStats.
     *
     * <p>Server owners who want no reporting disable it globally in plugins/bStats/config.yml, which
     * is the mechanism bStats provides; the id itself is fixed because it names this plugin's project.
     * The charts report the parsed settings (not raw config), so typos and blank entries don't skew them.
     */
    private void setupMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("mode", () -> settings.modeName()));
        metrics.addCustomChart(new SimplePie("full_saturation", () -> String.valueOf(settings.fullSaturation())));
        metrics.addCustomChart(new SimplePie("worlds_listed", () -> String.valueOf(settings.listedCount())));
    }

}
