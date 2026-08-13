# SporeAPI

SporeAPI is a Kotlin Minecraft API that you can use in your projects. It includes features such as menus, messages, a logger, boss bars, commands, cooldowns, and much more!

![License](https://img.shields.io/github/license/ClearedSpore/SporeAPI)
![Latest release](https://img.shields.io/github/v/release/ClearedSpore/SporeAPI)
![Kotlin](https://img.shields.io/badge/kotlin-2.2.20-blueviolet?logo=kotlin)
![PaperMC](https://img.shields.io/badge/papermc-1.21+-blue?logo=spigotmc)
![Author](https://img.shields.io/badge/author-ClearedSpore-brightgreen)

# Features

- [Advanced Menu system](#menu-system)
- [Message utility](#messages)
- [Logger utility](#logger)
- [Boss bars](#boss-bars)
- [Chat input](#chat-input)
- [Commands and listeners](#commands-and-listeners)
- [Cooldowns](#cooldowns)
- [Confirmations](#confirmations)
- [Tasks and scheduling](#tasks-and-scheduling)
- [Discord webhooks](#discord-webhooks)
- [Item builder](#item-builder)
- Serialization
- And much more!

> ⚠️ This is not a plugin, it's an API/library that your plugins can depend on to reduce boilerplate.

---

# Installation

You can include **SporeAPI** in your project either via **Gradle** or **Maven**.

Replace the version with the latest API version.

![Latest Version](https://img.shields.io/github/v/release/ClearedSpore/SporeAPI)
 
You can find the repository and dependency [here](https://repo.sporedev.eu/#/releases/eu/sporedev)

---

# Getting started

Most of the features below (commands, listeners, item builder, tasks, boss bars, action bar, serialization) set themselves up automatically, but only if your main class extends `SporePlugin` instead of `JavaPlugin`.

```kotlin
class TestingPlugin : SporePlugin() {

    override fun onEnable() {
        super.onEnable()
        Logger.initialize("Your plugin name")
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

# Commands and listeners

SporeAPI is built on top of ACF (Aikar's Command Framework), and can register your commands and listeners for you automatically, so you don't have to do it by hand in `onEnable`.

For this to work, your main class needs to extend `SporePlugin` (see [Getting started](#getting-started)).

## Commands

Extend `SporeCommand` like you normally would with ACF, and add `@RegisterCommand` on top of the class. SporeAPI will find it and register it for you.

```kotlin
@RegisterCommand
class TestCommand : SporeCommand() {

    @CommandAlias("test")
    fun onTest(sender: CommandSender) {
        sender.sendMessage("It works!")
    }
}
```

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

# Action bar

Paper's normal actionbar only lets you show one message at a time, so if two parts of your plugin both want to show something, they'll overwrite each other. `ActionBar` fixes that by letting you show several named messages at once, which get merged together automatically.

```kotlin
ActionBar.actionBar(player, "cooldown", "Ability ready in 5s".red())
ActionBar.actionBar(player, "status", "In combat".gold())
```

Both messages will show at the same time, separated by a `|`, and each one disappears on its own once its time runs out.
