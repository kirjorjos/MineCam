# MineCam — Minecheck's fork of Freecam

MineCam is the Freecam fork the Minecheck harness uses for its **human-only** observation camera.
The requirements it is built against live in `.notes/freecam-spec.md`, with the harness side in
Minecheck's `two-view-capture-plan.md`.

- Base: [MinecraftFreecam/Freecam](https://github.com/MinecraftFreecam/Freecam) (MIT; the notice is
  kept in `THIRD_PARTY_LICENSES.md`), cut from its `1.18` branch — Minecraft 1.18.2,
  `mod_version=1.3.5`.
- Lives at [kirjorjos/MineCam](https://github.com/kirjorjos/MineCam), branch `1.18.2/development`: the
  `<version>/development` grammar the shared CI keys on. `upstream` is Freecam, `origin` is MineCam.
- Registered in `kirjorjos/ci` as a `minecraft-mods` consumer with the `issues` type only — the fork
  has no screenshot corpus, lifecycle suites or script tree for the other three to judge.
- The mod id, packages and language keys stay `freecam`. Renaming them would touch every mixin
  config, metadata template and translation for no behavioural gain, and would make upstream merges
  expensive. MineCam is the fork's name, `freecam` is its technical id.

## What this fork changes

| # | Change |
|---|---|
| F5 | Leaving a world/server tears down the camera entity but keeps freecam **active**; rejoining rebuilds it. Nothing is persisted to disk. |
| F6 | A world or dimension change rebuilds the camera at the **player's** position and rotation. |
| F7 | Tripods are gone: one camera, one key. `tripod/*`, the combo key mapping, the tripod notifications setting and their `en_us` keys are deleted. |
| F4 | `Allow Interaction` defaults to *on* standalone and is forced *off* while Minecheck is installed. |
| F8 | With Minecheck installed, `Show Hand`, `Full Brightness` and freecam notifications are forced off, on load and on save of the config. |
| F1/F2 | Physical input drives the camera. With Minecheck installed, the player's input is inert on any tick that saw no injected input, so physical input reaches nothing while freecam is inactive. |
| F9 | Minecheck's own marker (`net.minecheck.platform.api.InjectedInput`) is read reflectively, so virtual input is recognised without a dependency; a mapping a dispatch held down stays the player's until it is released. |
| F10 | No Minecheck dependency and no Minecheck classes are referenced — presence is a loader query (`fabric`/`forge` `ModList`), so every change above is inert without it. |

Nothing outside this list changed. With Minecheck absent the fork behaves as upstream, except for
F5/F6/F7, which are fork-wide and intended.

## How the two mods find each other

Optional in both directions: neither mod requires the other, and each works alone (F10).

The fork declares Minecheck as optional — `recommends` in `fabric.mod.json`, a `mandatory = false`
dependency in `mods.toml`. The mirror image belongs in Minecheck's metadata and is **not** part of
this fork: Minecheck needs `"recommends": {"freecam": "*"}` and a `mandatory = false` entry for
`freecam`.

Presence is a loader query (`Platform#isModLoaded("minecheck")`); the dispatch window is read from
Minecheck's marker, `net.minecheck.platform.api.InjectedInput#active()`, by reflection — never
compiled against, so this fork does not need to be built after Minecheck and does not need it at
runtime. Minecheck needs no code change: the marker it already raises is the one the fork consults.

## The API Minecheck may use

`net.xolt.freecam.api.FreecamApi` only:

| Method | Meaning |
|---|---|
| `isActive()` | The human's camera is on. |
| `pose()` | Current camera position/rotation, read-only, `null` while inactive. |
| `setInjectedInput(boolean)` | Raise/lower the fork's own marker, for input that does not pass through Minecheck's `InjectedInput` (screens, widgets). |
| `isInjectingInput()` | Whether a dispatch is in progress right now. |
| `requestPlayerViewCapture()` | Say that a player-view capture is pending, so the fork can yield (D19). |
| `consumePlayerViewCaptureRequest()` | Read and clear that flag. |

The fork never toggles, poses or renders for anyone else: it only carries the flag. The frame yield
itself is a no-op in the fork today, because the player-view render belongs to Minecheck.

### Injected-input contract

Minecheck raises `InjectedInput` around each synthetic key/button dispatch; the fork reads that same
marker, and needs nothing from Minecheck's side.

The marker is only up for the duration of one call, and a client tick runs after it is cleared — so
the fork latches at the seam those dispatches pass through (`KeyMapping#setDown`) rather than
sampling the tick:

- a mapping that goes down inside the window is remembered as virtual and stays the player's until it
  is released, so a held script key never flies the camera;
- that mapping also hands the player's input back to vanilla (`KeyboardInput`) for the tick, and
  freezes the camera's input;
- with no dispatch in flight, physical input drives the camera as upstream.

Mouse look is not part of this today: Minecheck does not dispatch mouse movement — `setLook`/`lookAt`
write the player's rotation directly — so `MouseHandlerMixin`'s hand-off is a safety net that never
fires against the current harness. It covers a dispatch that does go through `MouseHandler#onMove`.

What this does **not** catch: a virtual press of the freecam toggle key itself (it is ticked by
`Minecraft`, outside any dispatch), and input dispatched straight at a screen or widget, which
Minecheck deliberately does not mark. Listed as open seams in the spec's §12.

## Building

```sh
./gradlew build            # fabric + forge, normal + modrinth variants
./gradlew :fabric:runClient
```

1.18.2 only on this branch, Java 17. `./gradlew build` writes the loadable jars to
`fabric/build/libs` and `forge/build/libs`; `.github/workflows/build.yaml` uploads from there.

## Licensing

MineCam is **GPLv3** (`LICENSE`) where Freecam is MIT. That is deliberate and it is permitted: MIT
grants sublicensing, so the combined work can ship under the GPLv3.

The one obligation it does not remove is MIT's — the copyright and permission notice has to travel
with every copy. It is therefore reproduced verbatim in `THIRD_PARTY_LICENSES.md`, not deleted, and
that file states which portions it covers. The GPLv3 adds the usual source-availability obligation
on anyone distributing a binary; this repository is the source.

`gradle.properties` declares `licence=GPL-3.0-only`, so the value the loaders display and
`fabric.mod.json` / `mods.toml` interpolate matches the licence file.

Upstream's MIT text names no copyright holder — `Copyright (c) 2021` and then nothing. That is left
as it was: filling in a name would attribute copyright to someone who does not hold it. The fork's
own changes are copyright kirjorjos.
