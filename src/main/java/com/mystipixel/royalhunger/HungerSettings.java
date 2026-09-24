package com.mystipixel.royalhunger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The parsed config, as one immutable snapshot. {@link RoyalHungerPlugin} swaps in a new instance on
 * reload, so readers off the main thread (the bStats charts) never see a half-rebuilt world set.
 *
 * <p>Deliberately free of Bukkit types so the world-matching rules can be unit tested without a server.
 */
final class HungerSettings {

    private final boolean whitelist;        // true = whitelist mode, false = blacklist
    private final boolean fullSaturation;   // also pin saturation (fast regen) — see config
    private final Set<String> worlds;       // lower-cased world names

    private HungerSettings(boolean whitelist, boolean fullSaturation, Set<String> worlds) {
        this.whitelist = whitelist;
        this.fullSaturation = fullSaturation;
        this.worlds = Set.copyOf(worlds);
    }

    /** Anything other than "whitelist" (case-insensitive) is blacklist, the safe default. */
    static HungerSettings of(String mode, boolean fullSaturation, Collection<String> worldNames) {
        Set<String> worlds = new HashSet<>();
        for (String w : worldNames) {
            if (w != null && !w.isBlank()) {
                worlds.add(w.trim().toLowerCase(Locale.ROOT));
            }
        }
        return new HungerSettings(isWhitelist(mode), fullSaturation, worlds);
    }

    static boolean isWhitelist(String mode) {
        return mode != null && mode.trim().equalsIgnoreCase("whitelist");
    }

    static boolean isKnownMode(String mode) {
        return mode != null && (mode.trim().equalsIgnoreCase("whitelist") || mode.trim().equalsIgnoreCase("blacklist"));
    }

    boolean whitelist() {
        return whitelist;
    }

    String modeName() {
        return whitelist ? "whitelist" : "blacklist";
    }

    boolean fullSaturation() {
        return fullSaturation;
    }

    int listedCount() {
        return worlds.size();
    }

    boolean isListed(String worldName) {
        return worldName != null && worlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    /** Whether hunger should be held full in the named world, per the mode + list. */
    boolean hungerDisabledIn(String worldName) {
        if (worldName == null) {
            return false;
        }
        boolean listed = isListed(worldName);
        return whitelist ? listed : !listed;
    }

    String describeScope() {
        if (whitelist) {
            return worlds.isEmpty() ? "no worlds (whitelist is empty)" : worlds.size() + " whitelisted world(s)";
        }
        return worlds.isEmpty() ? "all worlds" : "all worlds except " + worlds.size() + " blacklisted";
    }
}
