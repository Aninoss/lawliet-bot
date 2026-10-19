# Lawliet Discord bot

Single-module Gradle project (Java + Kotlin mixed under `src/main/java`, JDA 6). Entry point: `core.Main`. No tests, no CI, no linter/formatter config exist.

## Build
- `./gradlew build` / `./gradlew compileJava compileKotlin` (wrapper is Gradle 7.6.3; `sourceCompatibility = 15`). `java` is not on PATH in the sandbox, so compilation may not be runnable; don't assume it is.
- Docker image is built with Jib (`./gradlew jib*`); files under `src/main/jib/` are copied into the image (`/data/...` resources, fonts, emoji lists, `welcome_backgrounds`, `cdn`).
- The bot cannot start without a large set of env vars read via `System.getenv` (no defaults, NPE if missing): `PRODUCTION`, `PUBLIC`, `CLUSTER`, `ROOT_DIR` (base for `LocalFile`, contains `data/`), plus Redis/MySQL/Mongo, `WEBCACHE_*`, `RATELIMITER_*`, `SYNC_*`, `DISCORD_DOMAINS`, etc. Grep `System.getenv` before assuming something is configurable. Don't try to run the bot locally.
- `Program.productionMode()` / `publicInstance()` gate startup behavior (Patreon, heartbeat, version update, shard connect) and commands marked `onlyPublicVersion`.

## Layout (`src/main/java`)
- `commands/` - command framework. `commands/runnables/<x>category/` holds the commands; `*Abstract.java` are shared base classes.
- `commands/slashadapters/adapters` - slash-command adapters, discovered by Reflections via `@Slash`. Same for `events/discordevents` (`@DiscordEvent`) and `events/scheduleevents`: new classes are auto-registered by annotation, no manual list.
- `core/` utilities, shard/Discord connection, `TextManager` (i18n). `modules/` feature logic. `mysql/` DB access + caches; `mysql/hibernate/entity` are Hibernate (OGM/Mongo-backed) entities. `dashboard/` web dashboard backend. `mysql_schema.sql` at the root is the SQL schema.

## Adding/changing a command
1. Create class in `commands/runnables/<x>category/`, extend `Command`, annotate with `@CommandProperties(trigger=..., emoji=..., executableWithoutArgs=...)`, constructor `(Locale, String prefix)`.
2. **Manually register** it with `commandList.add(...)` in the static block of `commands/CommandContainer.java` (NOT auto-discovered, unlike slash adapters/events). Every registered command is instantiated once at class-load with `Locale.US` / prefix `"L."`, so constructors must not do real work.
3. The category is derived from the **package name** (`Category.findCategoryByCommand` uses `package.split(".")[2]`); a new category package needs a mapping in `Category.kt`.
4. Add translations in `src/main/resources/<category.id>_en_us.properties` **only** (English). Whenever new files/entries are added, do NOT add the other locales (`de_de, es_es, fr_fr, pt_br, ru_ru, tr_tr`) for now; they are added by the maintainer before the next release, so non-English files may lag behind `en_us`. Keys follow `<trigger>_title`, `_description`, `_helptext`, `_usage`, `_examples`, plus custom keys used via `getString("key")`. Note resource file prefix is `Category.id`, which is not always the package name (e.g. `fishery_category`, `invite_tracking_category`, `aitoys`, `external_services`).
5. Add the new command to the release notes (see below) if it is user-facing.

## Versions / change log
- `constants/Versions.java` lists releases; the last entry is the current version (`BotUtil.getCurrentVersion()`). Each version needs an entry in `src/main/resources/versions_<locale>.properties` (`2.74 = ...`). Text there uses `{PREFIX}` and `{category.key}` references to other bundles.

## Conventions
- Properties files are loaded as UTF-8 via `UTF8Control` (not the Java default ISO-8859-1); keep non-ASCII characters literal.
- Keep Java 15 compatible source (no records pattern matching/newer APIs); Kotlin plugin is 2.2.0 but declared stdlib deps are 1.6.0-RC2.
- `src/main/jib/temp`, `src/main/jib/data/cdn/{temp,emoji_cache}` and `src/main/jib/data/welcome_backgrounds/*` (except `placeholder.png`) are gitignored runtime dirs.
- Commit messages in history are short, imperative-ish English (e.g. "Reddit fix", "Translations").
