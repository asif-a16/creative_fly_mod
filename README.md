# Creative Fly Mod

Creative Fly Mod is a **client-side NeoForge mod** for Minecraft `1.21.1` that adds a toggleable flight mode in any gamemode, with adjustable speed and an on-screen speed indicator.

## Features

- Toggle fly mode with a keybind.
- Fly using movement keys (`WASD`) plus vertical controls (`Space` up, `Shift` down).
- Adjust flight speed in-game.
- Reset speed to normal instantly.
- Show current speed in a top-left HUD overlay while flight mode is enabled.
- Client-side behavior only (not required on dedicated servers).

## Default Keybinds

- `K` → Toggle flight mode
- `[` → Decrease speed by 10% of normal speed
- `]` → Increase speed by 10% of normal speed
- `R` → Reset speed to 100%

You can change all keybinds in Minecraft controls settings.

## Compatibility

- Minecraft: `1.21.1`
- NeoForge: `21.1.218`

## Build

From the project root on Windows:

```powershell
.\gradlew.bat build
```

Output jar:

- `build/libs/creativeflymod-1.0.2.jar`

## Install

1. Build the mod (or use a prebuilt jar).
2. Place the jar in your client `mods` folder.
3. Launch Minecraft with NeoForge `1.21.1`.

## Development Notes

- Main client implementation: `src/main/java/com/example/examplemod/CreativeFlyModClient.java`
- Language file: `src/main/resources/assets/creativeflymod/lang/en_us.json`
