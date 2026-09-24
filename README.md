# RoyalHunger

Keeps players' hunger bars full so food never drains and nobody has to eat, with per-world control.

There is no vanilla gamerule for this. `saturatedRegeneration` and friends change how healing works,
not whether the bar empties, so the only way to switch hunger off is to pin the food level — which is
all this plugin does.

Part of the Royal plugin suite. Requires Paper 26.2 or newer.

---

## Behaviour

By default hunger is disabled in **every world**. The `worlds` list carves out exceptions, in whichever
direction you need:

| `mode` | Meaning |
|---|---|
| `blacklist` | Hunger is off everywhere **except** the listed worlds, which keep normal vanilla hunger |
| `whitelist` | Hunger is off **only** in the listed worlds; everywhere else is vanilla |

So a server that wants no hunger anywhere except a hardcore world uses `blacklist` with that world
listed. A server that wants vanilla hunger everywhere except its hub uses `whitelist` with the hub
listed.

Players in an affected world keep a full food bar, so they can always sprint and never need to eat. By
default saturation is kept topped up as well, which means permanent fast (saturated) health
regeneration; set `full-saturation: false` to keep the bar full but let health regenerate at vanilla's
slower rate. That is the better choice for a world with combat.

Players are topped off when they join, respawn, change world, and when the config is reloaded.

---

## Commands

```text
/royalhunger reload     Re-read config.yml, rebuild the world list and top off online players
/royalhunger status     Show the mode, the list, and whether hunger is off in your current world
```

Aliases: `/nohunger`, `/nh`, `/rh`.

## Permissions

```text
royalhunger.admin   default: op   /royalhunger reload, /royalhunger status
```

---

## Configuration

```yaml
# blacklist - hunger off in ALL worlds except those listed
# whitelist - hunger off ONLY in the worlds listed
mode: blacklist

# true  - also keep saturation full (permanent fast health regeneration)
# false - keep only the food bar full (vanilla-speed regeneration)
full-saturation: true

# World names, case-insensitive. Empty list + blacklist = hunger off everywhere.
worlds: []
```

World names are matched case-insensitively, and the list is re-read by `/royalhunger reload` without a
restart.

---

## Upgrading from NoHunger

This plugin was previously called **NoHunger**. If you are upgrading:

1. Rename `plugins/NoHunger/` to `plugins/RoyalHunger/` to keep your settings. A plugin's data folder
   follows its name, so without this it starts on defaults (hunger off everywhere).
2. Delete the old `NoHunger.jar`, or both will load and fight over the same events.
3. The permission is now `royalhunger.admin`; `nohunger.admin` no longer exists.

`/nohunger` still works as a command alias, so existing macros and scripts don't need changing.

## Metrics

Reports anonymous usage to [bStats](https://bstats.org/plugin/bukkit/RoyalHunger/32732). Turn it off
for the whole server in `plugins/bStats/config.yml`.
