# Creative Fly Mod

Creative Fly Mod is a NeoForge mod for Minecraft 1.21.1 that adds a configurable fly mode with speed profiles, HUD feedback, and multiplayer safety controls.

## How It Works

1. Press K to arm fly mode.
2. Double-tap Space to enable movement flight.
3. Double-tap Space again to disable movement flight.

When active, movement is controlled by:

- W/A/S/D: horizontal movement
- Space: move up
- Shift: move down
- Sprint: speed multiplier while moving

## Features

- Arming/disarming toggle separate from actual flight movement
- Double-tap Space activation flow
- Adjustable speed with on-screen top-left speed HUD
- Instant reset speed action
- 3 speed profiles with profile switching and cycling keybinds
- Optional auto-arm on join (profile setting)
- Multiplayer opt-in gate (server-controlled)

## Default Keybinds

- K: Toggle fly mod armed/disarmed
- R: Reset speed to 100%
- O: Open profiles screen
- [Unbound by default] Decrease speed
- [Unbound by default] Increase speed
- [Unbound by default] Activate profile 1
- [Unbound by default] Activate profile 2
- [Unbound by default] Activate profile 3
- [Unbound by default] Cycle profile forward
- [Unbound by default] Cycle profile backward

Tip: bind the unbound actions in Controls under the Creative Fly Mod category.

## Singleplayer and Multiplayer

- Singleplayer: flight is allowed.
- Multiplayer: flight requires server opt-in from this mod.

Server owners can enable multiplayer use via the common config:

- Key: serverFlightOptIn
- File: config/creativeflymod-common.toml

## Compatibility

- Minecraft: 1.21.1
- NeoForge: 21.1.218

## Build

From project root on Windows:

	.\gradlew.bat build

Output jar:

- build/libs/creativeflymod-1.0.2.jar

## Install

1. Build the mod or use a release jar.
2. Place the jar in your client mods folder.
3. Start Minecraft with NeoForge 1.21.1.

## Source Layout

- Client runtime logic: src/main/java/com/example/examplemod/CreativeFlyModClient.java
- Profiles: src/main/java/com/example/examplemod/FlyProfileManager.java
- Profile GUI: src/main/java/com/example/examplemod/CreativeFlyProfilesScreen.java
- Server opt-in payload/state: src/main/java/com/example/examplemod/ServerFlightOptInPayload.java, src/main/java/com/example/examplemod/ServerOptInState.java
- Localization: src/main/resources/assets/creativeflymod/lang/en_us.json
