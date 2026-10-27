# SporeAPI

SporeAPI is a Kotlin Minecraft API that you can use in your projects. It includes features such as menus, messages, a logger, boss bars, commands, cooldowns, and much more!

![License](https://img.shields.io/github/license/ClearedSpore/SporeAPI)
![Latest release](https://img.shields.io/github/v/release/ClearedSpore/SporeAPI)
![Kotlin](https://img.shields.io/badge/kotlin-2.4.0-blueviolet?logo=kotlin)
![PaperMC](https://img.shields.io/badge/papermc-1.21+-blue?logo=spigotmc)
![Author](https://img.shields.io/badge/author-ClearedSpore-brightgreen)

# Features

- [Advanced Menu system](#menu-system)
- [Message utility](#messages)
- [Logger utility](#logger)
- [Boss bars](#boss-bars)
- [Chat input](#chat-input)
- [Dialogs](#dialogs)
- [Commands and listeners](#commands-and-listeners)
- [Events](#events) and an [event bus](#event-bus-your-own-events)
- [Coroutines](#coroutines), [repeat and delay](#repeating-and-delayed-work), [rate limiting](#rate-limiting)
- [Scoreboards](#scoreboards) and [tab lists](#tab-list)
- [Registries](#registries)
- [Repositories](#repositories)
- [Redis packets](#redis-packets)
- [Cooldowns](#cooldowns)
- [Confirmations](#confirmations)
- [Tasks and scheduling](#tasks-and-scheduling)
- [Discord webhooks](#discord-webhooks)
- [Item builder](#item-builder)
- [Timings](#timings)
- [Debugging](#debugging)
- [Velocity](#velocity)
- Serialization
- And much more!

> ⚠️ This is not a plugin, it's an API/library that your plugins can depend on to reduce boilerplate.

---

# Installation

You can include **SporeAPI** in your project either via **Gradle** or **Maven**.

Replace the version with the latest API version.

![Latest Version](https://img.shields.io/github/v/release/ClearedSpore/SporeAPI)
 
You can find the repository and dependency [here](https://repo.sporedev.eu/#/releases/eu/sporedev)

## Modules

SporeAPI is split into modules. Pick the one for your platform. Each one brings everything it needs with it, so you never have to add ACF, Cloud, InvUI, Caffeine, Mongo etc. yourself.

| Module                          | Use it for                    | Contains                                                                                                                                                           |
|---------------------------------|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `eu.sporedev:sporeapi-paper`    | Paper / Folia plugins         | Everything Minecraft related **and** all of `common`                                                                                                               |
| `eu.sporedev:sporeapi-velocity` | Velocity proxy plugins        | `SporeVelocityPlugin`, Cloud commands, listener scanning, `/debug` **and** all of `common`                                                                          |
| `eu.sporedev:sporeapi-common`   | Discord bots, any JVM app     | Logger, colors (`CC`), coroutines, `repeat`/`delay`, event bus, timings, rate limiter, debug/incidents, registries, repositories, serialization, cooldowns, webhooks |
| `eu.sporedev:sporeapi-extra`    | Add-on next to any of these   | Heavier systems: Redis packets (`SporeRedis`, `Packet`) and Mongo (`SporeMongo`, `MongoRepository`)                                                               |
| `eu.sporedev:sporeapi-acf`      | Older plugins that use ACF    | ACF support (`SporeAcfPlugin`, `SporeCommand`) **and** all of `paper`                                                                                              |

```kotlin
repositories {
    maven("https://repo.sporedev.eu/releases")
}

dependencies {
    implementation("eu.sporedev:sporeapi-paper:VERSION")
    // implementation("eu.sporedev:sporeapi-extra:VERSION") // only if you need Mongo etc.
}
```

You only ever need **one** of `common`, `paper`, `velocity` or `acf`: `acf` includes `paper`, and `paper` and `velocity` include `common`. Shade it into your plugin (and relocate it) as usual.

`common`, `velocity` and `extra` are built for **Java 21**, `paper` and `acf` for **Java 25** (what Paper 26 needs).

## Velocity

Extend `SporeVelocityPlugin` and pass the injected values through:

```kotlin
@Plugin(id = "atlas", name = "Atlas", version = "1.0")
class AtlasVelocity @Inject constructor(
    server: ProxyServer,
    logger: Logger,
    @DataDirectory dataDirectory: Path
) : SporeVelocityPlugin(server, logger, dataDirectory) {

    override fun onPluginEnable() {
        SporeDebugCommand.register(commandManager, "atlasv", "atlas.admin")
    }

    override fun setupCloud(manager: SporeVelocityCommandManager) {
        // suggestion providers, parsers...
    }
}
```

On proxy start SporeAPI sets itself up and registers every `@RegisterCommand` class (Cloud annotations) and every `@RegisterListener` class (any class with `@Subscribe` methods). Command methods can take a `Player` directly. On proxy shutdown everything is cleaned up, including running coroutines.

`SporeVelocity.server` / `SporeVelocity.plugin` give you the proxy from anywhere, and `CommandSource.success()` / `error()` work like on Paper. Everything from `common` (coroutines, `repeat`, event bus, timings, rate limiter, incidents) works the same on the proxy.

## Not on Paper or Velocity?

You tell SporeAPI where it is running once at startup:

```kotlin
SporeCore.init(StandalonePlatform("MyBot", MyBot::class.java))
Logger.initialize("MyBot")
```

Override `StandalonePlatform.log` to forward console lines to your own logger, and call `SporeCoroutines.useMainDispatcher(...)` if your platform has a main thread.

---

# Getting started

Most of the features below (commands, listeners, item builder, tasks, boss bars, action bar, coroutines, sidebars, serialization) set themselves up automatically, but only if your main class extends `SporePlugin` instead of `JavaPlugin`.

`SporePlugin` owns `onLoad`, `onEnable` and `onDisable` itself - that's where all the setup happens - so you override `onPluginLoad`, `onPluginEnable` and `onPluginDisable` instead. There's no `super` call to remember.

```kotlin
class TestingPlugin : SporePlugin() {

    override fun onPluginEnable() {
        Logger.initialize("Your plugin name")
    }

    override fun onPluginDisable() {
        // your own cleanup, the API cleans up after itself
    }
}
```

If you only want the simple stuff like `Message` and `Logger`, extending `JavaPlugin` still works fine.

---

# Messages

## Colors

You can easily translate color codes by adding `.translate()` to a string.
It supports `&` color codes, `&#RRGGBB` hex color codes, and MiniMessage tags.

There are also a few pre-made color methods you can use such as `.blue()`, `.white()`, `.red()`, `.green()`, `.gold()`, and more.

## Legacy text to MiniMessage

Text that comes from other plugins - LuckPerms prefixes, nicknames, config values - is often written with `&` color codes (including `&#RRGGBB` hex). If you drop that straight into a `.mm()` string, the codes show up as literal text. Convert it first with `.legacyToMiniMessage()`:

```kotlin
val prefix = luckPermsPrefix.legacyToMiniMessage()   // "&c[Admin] " -> "<red>[Admin] "
player.sendMessage("$prefix<white>${player.name}".mm())
```

## Message utility

There are many utility methods for sending messages.

You can call the `Message` class to access them. Some methods are not shown directly because they are extensions of the `Player` class.

If you want your success and error messages to start with your plugin name, call `Message.init(true)` once when your plugin starts.

- `sendBossBar` – Send a simple bossbar:
  ```kotlin
  player.sendBossBar(text, progress)
  ```
  This shows a blue bossbar. If you want more control (color, style, permissions, auto expiry), check out [Boss bars](#boss-bars).

- `endTimedBossBar` – Send a bossbar that fills up and disappears on its own:
  ```kotlin
  player.endTimedBossBar(plugin, title, progress, duration)
  ```

- `sendSuccessMessage` – Send a success message with sound:
  ```kotlin
  player.sendSuccessMessage("Success!")
  // Plays ENTITY_EXPERIENCE_ORB_PICKUP
  ```

- `sendErrorMessage` – Send an error message with sound:
  ```kotlin
  player.sendErrorMessage("Error!")
  // Plays ENTITY_VILLAGER_NO
  ```

For actionbars, either use Paper's own `player.sendActionBar(message)`, or use the [Action bar](#action-bar) helper below if you want to show more than one message at once without them overwriting each other.

---

# Logger

The API also includes an advanced logger for in-game and console logging.

## Setup

First, you need to set up the logger in your main class by initializing it in the `onEnable` method.

```kotlin
class TestingPlugin : JavaPlugin() {

    override fun onEnable() {
        Logger.initialize("Your plugin name")
    }

    override fun onDisable() {
    }
}
```

## In-game logging

If you want to send an in-game log message, you can call the `Logger.log` method.
This will send a log to all players with a specific permission.

```kotlin
log(playerSuffix, sender, permission, message)
```

The `playerSuffix` is just some extra text added after the player's name, useful for things like showing what a player is looking at or clicked on. You can leave it blank if you don't need it.

## Console logging

There are 6 methods you can call for console logging.
The basic ones are `info`, `error`, and `warn`.

These will send a colored message (if your console supports it) with the information you provide:

```kotlin
info(message)
error(message)
warn(message)
```

Example output:
```
[TestingPlugin] (info) Loading Testing plugin
```

If you use a database, you can also use the database logger methods.
These work the same, except the plugin name includes "Database":

```
[TestingPlugin Database] (info) Connected to H2 database.
```

Methods:
```kotlin
infoDB(message)
errorDB(message)
warnDB(message)
```

You can also send a quick message straight to a Discord webhook with `Logger.log(webhookURL, message)`. If you need something fancier like embeds, check out [Discord webhooks](#discord-webhooks).

---

# Menu system

Here I will tell you about the basics on how to make a normal and paginated menu.

## Normal Menu

In the menu below you can see I provided the menu name, rows and items.

You **have** to provide the instance of your plugin in order to register the listeners.

Of course every menu needs a name. I added a simple name but you can add color codes (make sure to do `.translate()` or `.blue()`).

For the menu size we don't use slots but rows. Every menu can have 6 rows (1 double chest).

To set items in the menu you can make a new Kotlin class and add the item stack and click.

When adding new items to the menu you will do `setMenuItem(x, y, item instance)`. The x and y are the coordinates for where it puts the item. So if you want it in slot 19 it would be `x = 2` and `y = 3`.

In the item class you can see I added the item stack and meta. You **have** to return the item stack and set the item meta.

For the inventory click it will only call for that item. Meaning that you don't have to add all the clicks for all the items in 1 method.
```kotlin
class TestingMenu() : Menu(TestingPlugin.instance)  {

    override fun getMenuName(): String {
        return "Menu | Testing menu"
    }

    override fun getRows(): Int {
        return 3
    }

    override fun setMenuItems() {
        setMenuItem(2, 2, FirstItem())
    }

}
```

```kotlin
class FirstItem() : Item() {

    override fun createItem(): ItemStack {
        val item = ItemStack(Material.STONE)
        val meta = item.itemMeta
        meta?.setDisplayName("Stone".blue())
        item.itemMeta = meta
        return item
    }


    override fun onClickEvent(clicker: Player, clickType: ClickType) {
        clicker.sendMessage("You have clicked stone!")
    }
}
```

If you want the empty slots to be filled with gray glass automatically, just override `fillEmptySlots()` and return `true`.

## Paginated menu

There are 2 ways to make a paginated menu.

### Using an item stack

You can add items using an item stack and then in the click event checking the persistentDataContainer.

This is not recomended but it does work.
By using the `addItem(item)` method it will add the item to the next slot. The API automaticly finds the nxet available slot and adds the item.

```kotlin
class TestingPaginatedMenu() : BasePaginatedMenu(TestingPlugin.instance) {

    override fun getMenuName(): String {
        return "Menu | Paginated menu"
    }

    override fun getRows(): Int {
        return 6
    }

    override fun createItems() {
       for (player in Bukkit.getOnlinePlayers()) {
            val item = ItemStack(Material.PLAYER_HEAD)
            val meta = item.itemMeta as SkullMeta
            meta.setDisplayName("Player: ${player.name}".blue())
            meta.owningPlayer = Bukkit.getOfflinePlayer(player.uniqueId)
            item.itemMeta = meta

            val key = NamespacedKey(TestingPlugin.instance, "player_name")
            meta.persistentDataContainer.set(key, PersistentDataType.STRING, player.name)
            item.itemMeta = meta
            addItem(item)
        }
    }

    override fun onInventoryClickEvent(
        clicker: Player,
        clickType: ClickType,
        event: InventoryClickEvent
    ) {
        val item = event.currentItem ?: return

        if (item.type == Material.PLAYER_HEAD) {
            val meta = item.itemMeta
            val key = NamespacedKey(TestingPlugin.instance, "player_name")
            val clickedPlayerName = meta?.persistentDataContainer?.get(key, PersistentDataType.STRING)
            if (clickedPlayerName != null) {
                clicker.sendMessage("You clicked the head of: $clickedPlayerName")
            }
        }
    }
}
```

### using the `Item` class

You can use the `Item` class to make an item and set the inventory click in the class. That way you don't have to add any persistentDataContainer and you can just add the items.
You add the necessary arguments in the `item` instance and then when you click on the item it will get the inventory click and handle it.

```kotlin
class TestingPaginatedMenu() : BasePaginatedMenu(TestingPlugin.instance) {

    override fun getMenuName(): String {
        return "Menu | Paginated menu"
    }

    override fun getRows(): Int {
        return 6
    }

    override fun createItems() {
        for (player in Bukkit.getOnlinePlayers()) {
            addItem(PlayerHeadItem(player))
        }
    }

    override fun onInventoryClickEvent(
        clicker: Player,
        clickType: ClickType,
        event: InventoryClickEvent
    ) {}
}
```

```kotlin
class PlayerHeadItem(var player: Player) : Item() {

    override fun createItem(): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta
        meta.setDisplayName("Player: ${player.name}".blue())
        meta.owningPlayer = Bukkit.getOfflinePlayer(player.uniqueId)
        item.itemMeta = meta
        return item
    }


    override fun onClickEvent(clicker: Player, clickType: ClickType) {
        clicker.sendMessage("You clicked the head of: ${player.name}")
    }
}
```

If you don't want to make a whole new class just for a simple item, you can use `BuilderItem` instead. It lets you build the item and handle the click in one place:

```kotlin
addItem(BuilderItem(
    { ItemBuilder(Material.DIAMOND).setName("Click me".blue()).build() },
    { clicker, clickType -> clicker.sendMessage("Clicked!") }
))
```

## Footer Paginated menu

If you want to use footerpaginated menus the only thing you have to add is the `true` boolean in the implemention arguments.
```kotlin
class TestingPaginatedMenu() : BasePaginatedMenu(TestingPlugin.instance, true) {
```

## Extra features

### Search item

**THIS ONLY WORKS FOR A PAGINATED MENU!**

If you want to add a search feature to your menu you can do that only by typing **1** line!

You only have to call the `addSearchItem(x, y)` method and then it will add a pre-made search item.

When you click on the item it will close the menu and ask you to type your search in chat. Once you have typed your input the menu will re-open and apply the search. This uses [Chat input](#chat-input) under the hood, so there is nothing else you need to set up.

```kotlin
override fun createItems() {
    for (player in Bukkit.getOnlinePlayers()) {
        addItem(PlayerHeadItem(player))
    }

    addSearchItem(5, 6)
}
```

### Enable clicks

Normally you always want to cancel clicks. For some menus you may want to enable inventory clicks or menu clicks. This can simply be done by overiding 2 methods.

**You are able to call these methods in a paginated menu but it is NOT recommended**

Enable inventory clicks:
```kotlin
override fun useInventory(): Boolean {
    return true
}
```

Enable menu clicks:
```kotlin
override fun cancelClicks(): Boolean {
    return false
}
```

### Click sound

By default it will play the `UI_BUTTON_CLICK` sound when you click an item but you can change that.

You can do that by overiding the `clickSound` method.

```kotlin
override fun clickSound(): Sound = Sound.ENTITY_ENDER_DRAGON_GROWL
```

---

# Boss bars

If you want more control over a bossbar than the simple `sendBossBar` shown above, like a custom color, a permission check, or auto expiring, you can build one with `BossBarBuilder`.

```kotlin
val bar = BossBarBuilder()
    .text("Boss fight!".red())
    .color(BarColor.RED)
    .style(BarStyle.SEGMENTED_10)
    .permission("boss.see")
    .durationTicks(200)
    .build()

BossBarManager.add(bar)
```

The bar will automatically show for players with the permission, update itself, and get removed once it's done. You can also remove it early with `BossBarManager.remove(bar.id)`.

If a bar should only be seen by certain players, use `viewer(player)` for a single player or `viewerCondition { player -> ... }` for your own check. You can change a bar while it's showing with `updateText`, `updateProgress` and `updateColor` (or just set `bar.color` / `bar.style`).

```kotlin
val bar = BossBarBuilder()
    .text("<green>Only you can see this".mm())
    .viewer(player)
    .build()

BossBarManager.add(bar)
bar.updateText("<red>Now it's red".mm())
bar.updateColor(BarColor.RED)
```

Text can be a MiniMessage `Component` (via `.mm()`) or a legacy string (via `.translate()`).

---

# Chat input

Sometimes you want a player to type something in chat and have your plugin catch it, instead of using a command or a sign. `ChatInputService` handles this for you.

```kotlin
ChatInputService.begin(player) { input ->
    player.sendMessage("You typed: $input")
}
```

The player will get a small message asking them to type something. If you don't want that message, pass `silent = true`. You can also cancel it early with `ChatInputService.cancel(player)`.

---

# Dialogs

Paper's dialogs are the pop-up screens with text, inputs and buttons. They're great for forms, confirmations and info screens, but the raw API is a pile of builders. SporeAPI wraps it so a dialog is just a few lines:

```kotlin
player.openDialog {
    title("Rename your pet")
    message("Pick a new name for <white>${pet.name}</white>.")

    val name = textField("Name") {
        initial = pet.name
        maxLength = 16
    }
    val glowing = checkbox("Glowing")

    button("Save") { click ->
        pet.rename(click[name])
        pet.isGlowing = click[glowing]
    }
}
```

All text is MiniMessage, with the same tags as `.mm()`. Every input gives you back a handle, so `click[name]` is a `String` and `click[glowing]` is a `Boolean` - no keys to keep track of.

## Reusable dialogs

For a dialog you open from more than one place, make it a class or an object. `build` runs every time it opens, so the content can depend on the player:

```kotlin
object SettingsDialog : SporeDialog() {

    override fun DialogBuilder.build(player: Player) {
        val settings = Settings.of(player)

        title("Settings")
        val volume = slider("Volume", 0..100) { initial = settings.volume }
        val mode = choice("Mode") {
            option("easy", "<green>Easy", selected = settings.mode == "easy")
            option("hard", "<red>Hard", selected = settings.mode == "hard")
        }

        button("Save") { click ->
            settings.volume = click[volume]
            settings.mode = click[mode]
        }
    }

    override fun onClose(player: Player, reason: DialogCloseReason) {
        // saved, cancelled, replaced, left...
    }
}

SettingsDialog.open(player)
```

`val shop = dialog { player -> ... }` does the same without a class.

## Inputs

| Input                            | Gives you              | Options                                                                     |
|----------------------------------|------------------------|-----------------------------------------------------------------------------|
| `textField("Name")`              | `String`               | `initial`, `maxLength`, `width`, `showLabel`, `multiline(maxLines, height)` |
| `checkbox("Glowing")`            | `Boolean`              | `initial`                                                                   |
| `slider("Volume", 0..100)`       | `Int`                  | `initial`, `step`, `width`, `format`                                        |
| `slider("Speed", 0.5f..2f)`      | `Float`                | the same                                                                    |
| `choice("Mode") { option(...) }` | the chosen option's id | `width`, `showLabel`                                                        |

There's also `message("...")` for text and `item(itemStack, description = "...")` for an item in the body.

## Buttons

- `button("Save") { click -> }` runs your code on the server.
- `commandButton("Spawn", "/spawn")` runs a command as the player.
- `linkButton("Website", "https://...")` opens a link. The server isn't told about this one.
- `exitButton("Cancel") { }` is the footer button, and it's also what Escape does. Dialogs with buttons get a "Close" one by default; `noExitButton()` removes it, but then closing with Escape can't be noticed.
- `confirm(yes = "Delete", no = "Keep") { click -> }` makes a yes/no dialog instead of a button list.
- A dialog without any buttons is a notice with a single OK button. Change it with `okButton("Got it") { }`.

`columns` sets how many buttons fit on a row (2 by default).

## Closing

A dialog closes when a button is clicked. Inside a handler you can also send the player somewhere else:

```kotlin
button("Save") { click ->
    if (click[name].isBlank()) {
        click.player.sendMessage("You need a name!")
        click.reopen()
        return@button
    }

    click.open(ConfirmRenameDialog)
}
```

Set `afterClick = AfterClick.KEEP_OPEN` to leave it open after a click, or `AfterClick.WAIT_FOR_RESPONSE` to show a waiting screen while you check something. It closes on its own if your handler doesn't open or reopen anything.

From anywhere else, use `SporeDialogs.close(player)`, `SporeDialogs.isOpen(player)` and `SporeDialogs.current(player)`.

`onClose { reason -> }`, or overriding `onClose` in a class, tells you why it closed: `BUTTON`, `EXIT` (the exit button, "no", or Escape), `CLOSED`, `REPLACED` or `QUIT`. Open dialogs are closed when your plugin disables, and errors in your handlers show up as [incidents](#incidents).

## Loading dialogs

Some dialogs need data first - a player's profile from the database, a page of punishments. `openLoaded` shows a loading dialog straight away, runs your loader in a coroutine, and swaps in the real dialog once it's done:

```kotlin
openLoaded(viewer, "<s_blue>$name", "<gray>Loading <white>$name</white>...", "profile.open", back = { LookupDialog() }) {
    val user = withAsyncCtx { UserRepository.findBlocking(uuid.toString()) }
        ?: return@openLoaded noticeDialog("<s_blue>$name", "<s_red>Couldn't load $name's data.") { LookupDialog() }

    ProfileDialog(user)
}
```

- The loader starts on the main thread, so do your database work in `withAsyncCtx` (or through a `SuspendCache`).
- If the player clicks **Cancel**, closes the dialog, opens something else or leaves before it finishes, the result is thrown away instead of popping up over whatever they're doing now.
- If the loader throws, it becomes an [incident](#incidents) under `operation`, and the player gets a notice with the ref.
- `back` is where Cancel and the error notice take the player. Leave it out and they just close the dialog.

`noticeDialog(title, text) { back }` is the small "message + one button" dialog it uses for errors. It's handy on its own for "No access" or "Not found" screens.

---

# Commands and listeners

SporeAPI can register your commands and listeners for you automatically, so you don't have to do it by hand in `onEnable`.

For this to work, your main class needs to extend `SporePlugin` (see [Getting started](#getting-started)).

## Commands

Put `@RegisterCommand` on a command class and SporeAPI finds and registers it. It looks at the class and sends it to the right framework, so you don't pick:

- a class with [Cloud](https://cloud.incendo.org/) annotations goes to Cloud (built into `sporeapi-paper`)
- a class extending `SporeCommand` goes to ACF (needs `sporeapi-acf`, see [ACF commands](#acf-commands))

The same routing applies to `SporeModule.getCommands()` and `registerCommand(command)` in your main class.

## Cloud commands

```kotlin
@RegisterCommand
class HealCommand {

    @Command("heal [target]")
    @Permission("myplugin.heal")
    fun heal(sender: Player, @Argument("target") target: Player?) {
        (target ?: sender).health = 20.0
    }
}
```

`Player` is injected for you - a console sender gets "Only players can run this command." instead of a crash.

If you need to configure the manager (custom parsers, Brigadier settings, suggestions), override `setupCloud` in your main class, and use the `cloudCommandManager` property when something wants the manager itself.

```kotlin
override fun setupCloud(manager: SporeCloudCommandManager) {
    CommandSuggestions.register(manager.manager)
}
```

## ACF commands

ACF (Aikar's Command Framework) lives in its own module for older projects. Use `eu.sporedev:sporeapi-acf` instead of `sporeapi-paper` (it includes paper), and extend `SporeAcfPlugin` instead of `SporePlugin`:

```kotlin
class MyPlugin : SporeAcfPlugin() {

    override fun setupACF(manager: PaperCommandManager) {
        super.setupACF(manager) // keeps the SporeAPI styled help/error messages
        // your contexts and completions
    }
}

@RegisterCommand
class TestCommand : SporeCommand() {

    @CommandAlias("test")
    fun onTest(sender: CommandSender) {
        sender.sendMessage("It works!")
    }
}
```

Cloud commands keep working next to ACF ones in the same plugin. The ACF manager is available as `commandManager`.

> ⚠️ ACF needs parameter names kept when compiling: `kotlin { compilerOptions { javaParameters = true } }`.

## Listeners

Same idea, but for a normal Bukkit `Listener`. Just add `@RegisterListener` and it gets registered automatically.

```kotlin
@RegisterListener
class JoinListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        event.player.sendMessage("Welcome!".green())
    }
}
```

This also works with Kotlin `object`s, so you can use a singleton instead of a normal class if you prefer.

---

# Events

If a whole listener class feels like too much for a single handler, you can register one inline. `on` just registers it, `subscribe` gives you back an `EventSubscription` so you can unregister it later.

```kotlin
on<PlayerJoinEvent> { it.player.sendMessage("Welcome!".green()) }

val subscription = subscribe<BlockBreakEvent>(EventPriority.HIGH, ignoreCancelled = true) {
    it.player.sendMessage("You broke ${it.block.type}")
}

subscription.unregister()
```

Both take a priority and `ignoreCancelled`, same as `@EventHandler` would.

## Waiting for an event

`awaitEvent` suspends until a matching event fires, which is much nicer than keeping a map of "players I'm waiting on" around. Pass `timeoutTicks` and you get `null` back if nothing matched in time.

```kotlin
SporeCoroutines.launch {
    val move = awaitEvent<PlayerMoveEvent>(timeoutTicks = 100) { it.player == player }

    if (move == null) {
        player.sendMessage("You didn't move in time".red())
        return@launch
    }
}
```

## Suspending handlers

Handlers can suspend. Inline, that's `onAsync` / `subscribeAsync`:

```kotlin
onAsync<PlayerJoinEvent> { event ->
    val profile = profileRepository.find(event.player.uniqueId.toString())

    if (!event.player.isOnline) return@onAsync
    applyProfile(event.player, profile)
}
```

In a `@RegisterListener` class, just mark the method `suspend` - `SporeListeners` spots the signature and registers it for you:

```kotlin
@RegisterListener
class JoinListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onJoin(event: PlayerJoinEvent) {
        val profile = profileRepository.find(event.player.uniqueId.toString())

        if (!event.player.isOnline) return
        applyProfile(event.player, profile)
    }
}
```

Plain Bukkit can't do this - a `suspend fun` compiles to a method with an extra `Continuation` parameter, and `registerEvents` rejects anything that isn't a single `Event` parameter, so the handler would silently never fire. SporeAPI registers those methods itself instead.

Three things to keep in mind:

- **You can't cancel or change the event past a suspension point.** The event finishes and the server moves on while your coroutine is still parked. SporeAPI logs a warning if you attach a suspending handler to a `Cancellable` event for exactly this reason.
- **Priority only decides when the handler starts.** Ordering against other plugins holds up to your first suspension point and means nothing after it.
- **Re-check state after resuming.** The player may have disconnected while you were away, so guard with `player.isOnline`.

Everything before the first real suspension point still runs inline on the main thread, so a `suspend` handler that never actually suspends behaves exactly like a normal one.

## Event bus (your own events)

The helpers above are for Bukkit events. For your own events, on any platform (Paper, Velocity, a bot), there's `SporeBus`:

```kotlin
class PunishmentIssuedEvent(val target: UUID, val reason: String) : Cancellable {
    override var isCancelled = false
}

SporeBus.on<PunishmentIssuedEvent> { event -> Webhook(url).setMessage(event.reason).send() }
SporeBus.on<PunishmentIssuedEvent>(BusPriority.LOWEST) { if (isExempt(it.target)) it.isCancelled = true }

val event = SporeBus.post(PunishmentIssuedEvent(uuid, "Spam"))
if (event.isCancelled) return
```

- Handlers run in priority order (`LOWEST` first, `MONITOR` last), on the thread that calls `post`.
- A handler for a parent class or interface also gets every subtype.
- `on` returns a `BusSubscription` you can `unregister()`. `ignoreCancelled = true` skips cancelled events.
- A handler that throws is reported as an incident, the other handlers still run.
- `onAsync { }` takes a suspending handler. Like with Bukkit events, it can't cancel the event, because `post` returns before it finishes.

---

# Coroutines

`SporeCoroutines` gives you a scope that's tied to your plugin's lifetime, so anything still running gets cancelled on disable instead of leaking into the next reload.

```kotlin
launch { /* starts on the main thread */ }
launchAsync { /* starts off the main thread */ }
```

Both are top-level functions (`me.clearedSpore.sporeAPI.coroutine.launch`), `SporeCoroutines.launch { }` still works too. Inside a coroutine, plain `launch { }` is kotlinx's own, which starts a child of the current coroutine - usually exactly what you want there.

Inside a coroutine you switch threads with `withAsyncCtx` and `withRunCtx`, which is the pattern for "load something slow, then touch the world with it":

```kotlin
launch {
    val data = withAsyncCtx { repository.findBlocking(id) }   // off the main thread
    withRunCtx { player.sendMessage("Loaded $data") }         // back on the main thread
}
```

`delayTicks(20)` is there too if you think in ticks rather than milliseconds.

The main dispatcher only actually schedules a task when it has to - if you're already on the server thread, `withRunCtx` runs your block inline.

## Repeating and delayed work

```kotlin
repeat(5.seconds) { sendHeartbeat() }
repeat(every = 1.minutes, delay = 30.seconds, async = true) { pushConfig() }

delay(10.seconds) { player.sendMessage("10 seconds later") }
```

- They return a `Job`: call `cancel()` to stop or call it off. Everything also stops when the plugin disables / the proxy shuts down.
- `repeat` waits for a run to finish before the next one starts, so runs never pile up or overlap.
- A run that throws is reported as an incident and the loop keeps going.
- They start on the main thread on Paper, pass `async = true` for blocking work. The block is a coroutine, so you can use `withAsyncCtx`, `delay`, etc. inside it.

Durations are Kotlin's (`import kotlin.time.Duration.Companion.seconds`). Kotlin's own `repeat(3) { }` and the suspending `delay(1.seconds)` keep working, they only differ in arguments.

## Rate limiting

```kotlin
private val chatLimit = RateLimiter<UUID>(limit = 5, per = 10.seconds)

if (!chatLimit.tryAcquire(player.uniqueId)) {
    player.error("Slow down! Try again in ${chatLimit.retryAfter(player.uniqueId).inWholeSeconds}s")
    return
}
```

- `tryAcquire(key)` uses one go if there is one left, `remaining(key)` and `retryAfter(key)` tell you where a key stands.
- `acquire(key)` suspends until a go is free instead of failing - good for webhooks and APIs with rate limits.
- Uses refill smoothly over time (a token bucket), and keys that sit idle are dropped, so it doesn't grow forever.

## Caching suspend loads

When a value is expensive to load - a database page, a stats query, a permission lookup - and gets asked for a lot, `SuspendCache` keeps the result around for a while so you don't hit the database every time.

```kotlin
private val cache = SuspendCache<UUID, Stats>(Duration.ofMinutes(1), maximumSize = 5_000)

suspend fun stats(uuid: UUID): Stats = cache.get(uuid) { StatsRepository.loadBlocking(uuid) }.value
```

The loader runs off the main thread, so it's fine to call blocking code in it. A few things it handles for you:

- **One load per key.** If ten players open the same page at once, the loader runs once and they all get the same result.
- **Invalidation wins.** `invalidate(key)` or `invalidateAll()` throws away a load that was still running, so a stale result can't sneak back into the cache after you changed the data.
- **You know how old it is.** `get` returns a `Cached<V>` with the `value` and `loadedAt`, handy for an "updated 30s ago" line.

Entries expire after the `ttl`, and the cache never holds more than `maximumSize` entries.

---

# Scoreboards

`Sidebar` is a scoreboard sidebar built from Adventure components, so you get MiniMessage, hex colours and per-line formatting without touching teams or entry strings yourself. Lines are diffed on every update, so only what actually changed gets sent to the client.

```kotlin
class MySidebar : Sidebar() {

    override val updateIntervalTicks = 20L

    override fun title(player: Player): Component = "<s_blue><b>MyServer".mm()

    override fun lines(player: Player): List<Component> = listOf(
        Component.empty(),
        "<white>Kills: <green>${player.getStatistic(Statistic.PLAYER_KILLS)}".mm(),
        "<white>Online: <green>${Bukkit.getOnlinePlayers().size}".mm()
    )

    override fun shouldShow(player: Player): Boolean = player.world.name != "lobby"
}
```

Then show it:

```kotlin
SidebarManager.show(player, sidebar)
SidebarManager.refresh(player)   // force an update now
SidebarManager.hide(player)
```

One instance can serve every player - the manager keeps the per-player state - and it cleans up on quit and on plugin disable by itself. By default it refuses to take over a scoreboard that another plugin already owns and warns instead; set `SidebarManager.takeOverExisting = true` if you want it to win anyway. Boards are capped at `SidebarManager.MAX_LINES` (15) lines.

## Async sidebars

If a value on your board is expensive - a balance behind a database query, a rank from another plugin's cache - reading it in `lines()` means doing that work on the main thread every update, for every player. `AsyncSidebar` splits it in two: `fetch` runs off the main thread and returns a snapshot, `title` and `lines` run on the main thread with that snapshot.

```kotlin
class MySidebar : AsyncSidebar<MySidebar.Data>() {

    data class Data(val balance: String, val rank: Component)

    override val updateIntervalTicks = 20L

    // Off the main thread - only touch thread-safe things here, not the Bukkit API.
    override fun fetch(player: Player) = Data(
        balance = economy.formatted(player.uniqueId),
        rank = ranks.prefixOf(player.uniqueId)
    )

    // Main thread - statistics, inventories and the world are safe here.
    override fun lines(player: Player, data: Data): List<Component> = listOf(
        "<white>Balance: <green>${data.balance}".mm(),
        "<white>Rank: ".mm().append(data.rank),
        "<white>Kills: <green>${player.getStatistic(Statistic.PLAYER_KILLS)}".mm()
    )

    override fun title(player: Player, data: Data): Component = "<s_blue><b>MyServer".mm()
}
```

Fetches never overlap - if one is still running when the next is due, that tick is skipped - and `lines` is never called before the first snapshot lands, so you get a blank board for a moment rather than a board full of zeroes. Use `fetchIntervalTicks` if you want to fetch less often than you render.

Text that comes from outside your plugin (rank prefixes, nicknames) is usually legacy-coded rather than MiniMessage, so build it into a component and `append` it instead of interpolating it into a `.mm()` string - otherwise the colour codes show up as literal text.

## Tab list

The tab list works with one adapter for the whole server. Set it once and every player online, and everyone who joins later, gets it:

```kotlin
object MyTablist : Tablist() {

    override fun header(viewer: Player) = "<s_blue><b>MyServer".mm()
    override fun footer(viewer: Player) = "<gray>Online: <white>${Bukkit.getOnlinePlayers().size}".mm()

    // How a player shows up in everyone's tab list, null keeps their normal name
    override fun name(target: Player) = "<gray>[${rank(target)}] <white>${target.name}".mm()

    // Higher shows first
    override fun order(target: Player) = rankWeight(target)
}

TablistManager.use(MyTablist)
```

Header and footer are per viewer. Name and order are per player and the same for everyone, that's how Minecraft's tab list works. Order uses Paper's list order, not scoreboard teams, so it never fights with sidebars.

It's built to stay out of the server's way:

- **Spread out.** With the default 20 tick interval about 1/20th of the players update each tick, instead of everyone in the same tick.
- **Only changes are sent.** Header/footer, name and order are compared with what the player already has, so an unchanged tab list costs no packets.
- **Folia safe.** Every player is updated on their own region thread.

If the header or footer needs slow data, use `AsyncTablist`. `fetch` runs off the main thread, and rendering always uses the latest result, so a slow database never holds up a tick:

```kotlin
object StatsTablist : AsyncTablist<Stats>() {

    override val fetchIntervalTicks = 100L

    override fun fetch(viewer: Player): Stats = StatsRepository.loadBlocking(viewer.uniqueId)

    override fun header(viewer: Player, data: Stats) = "<s_blue>Kills: <white>${data.kills}".mm()
}
```

`TablistManager.refresh(player)` / `refreshAll()` update right away (e.g. after a rank change), and `TablistManager.clear()` removes it and resets everyone.

---

# Registries

`Registry` is a thread-safe id-to-value map for your content - items, mobs, quests, whatever - with an optional annotation it can scan for.

```kotlin
object ItemRegistry : Registry<Item>(Item::class, RegisterItem::class) {
    override fun idOf(value: Item): String = value.id
}

ItemRegistry.scan(plugin)          // finds everything annotated with @RegisterItem (Paper, import me.clearedSpore.sporeAPI.scan.scan)
ItemRegistry.scan(MyApp::class.java) // same thing outside Paper: pass your main class
ItemRegistry.register(myItem)      // or register by hand

val item = ItemRegistry["magic_sword"]   // null if missing
val same = ItemRegistry.require("magic_sword")   // throws if missing
```

Registering the same id twice throws `DuplicateRegistrationException` instead of quietly overwriting. Override `onRegister` if you need to hook each entry as it comes in.

Every registry is tracked in `RegistryIndex`, which is what `/<label> debug` uses to list them and their sizes.

---

# Repositories

`Repository` is a small storage interface with two of every method: a `Blocking` one you call when you're already off the main thread, and a `suspend` one that hops off it for you.

```kotlin
val user = userRepository.findBlocking(id)   // blocking, off-thread callers

SporeCoroutines.launch {
    val user = userRepository.find(id)       // suspends, runs off the main thread
}
```

`MongoRepository` implements it against a Mongo collection - you supply the document mapping:

```kotlin
object UserRepository : MongoRepository<User>("users") {
    override fun idOf(value: User) = value.uuid.toString()
    override fun toDocument(value: User) = Document("_id", idOf(value)).append("name", value.name)
    override fun fromDocument(document: Document) = User(
        UUID.fromString(document.getString("_id")),
        document.getString("name")
    )
}
```

Point `SporeMongo` at your database once during enable and every repository can resolve its collection:

```kotlin
SporeMongo.init(myMongoDatabase)
```

Your plugin still owns the client and its lifetime - SporeAPI never opens or closes the connection.

`YamlRepository` is the same interface backed by a folder of `.yml` files, with `write`/`read` instead of `toDocument`/`fromDocument`.

Every blocking call in both goes through the main-thread IO guard, so calling one on the server thread shows up in [debugging](#debugging).

---

# Redis packets

Send packets between servers, proxies and bots over Redis (Jedis pub/sub). Needs `sporeapi-extra`.

Connect once at startup, and disconnect on shutdown:

```kotlin
override fun onPluginEnable() {
    SporeRedis.connect(
        RedisSettings(
            address = config.redis.address,   // "host:port"
            username = config.redis.username,
            password = config.redis.password,
            channel = "atlas"                 // everything on the same channel sees each other's packets
        ),
        MyPlugin::class.java                  // scans your jar for packets and packet listeners
    )
}

override fun onPluginDisable() {
    SporeRedis.shutdown()
}
```

A packet is a class extending `Packet`, its fields are sent as JSON:

```kotlin
@RegisterPacket
class KickPlayerPacket(val uuid: String, val lines: List<String>) : Packet()

KickPlayerPacket(uuid, listOf("Banned")).sendAsync()   // or send() to publish on this thread
```

Listen with a class full of `onReceive` methods, or a lambda:

```kotlin
@RegisterPacketListener
class NetworkListener {
    fun onReceive(packet: KickPlayerPacket) { /* ... */ }
    fun onReceive(packet: BroadcastPacket) { /* ... */ }
}

SporeRedis.onReceive<KickPlayerPacket> { packet -> /* ... */ }
```

Or let the packet handle itself, `this` is the packet that arrived:

```kotlin
@RegisterPacket
class StaffChatPacket(val sender: String, val message: String) : Packet() {

    override fun onReceive() {
        Bukkit.broadcast("<s_blue>[Staff] <white>$sender: $message".mm(), "myplugin.staff")
    }
}
```

That's handy for packets only one platform uses. A packet in a shared module that Paper and Velocity handle differently still needs a `@RegisterPacketListener` per platform, since the shared module can't call Bukkit or Velocity code. Use `@RegisterPacket(async = true)` to run `onReceive()` off the main thread. It runs next to any listeners for the same packet, and the packet needs `@RegisterPacket` (or `SporeRedis.register<T>()`) so it's picked up.

- **Main thread by default.** Listeners run on SporeAPI's main dispatcher, which is the main thread on Paper, so you can touch the world straight away. Use `@RegisterPacketListener(async = true)` or `onReceive(async = true)` for blocking work.
- **Reconnects by itself.** If Redis goes down the subscriber retries (1s, 2s, 4s ... up to 30s) and picks up again when it's back. If Redis isn't reachable at startup it keeps retrying in the background instead of breaking your plugin.
- **Errors are incidents.** A listener that throws, or a packet that can't be read, shows up in [debugging](#debugging). The other listeners still run. `/mycommand debug` shows the connection and how many packets were sent and received.
- **Your own packets.** Redis also delivers a packet to the server that sent it. Set `receiveOwnPackets = false` to skip those.
- `@RegisterPacket` is only needed for packets nobody listens to locally: a listener registers its packet type on its own. `onReceive` returns a subscription you can `unregister()`.
- `send()` is blocking IO, so it's flagged by the main-thread IO guard. Use `sendAsync()` on the main thread.

The JSON format (`{"type": "<class name>", "data": {...}}`) is the same as Atlas uses, so packets still match as long as the class names do.

---

# Cooldowns

`Cooldown` lets you track a cooldown for anything with a UUID, not just players.

```kotlin
Cooldown.createCooldown("mine_ability", 10) // 10 second cooldown
Cooldown.addCooldown("mine_ability", player.uniqueId)

if (Cooldown.isOnCooldown("mine_ability", player.uniqueId)) {
    player.sendMessage("Still on cooldown!")
}
```

There are also some shortcuts on `CommandSender` to keep your code cleaner:

```kotlin
if (sender.withCooldown("mine_ability", 10)) {
    // runs your logic and starts the cooldown, only if not already on cooldown
}
```

Expired cooldowns aren't removed on their own - they don't do anything, they just sit in memory. Call `Cooldown.cleanup()` whenever you want to clear them out, for example from a repeating task. To end one player's cooldown early, use `Cooldown.removeCooldown(id, uuid)`.

If you only need a cooldown inside one class (anti-spam on a toggle, a pay command), `CooldownMap` is simpler - its entries expire by themselves, and you can wipe it with `clear()`:

```kotlin
private val payCooldown = CooldownMap<UUID>(15, TimeUnit.SECONDS)

if (payCooldown.isOnCooldown(player.uniqueId)) return
payCooldown.add(player.uniqueId)
```

---

# Confirmations

If you have a dangerous command and want the player to confirm it by typing it twice, `Confirmation` makes that easy.

```kotlin
if (!Confirmation.isPlayerPending(player.uniqueId)) {
    Confirmation.addPlayer(player.uniqueId)
    return
}

Confirmation.removePlayer(player.uniqueId)
// do the dangerous thing
```

---

# Tasks and scheduling

`Tasks` is a simple wrapper around Bukkit's scheduler, so you don't need to pass your plugin instance around everywhere.

```kotlin
Tasks.run { /* runs next tick */ }
Tasks.runAsync { /* runs off the main thread */ }
Tasks.runLater(20) { /* runs in 1 second */ }
Tasks.runTimer(0, 20) { /* runs every second */ }
```

If you'd rather work with real time instead of ticks, `TaskBuilder` lets you use durations:

```kotlin
TaskBuilder(plugin)
    .async()
    .delay(TimeUtil.seconds(2))
    .repeat(TimeUtil.seconds(5))
    .run { /* your code */ }
```

`TimeUtil` can also turn a string like `"1d2h30m"` into a duration with `TimeUtil.parse("1d2h30m")`, which is handy for things like ban lengths or cooldown configs.

---

# Discord webhooks

If you need to send a Discord message with an embed, use `Webhook`.

```kotlin
Webhook("https://discord.com/api/webhooks/...")
    .setMessage("Something happened!")
    .setUsername("Server Bot")
    .addEmbed(
        Webhook.Embed()
            .setTitle("Player joined")
            .setDescription("${player.name} joined the server")
            .setColor(0x00FF00)
    )
    .send()
```

For a quick one-liner without embeds, `Logger.log(webhookURL, message)` still works too.

---

# Item builder

`ItemBuilder` lets you build an `ItemStack` without all the usual boilerplate.

```kotlin
val item = ItemBuilder(Material.DIAMOND_SWORD)
    .setName("Legendary Sword".gold())
    .setLore("A sword of great power", "Right click to use")
    .addEnchant(Enchantment.SHARPNESS, 5, true)
    .setGlow(true)
    .build()
```

You can also start from an existing item with `ItemBuilder.of(item)` if you just want to tweak it a bit.

---

# Serialization

SporeAPI can turn common Minecraft objects into strings and back, so you can easily save them to a config or database. `Location`, `ItemStack`, and `Inventory` are supported out of the box.

```kotlin
val saved = SporeSerializer.serialize(player.location)
val loaded = SporeSerializer.deserialize(saved, Location::class.java)
```

For anything else, it just falls back to normal JSON, so it works with most of your own data classes too. If you want full control over how a type is saved, you can register your own codec with `SporeCodecRegistry.register(MyType::class.java, MyCodec())`.

---

# Timings

Time a series of steps, like everything a reload command does, and send the result at the end:

```kotlin
@Command("atlas reload")
suspend fun reload(sender: CommandSender) {
    val timing = Timing("Atlas reload")

    withAsyncCtx {
        timing.step("config") { AtlasConfig.reload() }
        timing.step("punishments") { PunishmentConfig.reload() }
        timing.step("shop") { SaintCoinsShopService.reload() }
        timing.stepSuspend("tebex") { TebexService.refresh() }
    }

    sender.sendMessage(timing.summary())
}
```

That sends **"Atlas reload took 132ms"**, and hovering it shows every step with its time, the slowest one highlighted.

- A step that throws doesn't stop the rest: it's reported as an incident and shows up as **✗** in red, and the summary says how many failed.
- `step` returns the block's result (or null if it failed).
- `timing.total`, `timing.steps` and `timing.failed` are there if you want to build your own message. `summaryText()` and `breakdownText()` give you the MiniMessage strings.

For things that run over and over (a database load, a packet handler), `Timings.timed("db.loadPlayer") { ... }` keeps running stats. The slowest ones show up in `/mycommand debug`, and `/mycommand debug timings` lists them all with average, max and count.

---

# Debugging

SporeAPI has two tools for this: the **main-thread IO guard**, which catches the calls that cause lag, and **incidents**, which turn errors into something you can actually read.

## Main-thread IO

The usual reason a server starts lagging is something blocking the main thread - a database read, a file write - somewhere you forgot about. `SporeDebug` watches for that. Every blocking repository call goes through it, and you can wrap your own with `blockingIo`:

```kotlin
fun loadSettings(): Settings = blockingIo("settings.load") {
    // reads a file, queries an API, whatever
}
```

The guard is **off by default** (`IGNORE`). It's meant for server admins tracking down lag: they turn it on with `/mycommand debug io` (see [Commands](#commands-1)) without a restart, and turn it off again once they're done.

While it's on, a wrapped call that runs on the server thread logs a warning with a stack trace pointing at the caller. Repeats of the same operation are collapsed so one bad call in a loop can't spam your console.

```kotlin
SporeDebug.mainThreadIoPolicy = MainThreadIoPolicy.THROW   // IGNORE (default), WARN, THROW
```

Setting it in code is for development - `THROW` fails loudly the first time instead of leaving you to notice the lag later. Startup and shutdown are exempt by default, since blocking there is usually deliberate; set `SporeDebug.reportDuringLifecycle = true` if you want those too.

## Incidents

Every error SporeAPI catches - a listener that throws, a failed database call, a coroutine that crashes - becomes an **incident**. Instead of a wall of stack trace, you get this:

```text
[MyPlugin] (error) kit.give failed [ref sLOi8] - seen 3x
[MyPlugin] (error)   What:    NumberFormatException: For input string: "abc"
[MyPlugin] (error)   Why:     Some text couldn't be read as a number.
[MyPlugin] (error)   Hint:    Check config values and command arguments that are supposed to be numbers.
[MyPlugin] (error)   Where:   KitService.kt:42 (KitService.give)
[MyPlugin] (error)   Inside:  listener.KitListener.onJoin
[MyPlugin] (error)   Details: player=Steve
[MyPlugin] (error)   Thread:  Server thread (main thread), while running
[MyPlugin] (error)   More:    /mycommand debug error sLOi8
```

- **Where** is the first line of *your* code in the trace - not Bukkit's, not SporeAPI's.
- **Why** and **Hint** come from a built-in list of common errors, which you can add to.
- The same error from the same line is grouped: it's logged at most once every 30 seconds, and it keeps the same ref, so the ref a player gave you still works later.

Warnings print just the summary. Errors add a short trace underneath - `IncidentReporter.consoleTraceDepth` sets how many lines, 0 turns it off.

### runDebug

Wrap anything that might fail in `runDebug`. It returns the result, or `null` if the block threw (after reporting it), so a fallback is just `?:`:

```kotlin
val kits = runDebug("kits.load") { loadKits() } ?: emptyList()
```

Add details and they show up in the report, and use `onFailure` to tell the player something went wrong:

```kotlin
runDebug(
    "kit.give",
    details = mapOf("player" to player.name, "kit" to kit.id),
    onFailure = { player.error("Couldn't give you that kit (ref ${it.id})") }
) {
    kit.giveTo(player)
}
```

The default severity is `ERROR`. Pass `Severity.WARNING` when there's a fallback and it isn't a big deal, or `Severity.CRITICAL` when the plugin can't work without it. Name operations like `area.action` - `kits.load`, `profiles.save` - so they read the same as the IO guard's.

Inside coroutines, use `runDebugSuspending`. Real cancellation still goes through untouched, but a `withTimeout` that runs out inside the block counts as a failure:

```kotlin
val profile = runDebugSuspending("profiles.load") { profiles.find(uuid) }
```

`runDebug` isn't `inline` on purpose: its block can't suspend, which is what keeps the breadcrumbs below on the right thread.

If you already have a `catch`, report it directly:

```kotlin
} catch (e: Exception) {
    IncidentReporter.report("shop.purchase", e, Severity.ERROR, mapOf("item" to item.id))
}
```

### Breadcrumbs

Incidents remember what they happened *inside of*. Listeners, event handlers and `runDebug` blocks all add themselves, so a failure in `kit.give` called from a join listener shows `Inside: listener.KitListener.onJoin`. You can add your own layer without catching anything:

```kotlin
DebugContext.inside("arena.start") {
    // anything reported in here shows "Inside: arena.start"
}
```

### Already covered

You don't need to wrap these - they go through the reporter already:

- `@EventHandler` methods registered through `SporeListeners` (normal and `suspend`), plus `on<T> { }` and `onAsync<T> { }` handlers
- Coroutines started with `SporeCoroutines.launch` / `launchAsync` that throw
- Every `MongoRepository` and `YamlRepository` call
- Modules, commands, listeners and registry entries that fail to register or enable
- Async sidebar fetches, webhook sends and `SporeSerializer` decoding, as warnings

### Explanations

The built-in list covers the usual suspects: null pointers, bad numbers, async Bukkit calls, main-thread IO, missing classes, file and network errors, MongoDB timeouts and logins, broken YAML and more. Add your own for your plugin's exceptions:

```kotlin
IncidentExplainers.register<KitNotFoundException> {
    Explanation("That kit doesn't exist.", "Check the kit id in kits.yml.")
}
```

Yours win over the built-in ones, and returning `null` passes it on - handy for matching on the message. For a library that might not be installed, register by class name so nothing gets loaded:

```kotlin
IncidentExplainers.register(
    "com.zaxxer.hikari.pool.HikariPool\$PoolInitializationException",
    "Couldn't connect to the SQL database.",
    "Check the host, port and password in config.yml."
)
```

### Commands

Register the debug command:

```kotlin
SporeDebugCommand.register(cloudCommandManager, "mycommand", "myplugin.admin")
```

| Command                              | What it does                                                                                           |
|--------------------------------------|--------------------------------------------------------------------------------------------------------|
| `/mycommand debug`                   | Overview - scheduler load, coroutines, sidebars, tab lists, Mongo, registries, the IO guard, the slowest timings and an incident count |
| `/mycommand debug timings`           | Every `Timings.timed` operation, slowest average first                                                 |
| `/mycommand debug timings clear`     | Forgets every timing                                                                                   |
| `/mycommand debug errors`            | The last 10 incidents, newest first. Click one for details                                             |
| `/mycommand debug error <ref>`       | Everything about one incident: what, why, where, breadcrumbs, how often, and a short trace             |
| `/mycommand debug error <ref> trace` | Prints the full stack trace to the console                                                             |
| `/mycommand debug errors clear`      | Forgets every incident                                                                                 |
| `/mycommand debug io`                | Toggles the IO guard without a restart                                                                 |

Refs tab-complete, and a lowercase ref still works as long as only one incident matches.

To alert staff in chat when something breaks, pass `notifyStaff = true` - everyone online with the permission gets a clickable message for every new error:

```kotlin
SporeDebugCommand.register(cloudCommandManager, "mycommand", "myplugin.admin", notifyStaff = true)
```

On Velocity it's the same command, registered with `SporeDebugCommand.register(commandManager, "mycommand", "myplugin.admin")` from `SporeVelocityPlugin`.

### Hooking in

`IncidentReporter.onIncident { }` runs for every incident that gets logged, which is how you'd send them to Discord, a database, or anywhere else:

```kotlin
IncidentReporter.onIncident { incident ->
    if (incident.severity != Severity.WARNING) {
        Webhook(webhookUrl).setMessage("${incident.operation} failed (ref ${incident.id})").sendAsync()
    }
}
```

It runs on whichever thread reported the error, so keep anything slow off it.

---

# Action bar

Paper's normal actionbar only lets you show one message at a time, so if two parts of your plugin both want to show something, they'll overwrite each other. `ActionBar` fixes that by letting you show several named messages at once, which get merged together automatically.

```kotlin
ActionBar.actionBar(player, "cooldown", "Ability ready in 5s".red())
ActionBar.actionBar(player, "status", "In combat".gold())
```

Both messages will show at the same time, separated by a `|`, and each one disappears on its own once its time runs out.
