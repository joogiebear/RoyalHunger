# RoyalHunger

Keeps players' hunger bars full so food never drains and nobody has to eat, with per-world control.

There is no vanilla gamerule for this. `saturatedRegeneration` and friends change how healing works,
not whether the bar empties, so the only way to switch hunger off is to pin the food level — which is
all this plugin does.

Part of the Royal plugin suite.

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

Players in an affected world keep a full bar and full saturation, so sprinting and natural regeneration
behave as if they had just eaten.

---

## Commands

```text
/royalhunger reload     Re-read config.yml and rebuild the world list
```

Aliases: `/nohunger`, `/nh`, `/rh`.

## Permissions

```text
royalhunger.admin   default: op   /royalhunger reload
```

---

## Configuration

```yaml
# blacklist - hunger off in ALL worlds except those listed
# whitelist - hunger off ONLY in the worlds listed
mode: blacklist

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
