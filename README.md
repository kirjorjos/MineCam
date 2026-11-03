# MineCam

MineCam is the [Freecam](https://github.com/MinecraftFreecam/Freecam) fork that the
[Minecheck](https://github.com/kirjorjos/Minecheck) harness uses as its **human-only observation
camera**. It is a component of that harness, not a mod players install: it is built, tested and
shipped alongside Minecheck, and Minecheck works without it.

If you arrived here looking for Freecam itself — the standalone mod that lets a player fly a camera
while their body stays put — go to [MinecraftFreecam/Freecam](https://github.com/MinecraftFreecam/Freecam).
Its documentation describes the mod this one is derived from, and not all of it is true here.

## What is different

Every change from upstream is listed in [FORK.md](./FORK.md), with the reason for it, along with how
the two mods find each other and the API Minecheck consumes. Nothing outside that list changed.

## Licence

MineCam is distributed under the **GNU General Public License, version 3** — see [LICENSE](./LICENSE).
That covers the combined work, including the portions taken from Freecam.

Freecam is MIT-licensed, and MIT requires its notice to travel with every copy, so it is reproduced
verbatim in [THIRD_PARTY_LICENSES.md](./THIRD_PARTY_LICENSES.md) rather than dropped. MIT permits
sublicensing, which is how this fork can ship as a whole under the GPLv3 without the upstream notice
disappearing.

## Building

```sh
./gradlew build            # fabric + forge, normal and modrinth variants
./gradlew :fabric:runClient
```

Minecraft 1.18.2 only on this branch, Java 17. The loadable jars land in `fabric/build/libs` and
`forge/build/libs`; `.github/workflows/build.yaml` uploads from there.
