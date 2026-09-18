# EmpowerSMP

A Paper 1.21 plugin implementing the "Empower SMP" skill tree system: five
trees (Mobility, Damage, Vitality, Prosperity, Tenacity), each with 8 levels.
Progression is **admin-driven** — there's no grind/XP system. You run a
command to grant a level after a player wins an event, and the plugin
instantly (re)applies every effect they're entitled to.

## Building

```
mvn clean package
```

Requires internet access (Maven pulls Paper's API from `repo.papermc.io`).
The shaded jar lands in `target/EmpowerSMP.jar` — drop that into your
server's `plugins/` folder.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/empower give <player> <tree> <amount>` | `empowersmp.admin` (op by default) | Adds levels (use a negative number to remove) |
| `/empower set <player> <tree> <level>` | `empowersmp.admin` | Sets an exact level (0-8) |
| `/empower levels [player]` | everyone | Shows current levels |
| `/empower choice <waterbreathing\|nightvision>` | everyone | Picks the Tenacity Level 1 permanent effect |
| `/empower info` | everyone | Lists the five trees |

Trees: `Mobility`, `Damage`, `Vitality`, `Prosperity`, `Tenacity` (case-insensitive).

## What's implemented, and how

Most rows in your design doc map directly onto Bukkit potion effects,
attributes, or enchantments and are applied automatically whenever a level
changes (join, respawn, or right after `/empower give|set`). A handful of
rows aren't native Minecraft mechanics — here's exactly how each of those was
interpreted, so you can adjust anything that doesn't match what you had in
mind:

- **Mobility L3 "Weaving"** — this is the real Weaving status effect added in
  the 1.21 Tricky Trials update, applied permanently.
- **Mobility L8 "Sharpness 6 Spear"** — grants a one-time real **Spear**
  (`Material.SPEAR`, added in the Mounts of Mayhem update, Dec 2025)
  enchanted with Sharpness VI via `ItemMeta#addEnchant(..., true)`, the same
  "ignore level restriction" mechanism an NBT give-command generator uses to
  produce over-leveled enchants — just done in code instead of via `/give`.
- **Damage L3 "Override Sharpness Limits to Sharpness 6", L6 "Sweeping Edge
  V", and Vitality L4 "Allow Protection IV"** — EmpowerSMP now enforces these
  enchant caps itself (`EnchantCapListener`/`EnchantCapRules`), so it no
  longer needs — and actively conflicts with — a separate enchant-limiter
  plugin. **Uninstall EnchantLimiter (or any similar plugin) before running
  this**, or the two will fight over the same items. By default: Sharpness/
  Smite/Bane of Arthropods cap at IV, and Protection/Blast/Projectile/Fire
  Protection cap at III, for every player. Reaching Damage L3 raises the
  Sharpness-family cap to VI; Damage L6 raises Sweeping Edge's cap to V;
  Vitality L4 raises the Protection-family cap to IV. These caps are
  enforced at anvils, enchanting tables, item pickups, inventory clicks
  (covers villager trades and moving items out of chests), and a periodic
  sweep of online players' inventories for anything else (commands, other
  plugins). All the base/elevated levels and unlock levels are configurable
  under `enchant-caps` in `config.yml`.
- **Tenacity L4 "Override Armor Limit to Netherite"** — vanilla doesn't
  restrict armor tiers by default, so there's nothing to override unless
  you're running a separate plugin that caps armor tier. This is a no-op
  hook; if you have such a restriction plugin, tell me its name/API and I'll
  add the bypass.
- **Tenacity L6 "Put on 12 Hearts"** — reads as ensuring at least 12 hearts
  (24 HP) of max health once Tenacity hits 6, independent of the Vitality
  tree. Implemented as a floor that's combined with whatever Vitality grants
  (the higher of the two always wins — health values never stack additively
  between trees).
- **Absorption (Vitality L7, Tenacity L5/L7)** — Minecraft only has one
  Absorption effect slot, so the three sources are combined by taking the
  *highest* amplifier among them (Tenacity L7 = Absorption II is the
  strongest, and wins over the others once reached).
- **Prosperity L4 "Potion Effects Multiplied by 1.5x"** — applied as a 1.5x
  *duration* multiplier on potions the player drinks (amplifier/strength is
  left alone, since multiplying that isn't well-defined for every effect).
- **Prosperity L6 "Villagers Give 2x Normal Trade"** — doubles the output
  item count at the moment a player clicks a trade result. This is a
  best-effort implementation; some edge cases (rapid shift-clicking through
  a trade) aren't perfectly covered.
- **Crit chance (Damage L2/4/7/8)** — rolled on every player melee hit that
  isn't already a natural vanilla crit (falling attack), multiplying damage
  by a configurable factor (default 1.5x) on success.
- **Hero of the Village (Prosperity L1-5)** — vanilla only goes up to
  Hero of the Village V from actual village-defense; this plugin grants the
  effect directly at the listed amplifiers (II/IV/VI/VIII/X) regardless of
  in-game village defense.

Everything else (Speed tiers, Strength tiers, Dolphin's Grace, Swift Sneak
III / Soul Speed III on boots, max health tiers, Regeneration tiers,
Resistance, Fire Resistance, Water Breathing/Night Vision choice, Knockback
Resistance, ore drop bonus, XP multiplier) is a direct, literal
implementation of the row in your doc.

## Data storage

Player progress is stored as flat YAML files under
`plugins/EmpowerSMP/playerdata/<uuid>.yml`, cached in memory while online,
autosaved every 5 minutes (configurable), and saved on quit/shutdown.

## Config

See `config.yml` for tunables: the optional Mobility L6 Speed III toggle,
Weaving's cooldown/strength, crit damage multiplier, anvil override
thresholds, ore/XP/potion/trade multipliers, and Tenacity L8 knockback
resistance value.

## Notes / next steps

- Not yet included: a GUI (e.g. `/empower menu`) showing progress bars —
  happy to add one if useful.
- Not yet included: an actual *event* framework (e.g. auto-running
  minigames). This plugin only handles the reward side; you said progression
  should come from events you already run, so levels are granted manually.
- The project targets Paper 1.21.11's API (Mounts of Mayhem, Dec 2025) so
  that `Material.SPEAR` resolves. If you're on a different Paper version,
  bump the `paper-api` version in `pom.xml` to match — a few registry-based
  names (`Attribute`, `Enchantment`, `Particle`) changed between 1.20.x and
  1.21.x.

