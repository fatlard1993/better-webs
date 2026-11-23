# Better Webs - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients
need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API)
and `fabric.mod.json` (Java).

## Key Files

| File | Responsibility |
|------|---------------|
| `Main.java` | Entry point; ships the strand models and their textures |
| `Webs.java` | What holds a web, the same rule Pandorical's strands draw by |
| `WebThrowGoal.java` | The rear and throw at something out of reach |
| `ThrownWeb.java` | The ball of web: its flight and trail, where it lands, what it leaves |
| `Silk.java` | What silk looks like on its way: strands, puffs, the spinnerets they come from, and the spider's animations |
| `WebSpinGoal.java` | An idle spider spinning a web somewhere dark |
| `SpiderSizes.java` | Every spider a size the first time it is loaded: smaller and varied, and now and then a big slow one |
| `mixin/SpiderMixin.java` | Gives spiders, and so cave spiders, the two goals |
| `mixin/SnowballMixin.java` | A snowball marked as a web lands as a web |
| `mixin/WebBlockMixin.java` | A cobweb's scheduled tick is when a thrown one comes apart |
| `generate_assets.py` | Every texture, model and animation under `src/main/resources/assets`, and the icon |

## Strung webs

The drawing is Pandorical's `Strands` context model, which picks pieces by neighbour as the chunk
is built. This mod only ships the pieces, named after the block they are for, which for the cobweb
means `assets/minecraft/models/block/cobweb_strand_*`. Pandorical's DEVELOPMENT.md has the naming.

## Thrown webs come apart

Vanilla never schedules a tick on a cobweb, so a scheduled tick marks the thrown ones: their tick
is when they go. It is saved with the chunk, so a web thrown just before an unload or a restart
still comes apart. A web someone places, or one a spider spins, is never ticked and stays.

## Art

`generate_assets.py` draws every texture from scratch (none are cut out of the vanilla jar) and
writes the models beside them. A sheet's texture is drawn for the exact sides holding it, five
variants of each from a fixed seed, so rerunning it changes nothing unless the script changed. It clears `assets/minecraft` and this mod's textures before writing,
so anything hand-placed there is lost: put it in the script. Re-run it after changing it.

## Tests

`xvfb-run -a ./gradlew runClientGameTest -Ptest.level=full` runs `ThrownWebs` (a thrown web lands
by its target, comes apart on time without string, and with mob griefing off leaves no block and
slows instead), `SpidersComeInSizes` (four hundred of each kind rolled: ranges, averages, how often a
big one, and what being big does), `Showcase`, which webs a stone room and a dark cave and catches
spiders throwing and spinning, and `Mineshaft`, which finds a real mineshaft in a world of its own
with a fixed seed and photographs its webs. The pictures land in
`build/run/clientGameTest/screenshots`. The spinning goal is chance-driven and has no test.
