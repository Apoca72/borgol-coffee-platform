# Ratio Calculator — Drink Type Options & Timer Integration

**Date:** 2026-04-28
**Status:** Approved

## Summary

Enhance `brew-timer.html`'s ratio calculator with a two-level selection flow: brew method → drink type. Each drink type carries a research-backed ideal ratio that auto-populates the calculator. The user can customize the ratio freely before starting the timer, which shows the active amounts inline on the Start button.

## Problem

The current ratio calculator only provides generic ratio presets per brew method (e.g. V60: 1:15 / 1:16 / 1:17). There is no concept of drink types — users have no guidance on what ratio to start from for a Ristretto vs Lungo, or a Strong French Press vs a Mild one.

## Design

### Layout Change

A **Drink Type row** is inserted between the method grid and the ratio card. It is labeled "Drink Type" with a subtitle "choose a style to set its ideal ratio".

Flow:
1. User selects a **brew method** (existing grid) → drink type chips filter to that method's variants
2. User selects a **drink type chip** → ratio inputs auto-set to the ideal ratio; matching fine-tune preset chip activates
3. User optionally edits coffee/water inputs or fine-tune presets
4. User clicks **Start** → timer launches immediately using the current ratio

### Drink Type Data

Research-sourced ideal ratios (coffee : water/output by weight):

| Method     | Drink Type    | Ratio | Notes                          |
|------------|---------------|-------|--------------------------------|
| Espresso   | Ristretto     | 1:1   | Short, restricted shot         |
| Espresso   | Espresso      | 1:2   | Standard double (18g → 36ml)   |
| Espresso   | Lungo         | 1:3   | Long shot                      |
| Espresso   | Americano     | 1:6   | Espresso + hot water           |
| V60        | Light         | 1:17  | Delicate, tea-like             |
| V60        | Standard      | 1:15  | SCA guideline sweet spot       |
| V60        | Strong        | 1:13  | Full-bodied                    |
| French Press | Mild        | 1:15  | Clean, lighter body            |
| French Press | Standard    | 1:12  | Classic French Press           |
| French Press | Strong      | 1:10  | Bold, heavy body               |
| AeroPress  | Concentrate   | 1:6   | For diluting with water/milk   |
| AeroPress  | Standard      | 1:12  | Balanced                       |
| AeroPress  | Diluted       | 1:16  | Lighter cup                    |
| Moka Pot   | Standard      | 1:7   | Traditional moka               |
| Moka Pot   | Mild          | 1:9   | Less concentrated              |
| Cold Brew  | Concentrate   | 1:4   | Dilute 1:1 before serving      |
| Cold Brew  | Standard      | 1:8   | Ready-to-drink                 |

### Ratio Card Changes

- When a drink type is selected, the water/output input auto-updates to `coffee × ratio`
- The existing fine-tune preset chips update their `active` state to reflect the selected ratio
- Bidirectional editing is preserved — editing either input recalculates the display ratio
- A helper line below the inputs shows: "Espresso selected — ideal ratio auto-applied. Adjust below if you like."

### Timer Start Button

The Start button label updates dynamically to show the current ratio:
> `▶ Start — 18g coffee · 36ml`

This gives the user confidence they're using the right ratio before committing to the brew.

### State Logic

```
selectMethod(id)
  → sets currentMethod
  → resets to first drink type for that method
  → applies that drink type's ratio to inputs
  → rebuilds drink type chips
  → rebuilds ratio presets
  → updates Start button label

selectDrinkType(drinkType)
  → sets currentDrinkType
  → applies ratio: waterMl = coffeeG × drinkType.ratio
  → rebuilds drink type chips (active state)
  → rebuilds ratio presets (active state)
  → updates Start button label

input change (coffee or water)
  → recalculates ratio display
  → rebuilds drink type chips (deactivates all — custom ratio)
  → rebuilds ratio presets (active if within 0.1 of a preset)
  → updates Start button label
```

### Default State

On page load, Espresso is the default method (existing behavior). The default drink type for Espresso is `Espresso 1:2`, which sets coffee=18g, water=36ml.

Each method has a designated default drink type:
- Espresso → Espresso (1:2)
- V60 → Standard (1:15)
- French Press → Standard (1:12)
- AeroPress → Standard (1:12)
- Moka Pot → Standard (1:7)
- Cold Brew → Standard (1:8)

## Scope

- **In scope:** `brew-timer.html` (web)
- **Out of scope:** `BrewTimerPane.java` (desktop) — the desktop already shows ratio as a static chip; a separate feature would be needed to make it interactive
- **No backend changes** — all data is client-side JS

## Data Structure

Each `METHODS` entry gains a `drinkTypes` array and a `defaultDrinkType` string:

```js
{
  id: 'espresso',
  name: 'Espresso',
  icon: '☕',
  time: '25–30 s',
  defaultRatio: 2,
  defaultDrinkType: 'espresso',
  drinkTypes: [
    { id: 'ristretto', label: 'Ristretto', ratio: 1   },
    { id: 'espresso',  label: 'Espresso',  ratio: 2   },
    { id: 'lungo',     label: 'Lungo',     ratio: 3   },
    { id: 'americano', label: 'Americano', ratio: 6   },
  ],
  ratioPresets: [...],
  steps: [...]
}
```

The existing `ratioPresets` array remains for fine-tune chips; drink types are the new coarse-level selector above them.
