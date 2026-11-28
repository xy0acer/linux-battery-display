# Linux Battery Display (for Minecraft)
This is a small utility mod for Minecraft versions `26.1`, `26.2` and `26.3`. When you enter any world, you can see a little display in the bottom right corner of your screen that shows the battery icon and percentage, with optional toggle of estimated time remaining until the system's battery completely depletes or charges, depending on the current battery state.
<img width="984" height="638" alt="Screenshot_20261128_142246" src="https://github.com/user-attachments/assets/0b52b984-99b6-4049-8883-a10590041f11" />

## Building from source

### Requirements
- Linux (the mod reads `/sys/class/power_supply`)
- A **Java 25 JDK**
- An internet connection for the first build

You don't need to install Gradle. The included wrapper (`./gradlew`) downloads the right version itself. If it says Java 25 is missing, set `JAVA_HOME` to your Java 25 JDK, for example `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`.

### Build
```sh
git clone https://github.com/xy0acer/linux-battery-display
cd linux-battery-display
./gradlew build
```

This builds for **Minecraft 26.2**. All dependency versions are set in `gradle.properties`. The JAR ends up in `build/libs/`, for example `batterydisplay-26.2-0.4.4.jar`. Don't use the `-sources.jar`, which only contains source code.

The first build downloads Minecraft and Fabric and takes a few minutes. Later builds are much faster.

### Other Minecraft versions
The same code builds for other versions. Pass the matching dependency versions on the command line:

| Minecraft | Command |
| --- | --- |
| 26.1 | `./gradlew clean build -Pminecraft_version=26.1 '-Pminecraft_dependency=~26.1' -Pfabric_api_version=0.145.1+26.1 -Pmodmenu_version=18.0.2 -Pmod_version=26.1-0.4.4` |
| 26.2 | `./gradlew clean build` |
| 26.3 | `./gradlew clean build -Pminecraft_version=26.3 '-Pminecraft_dependency=~26.3' -Pfabric_api_version=0.167.0+26.3 -Pmodmenu_version=21.0.1 -Pmod_version=26.3-0.4.4` |

Fabric API and Mod Menu versions belong to one Minecraft version, so never mix them. Keep `clean` in the command whenever you switch versions.

### Install
1. Install **Fabric Loader 0.20.1 or newer** for your Minecraft version.
2. Put the built JAR and the matching **[Fabric API](https://modrinth.com/mod/fabric-api)** into your instance's `mods` folder.
3. Optional: add **[Mod Menu](https://modrinth.com/mod/modmenu)** to change settings in-game. Without it, edit `config/batterydisplay.json`.

### Try it without installing
```sh
./gradlew runClient
```
This starts a separate development copy of Minecraft with the mod and Mod Menu already loaded. Its files are stored in `run/`.
