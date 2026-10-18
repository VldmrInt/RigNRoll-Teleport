# Rig'n'Roll Teleport

[Русская инструкция](README.md)

Clone the repository into the `Tools/Teleport` folder inside the game installation:

```powershell
git clone https://github.com/VldmrInt/RigNRoll-Teleport.git Tools/Teleport
Copy-Item Tools/Teleport/game-root.example.txt Tools/Teleport/game-root.txt
```

Set the game path in `game-root.txt`, then run `Tools/Teleport/Teleport.bat`.

Double-click `Teleport.bat` to start. Close the game first if it is already running. The agent loads at game startup; the launcher preserves any existing `JAVA_TOOL_OPTIONS` settings.

1. Load a save or start a game.
2. Press Alt+Tab to open the “Rig'n'Roll — Teleport” window.
3. Click “Refresh locations”, then switch back to the game for a few seconds. The game may pause map access while minimized. Return to the tool, search by name or type (for example, “Warehouse”), select a location, and click “Teleport”.
4. Return to the game and unpause to let the engine finish. The tool reports the result.

Choose “Русский” or “English” in the language selector beside the search box. It translates the tool, location types, built-in descriptions, search, and errors. Detailed location text comes from the game's installed localization. When English is selected, Russian game text is replaced with an English notice and a general description based on the location type.

Each entry shows the game's localized short name when available and its stable map ID. The description pane displays the detailed game text, location type, ID, and coordinates. Search checks the title, description, and ID. If no detailed description exists, the tool says so and explains the known purpose of the location type; it does not guess the purpose of unknown locations.

Locations come from the current world's `MissionsMap`: warehouses, bars, offices, gas stations, repair shops, and scenario locations registered by the game. The binary `GAME.CNF` is not parsed directly, so only locations registered in the current world are listed.

Teleport uses the game's `actorveh.teleport` API under the engine lock, stops the truck first, waits for the engine callback, then checks the truck's actual coordinates. If the game does not confirm completion, the tool does not retry automatically. Closing the window minimizes it; the agent stays active until the game exits. Refresh the list after loading a different save.

Copy `game-root.example.txt` to `game-root.txt` and set the path to your game installation. `Teleport.jar` is already included; rebuilding requires JDK 21 through `JAVA_HOME` or `PATH`. Build with `powershell -ExecutionPolicy Bypass -File Tools/Teleport/build.ps1`. Run the self-test with `Tools/Teleport/test.ps1`. The bundled runtime is Java 5.

The log is `Tools/Teleport/teleport.log` when the tool is inside the game directory. If the tool is elsewhere, the log is created in the game's working directory (`MonsterCup`). For installations under `Program Files`, Windows may redirect it to `%LOCALAPPDATA%\VirtualStore\Program Files (x86)\1C\RigNRoll\MonsterCup\teleport.log`.

### Verification

Build and self-test passed on the bundled JVM. In the primary installation, the GUI listed 716 locations and confirmed teleport to warehouse `MP_AL` near Alturas within 0.17 m. In the second installation at `C:\Program Files (x86)\1C\RigNRoll`, the agent also loaded and listed 716 locations, 715 with detailed descriptions. Teleport to `MP_CF` in the second installation was confirmed within 0.99 m. The user confirmed that the trailer teleports too. No save was modified. Not every location was tested.
