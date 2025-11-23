# Bow Horse Control

A Minecraft Fabric mod for version 1.20.1 that allows free camera movement when using a bow on a horse.

## Features

- **Free Camera Movement**: When drawing a bow while riding a horse, the horse no longer automatically follows your camera direction
- **Independent Aiming**: You can freely look around and aim your bow without the horse turning
- **Movement Control**: The horse still responds to explicit movement key presses (WASD) while you're drawing the bow

## How It Works

The mod uses a Mixin to intercept the horse's movement calculation. When a player is drawing a bow while riding a horse, the mod prevents the horse from using the player's rotation vector for movement direction, allowing independent camera control.

## Building

1. Clone this repository
2. Run `./gradlew build` (or `gradlew.bat build` on Windows)
3. The built mod will be in `build/libs/`

## Installation

1. Install Fabric Loader for Minecraft 1.20.1
2. Install Fabric API
3. Place the mod JAR file in your `.minecraft/mods/` folder
4. Launch Minecraft with the Fabric profile

## License

MIT

