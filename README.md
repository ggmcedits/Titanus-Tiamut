# Tiamut – Sea Kaiju (Minecraft 1.20.1 / Forge 47.x + GeckoLib 4)

Adds **Tiamut**, a colossal, very fast sea kaiju inspired by the pre-evolution Monsterverse Tiamat:
a long, serpentine sea-dragon with a three-part neck, swept horn crest, heavy torso, four paddle flippers
and a long tail ending in a vertical fluke, with bioluminescent spines and glowing amber eyes.

## Features
* **Swimming animations** – hover/idle, cruise swim, **fast swim** (tail whips at ~0.85 s per cycle, neck stretched forward), and a "beached" flop on land.
* **Fast swimmer** – 600 HP, `MOVEMENT_SPEED` 1.6 with Dolphin-style smooth swimming; cruises ~10 b/s and chases ~17 b/s.
  Tune `SWIM_SPEED_ATTRIBUTE` / `CHASE_SPEED_MODIFIER` in `TiamutEntity`.
* **Three animated roars** (synchronised jaw/neck/flipper animation + custom sound):
  * `roar_short` – ambient roar every 45–135 s while idle
  * `roar_long` – rearing bellow, sometimes when she takes heavy damage
  * `roar_challenge` – bow, snap-up, double bellow + head shake, when she picks a target
* **Bite attack** with lunge animation, reach long enough for the head (~9 blocks).
* Neutral: only attacks when hurt (or Guardians). Boss bar while in combat. Persistent (never despawns).
* Spawns very rarely in deep oceans, or use the **Tiamut Spawn Egg** / `/summon tiamut:tiamut`.
* Debug command (OP): `/tiamut roar <short|long|challenge>` forces the nearest Tiamut to roar.
* Drops heart of the sea, nautilus shells, prismarine.

## Build / run
1. JDK 17.
2. This repo contains `build.gradle`, `gradle.properties`, `settings.gradle` but **not** the Gradle wrapper.
   Copy `gradlew`, `gradlew.bat` and the `gradle/` folder from the official Forge 1.20.1 MDK (47.2.0), or run `gradle wrapper` with Gradle 8.1.1.
3. `./gradlew runClient`  → test in dev.   `./gradlew build` → jar in `build/libs/`.
4. Players need **GeckoLib 4.4.x for 1.20.1** installed alongside the jar.
   If 4.4.9 isn't resolvable, change `geckolib_version` in `gradle.properties` to any 4.4.x release.

## Editing the art
`python3 tools/gen_model.py` regenerates `geo/tiamut.geo.json`, `tiamut.png`, `tiamut_glowmask.png` and
`animations/tiamut.animation.json`. `python3 tools/gen_sounds.py` regenerates the roars (needs numpy, scipy, ffmpeg).
You can also open the geo/animation files in **Blockbench** (GeckoLib plugin) to hand-sculpt, repaint over the
texture, or swap in a model closer to your reference images. Keep bone names the same and the Java side keeps working.
Replace the synthesized `.ogg` roars with real recordings (mono) – same file names.

## Bones
root > body > neck1 > neck2 > neck3 > head > jaw · body > tail1..tail4 > tail_fin ·
body > front_flipper_{l,r} > _tip · body > back_flipper_{l,r} > _tip

## Known caveats
* Written against GeckoLib 4.4.x APIs and Forge 47.2.0 but **not compiled or launched in the authoring environment** (no network access); expect to fix at most small API/typo issues on first build.
* Animation sign conventions follow Blockbench/Bedrock (+X pitches the front down). If a roar lowers the head instead of raising it in your build, flip the X signs in `tools/gen_model.py` and regenerate.
* Vanilla pathfinding isn't built for 20-block creatures; she may hesitate in tight caves. Open deep water works best.
