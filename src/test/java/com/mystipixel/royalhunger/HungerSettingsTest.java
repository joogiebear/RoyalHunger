package com.mystipixel.royalhunger;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HungerSettingsTest {

    @Test
    void blacklistDisablesHungerEverywhereExceptListedWorlds() {
        HungerSettings s = HungerSettings.of("blacklist", true, List.of("world_hardcore"));
        assertTrue(s.hungerDisabledIn("world"));
        assertFalse(s.hungerDisabledIn("world_hardcore"));
    }

    @Test
    void emptyBlacklistDisablesHungerEverywhere() {
        HungerSettings s = HungerSettings.of("blacklist", true, List.of());
        assertTrue(s.hungerDisabledIn("world"));
        assertEquals("all worlds", s.describeScope());
    }

    @Test
    void whitelistDisablesHungerOnlyInListedWorlds() {
        HungerSettings s = HungerSettings.of("whitelist", true, List.of("hub"));
        assertTrue(s.hungerDisabledIn("hub"));
        assertFalse(s.hungerDisabledIn("world"));
    }

    @Test
    void emptyWhitelistDisablesHungerNowhere() {
        HungerSettings s = HungerSettings.of("whitelist", true, List.of());
        assertFalse(s.hungerDisabledIn("world"));
    }

    @Test
    void worldNamesMatchCaseInsensitivelyAndIgnoreSurroundingSpace() {
        HungerSettings s = HungerSettings.of("whitelist", true, List.of(" Hub "));
        assertTrue(s.hungerDisabledIn("HUB"));
        assertTrue(s.isListed("hub"));
    }

    @Test
    void blankAndNullEntriesAreIgnored() {
        HungerSettings s = HungerSettings.of("blacklist", true, Arrays.asList("", "  ", null, "world_nether"));
        assertEquals(1, s.listedCount());
    }

    @Test
    void duplicateEntriesAreCountedOnce() {
        HungerSettings s = HungerSettings.of("blacklist", true, List.of("hub", "HUB"));
        assertEquals(1, s.listedCount());
    }

    @Test
    void modeIsCaseInsensitiveAndUnknownModesFallBackToBlacklist() {
        assertTrue(HungerSettings.of("WhiteList", true, List.of()).whitelist());
        assertFalse(HungerSettings.of("whitelsit", true, List.of()).whitelist());
        assertFalse(HungerSettings.of(null, true, List.of()).whitelist());
        assertTrue(HungerSettings.isKnownMode(" Blacklist "));
        assertFalse(HungerSettings.isKnownMode("whitelsit"));
    }

    @Test
    void nullWorldNameNeverDisablesHunger() {
        assertFalse(HungerSettings.of("blacklist", true, List.of()).hungerDisabledIn(null));
    }

    @Test
    void fullSaturationIsCarriedThrough() {
        assertFalse(HungerSettings.of("blacklist", false, List.of()).fullSaturation());
    }
}
