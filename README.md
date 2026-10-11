# RoyalEnchanting

A SkyBlock-style enchanting table. Right-clicking an enchanting table opens a menu where the player puts
in an item, picks the enchant they want from everything that fits it, and picks the level — no random
rolls. Stronger enchants are gated behind the player's Enchanting skill and the table's Bookshelf Power.

Built for RoyalSkyblock on top of EcoEnchants: EcoEnchants' custom enchants and a chosen set of vanilla
ones share one table. Part of the Royal plugin suite.

---

## The menu

```text
 . . . . . . . . .
 . . . E E E E E .      E  enchants that fit the item (15 per page)
 . I . E E E E E .      I  the item slot: click it with an item, or shift-click one in your inventory
 . T . E E E E E .      T  help
 . . . . . . . . .
 < . . B X S . . >      B  Bookshelf Power   X  close   S  sort   < >  pages
```

- **Put an item in** by clicking the open slot with it on your cursor, or by shift-clicking it in your
  inventory. One item from a stack goes in. Books can't be enchanted here.
- **The list** shows every enchant in `enchants.yml` that can go on that item. Enchants you haven't
  unlocked still show, as a plain book in red, with what's missing. An enchant the item already has
  glows and shows its current level; one that conflicts with something on the item says what it would
  replace.
- **Sort** cycles default (the order in `enchants.yml`, which is by unlock tier) → missing first →
  A–Z → Z–A. The choice is remembered until you log out.
- **Click an enchant** to see its levels, I up to the highest the table offers, each with its cost,
  requirements and what clicking it would do:
  - a level above the item's current one **applies** it, replacing any conflicting enchant;
  - the level the item already has **removes** the enchant, for the same cost;
  - a lower level does nothing;
  - an enchant above the table's maximum (a VI from a boss book) can't be changed or removed here.
- **Take the item out** by clicking it (to your cursor) or shift-clicking it (to your inventory).
  Closing the menu, logging out, dying, a reload and a server stop all give it back; with a full
  inventory it drops at your feet.

Every apply and remove charges XP levels (and lapis, if `costs.lapis` is set; the default is none, as on
Hypixel). An apply then pays the rewards below.

## Bookshelf Power

The number of bookshelves around the table, counted exactly the way vanilla counts them: the ring two
blocks out from the table, on the table's level and the level above, each shelf needing air (or another
replaceable block) in the block between it and the table. Vanilla stops at 15; here the cap is 20
(`bookshelf-power.max`), so a full two-high ring of 32 shelves gives 20.

## Requirements and costs

Each enchant in `enchants.yml` asks for an **Enchanting skill level** (EcoSkills) and a **Bookshelf
Power**. Every level of every enchant costs, by default:

| Level | I | II | III | IV | V |
|---|---|---|---|---|---|
| XP levels | 5 | 10 | 15 | 20 | 30 |
| Lapis Lazuli | 1 | 2 | 3 | 4 | 5 |

Only plain lapis counts; custom items built on lapis (enchanted lapis) are never spent.
The table offers as many levels as the cost lists have entries, never more than the enchant's own
max-level.

**Fortune, Looting and Luck of the Sea are never offered**, whatever the config says. The vanilla
enchanting screen is also cancelled if anything else tries to open it (`block-vanilla-table`).

## Rewards

The vanilla `EnchantItemEvent` never fires for this table, so the `enchant_item` triggers EcoSkills and
EcoJobs normally react to don't either. The plugin pays instead when an apply raises an enchant above the
highest level that item has already been paid for:

| Reward | Default | Through |
|---|---|---|
| Enchanting skill XP | `3.5 * %levels% ^ 1.5` | EcoSkills API (XP multipliers apply) |
| Enchanter job pay | `%levels% * %job_level% * 0.5` coins | Vault |
| Enchanter job XP | `%levels% * 0.5` | EcoJobs API |

`%levels%` is the XP levels spent. Job rewards go only to players with the `enchanter` job active.
Removing an enchant pays nothing, and the item remembers the highest level it was paid for (in its
persistent data, through removes), so removing Sharpness I and putting it back pays nothing again, while
going on to Sharpness II does pay. The extra commands still run on every apply and remove.
Every formula, the skill and job ids, and a list of extra console commands are in `config.yml`.
Formulas take `+ - * / ^`, parentheses, `min max floor ceil round sqrt abs`, the placeholders listed
there and any PlaceholderAPI placeholder.

---

## Refunds

Two chances to get an enchant's cost back, by Enchanting level, in `refunds:` (config.yml). They stand in
for EcoSkills' Reimbursement and Overcompensation perks, whose effects only hook the vanilla table:

| Refund | From | Chance |
| --- | --- | --- |
| XP levels (Reimbursement) | Enchanting 10 | 0.5% per level from 10 (20.5% at 50) |
| Lapis (Overcompensation) | Enchanting 20 | 0.75% per level from 20; off by default, since the table costs no lapis |

Rewards (skill XP, job pay) are still given for a refunded enchant.

## Configuration

### enchants.yml

What the table offers, in the menu's default order. Anything not listed is not offered.

```yaml
enchants:
  harvesting:               # the enchant's key (EcoEnchants and vanilla enchants are both minecraft:<id>)
    name: "Harvesting"      # shown in the menu
    skill-level: 0          # Enchanting level needed
    bookshelf-power: 0      # Bookshelf Power needed
  sharpness:
    name: "Sharpness"
    skill-level: 0
    bookshelf-power: 0
    max-level: 5            # optional cap below the enchant's own max
    enabled: true           # optional
```

An enchant only appears for items it can go on: EcoEnchants' `targets` for custom enchants, vanilla's
rules for vanilla ones. The shipped file is RoyalSkyblock's unlock table:

| Enchanting | Power | Enchants |
|---|---|---|
| 0 | 0 | Harvesting, Prosperity, Sylvan Fortune, Swift Reel, Critical, Growth, Efficiency, Unbreaking, Sharpness, Power, Protection |
| 3 | 4 | Tremor, Siren's Call, Magic Seeker, Rooted, Stonebound, Barkskin, Tidebound |
| 5 | 8 | Green Thumb, Woodworker, Angler, Vicious, Sugar Rush, Smite, Bane of Arthropods |
| 8 | 12 | Replenish, Timber, Arborist, Frail, Spiked Hook, Lifesteal, Scavenger, Infinite Quiver |
| 12 | 16 | Leafblower, Executioner, Giant Killer, Cleave, Snipe, Overload, Rejuvenate |
| 16 | 20 | Thunderlord, Smelting Touch |

On RoyalSkyblock the file is generated by `rsb-content/generators/gen_p2_enchant_table.py`; edit the
generator, not the server copy.

### config.yml

Bookshelf Power cap, the cost lists, the skill used for requirements, the rewards, extra blocked
enchants and sounds. Every option is commented in the file.

### messages.yml

Every chat line and every name and lore line in the menu. `&` colour codes and `&#RRGGBB` hex.

## Commands

```text
/royalenchanting reload     Re-read config.yml, enchants.yml and messages.yml; open menus redraw
```

Alias: `/renchanting`.

## Permissions

```text
royalenchanting.use     default: true   Use enchanting tables (without it, the table does nothing)
royalenchanting.admin   default: op     /royalenchanting reload
```

---

## Integrations

All optional; the plugin soft-depends on each.

- **EcoEnchants** registers every custom enchant in the server's enchantment registry as
  `minecraft:<id>`, with its max level, conflicts and item targets behind the standard Bukkit
  `Enchantment` methods. The table uses only that Bukkit API, so it has no compile-time dependency on
  EcoEnchants. Descriptions in the menu come from EcoEnchants when it's there.
- **EcoSkills** and **EcoJobs** are called through their public API classes (`EcoSkillsAPI`,
  `EcoJobsAPI`) by reflection, since their shaded jars aren't something to compile against. When the
  API can't be reached, the skill level and job level fall back to the PlaceholderAPI placeholders in
  `config.yml`, and the rewards that need the API are skipped with one warning.
- **Vault** pays the job money. **PlaceholderAPI** placeholders work in formulas and commands.

The startup log lists which integrations were found.

---

## Building

```bash
mvn clean package     # target/RoyalEnchanting.jar
```

Requires Paper 26.2 or newer to run, and JDK 25 to build (paper-api 26.2 ships Java 25 bytecode).
