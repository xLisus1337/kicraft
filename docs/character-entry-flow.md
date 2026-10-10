# Character Entry Flow (Future Plan)

## Goal

Replace the normal first interaction after launching Minecraft with a Ki Craft
character-first flow.

```
Bootstrap loading screen -> optional intro cinematic -> character slots -> play destination
```

## Player Flow

1. The game opens a Ki Craft bootstrap screen before the usual title choices.
2. The screen displays `Initializing Ki Craft...` while the mod finishes loading
   its client configuration, character-slot data, and presentation assets.
3. When ready, an intro cinematic starts automatically.
4. The player can use `Skip` or `Esc` at any time.
5. The cinematic opens the character-slot screen.
6. The player can create, select, edit, or delete one of three character slots.
7. After selecting a character, the player chooses `Singleplayer` or `Multiplayer`.

## Intro Cinematic

- Build it as an in-game client renderer, not an MP4/video player.
- Use scripted camera movement, scene images or world rendering, particles,
  sound, and localized text.
- Save an `introSeen` client setting after the first full viewing.
- Later launches can go directly to the character screen, with a `Replay Intro`
  option available there.

## Character Slots

- Maximum slots: 3 per player.
- Each slot stores identity data such as name, race, Ki color, and future
  cosmetic choices.
- The selected slot is passed into the selected singleplayer world or server
  connection.

## Multiplayer Rule

- Progression, statistics, transformations, inventory, and unlocks remain
  server-authoritative.
- They are stored per server/world, player UUID, and character slot.
- Local client slot files must never grant stats on an arbitrary multiplayer
  server.
- Shared characters across several worlds are only allowed on an explicitly
  controlled Ki Craft server network in a later phase.

## Suggested Modules

- `client.startup` - bootstrap screen and intro state.
- `client.cinematic` - scripted scene, camera, sound, skip handling.
- `client.character` - slot selection and character creation screens.
- `server.character` - per-server character-slot save data and validation.
- `network` - selected-slot handshake after joining a server.

## Acceptance Criteria

- First launch shows the bootstrap screen, then the skippable intro, then
  character selection.
- The player can create no more than three slots.
- A selected character can enter singleplayer or multiplayer.
- Death, logout, and reconnect preserve the active server-side character.
- Local files cannot be edited to obtain multiplayer stats.
