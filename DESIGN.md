# SplitEase design system

A small, layered system so every screen is built from the same few decisions. Everything below maps
directly to Figma, so a designer can rebuild or extend it without guessing.

## 1. Layers (build bottom-up, never skip a layer)

| Layer | Figma equivalent | Code |
|---|---|---|
| **Foundations**: color, type, spacing, radius, shape | Variables / styles | `ui/theme/Theme.kt`, `Type.kt`, `Dimens.kt` |
| **Components**: avatar, category badge, hero card, empty state, dialogs, pickers | Components / variants | `ui/common/Components.kt` |
| **Patterns**: list row, bottom sheet form, stat pill | Component sets | inside screen files, built only from components |
| **Screens**: Home, Group dashboard, Add/Edit expense, Edit group, Settle up | Pages / frames | `ui/home`, `ui/group`, `ui/settle` |

Rule: a screen may use components and tokens, but never raw colors, raw font sizes or raw spacing numbers.

## 2. Auto layout mapping

Compose `Row` / `Column` are Figma auto layout. Use the same vocabulary:

| Figma auto layout | Compose |
|---|---|
| Direction horizontal / vertical | `Row` / `Column` |
| Gap between items | `Arrangement.spacedBy(Spacing.x)` |
| Padding | `Modifier.padding(Spacing.x)` |
| Alignment | `verticalAlignment` / `horizontalAlignment` |
| Fill container | `Modifier.fillMaxWidth()` / `weight(1f)` |
| Hug contents | default (no size modifier) |
| Clip content + corner radius | `Modifier.clip(RoundedCornerShape(Radius.x))` |

## 3. Tokens

**Spacing** (`Spacing`): `xxs 4 · xs 8 · sm 12 · md 16 · lg 20 (screen gutter) · xl 24 · xxl 32`

**Radius** (`Radius`): `xs 8 · sm 12 · tile 18 · row 20 · card 24 · hero 28`

**Color** (light / dark, Material 3 roles):
- Brand gradient: teal `#0F766E` → indigo `#4F46E5` (dark: `#115E59` → `#3730A3`)
- Primary `#0F766E` / `#5EEAD4` · Secondary `#4F46E5` / `#A5B4FC` · Accent `#F59E0B`
- Background `#F3F6F5` / `#0B1211`
- Money: owed `#16A34A` / `#4ADE80`, owe `#DC2626` / `#F87171` (always paired with a text label, never color alone)
- Category tints: food orange, travel sky, utilities yellow, entertainment purple, rent green, shopping pink, other slate

**Typography**: **Plus Jakarta Sans** (SIL Open Font License, free for commercial use; license in `FONT-LICENSE-OFL.txt`).
One variable font file, weights 400 / 500 / 600 / 700 / 800.

| Role | Size / weight | Used for |
|---|---|---|
| displaySmall | 36 / ExtraBold | Hero balances |
| headlineLarge | 32 / ExtraBold | App title |
| headlineSmall | 24 / Bold | Sheet titles |
| titleLarge / Medium / Small | 22 / 16 / 14 · Bold / SemiBold | Section and row titles |
| bodyMedium / Small | 14 / 12 · Regular | Secondary text |
| labelLarge / Medium / Small | 14 / 12 / 11 · SemiBold / Medium | Buttons, chips, captions |

## 4. Layer names to use in Figma

Name frames like the code so handoff is obvious:
`Home / Overview card`, `Home / Group card`, `Group / Hero`, `Group / Expense row`,
`Group / Member row`, `Sheet / Add expense`, `Sheet / Edit group`, `Settle / Payment card`.

## 5. Interaction rules

- Tap an expense to edit it; swipe left to delete (with Undo).
- Destructive actions always ask first (delete group, remove member).
- Haptics: light tick on opening sheets, confirm pulse on save / delete / settle.
- Numbers count up when they change; list items animate in, out and reorder.
- Respect system dark mode and edge-to-edge insets on every screen.
