# Ki Craft HUD Concepts

## PULSE_ARRAY

Status: currently active in `KicraftHud.java`.

Style:
- original asymmetrical resource array,
- compact energy core on the left connected to a cut-corner display,
- separate HP, Ki, and Stamina channels,
- Rage channel appears only during combat,
- player Ki color is used by the core and the Ki channel,
- dark hardware shell with a warm neutral accent, avoiding a scouter design.

## WARM_PANEL

Status: saved in `KicraftHud.java`.

Style:
- simple rectangular HUD,
- warm colors,
- orange/gold border,
- red/orange/yellow resource bars.

## COOL_PANEL

Status: saved in `KicraftHud.java`.

Style:
- same simple rectangular layout,
- cooler colors,
- blue/cyan border,
- pink/cyan/green/purple resource bars.

Current behavior:
- the layout stays fixed,
- the Ki bar, HUD accent, border and percentage values use the player's saved Ki color,
- default Ki color is cyan (`#45DFFF`).

## SCOUTER_HUD_V1

Status: active and preserved in `KicraftHud.java`.

Style:
- compact physical scouter module on the left,
- connected dark-green scanner display on the right,
- HP, Ki, and Stamina rows,
- Rage row appears only while the player is in combat,
- white/gray hardware casing, bright cyan scanner lens,
- Ki row uses the player's selected Ki color.

Implementation:
- `renderScouterHud(...)`
- `drawScouterModule(...)`
- `drawScouterDisplayDecorations(...)`
- `drawScouterMeter(...)`

This is the saved fallback design. Do not remove these methods when experimenting
with later HUD concepts unless this version is deliberately retired.

## ANGLED_COMBAT_HUD

Status: saved in `KicraftHud.java`.

Style:
- clean bars only,
- HP/KI bars share the same length,
- ST/RG bars share the same length,
- no top accent line or side spine,
- parallelogram resource bars,
- white labels and percentages inside the bars,
- border and accent follow the player's saved Ki color.

To switch concepts, change `ACTIVE_STYLE` in `KicraftHud.java`.
