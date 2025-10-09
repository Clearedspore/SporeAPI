# SporeAPI

SporeAPI is a Kotlin Minecraft API that you can use in your projects. It includes features such as menus, messages, a logger, and much more!

![License](https://img.shields.io/github/license/ClearedSpore/SporeAPI)
![Latest release](https://img.shields.io/github/v/release/ClearedSpore/SporeAPI)
![Kotlin](https://img.shields.io/badge/kotlin-2.2.20-blueviolet?logo=kotlin)
![PaperMC](https://img.shields.io/badge/papermc-1.16+-blue?logo=spigotmc)
![Author](https://img.shields.io/badge/author-ClearedSpore-brightgreen)

# Features

- [Advanced Menu system](#menu-system)  
- [Message utility](#messages)  
- [Logger utility](#logger)  
- [Chat input feature](#chatinput)  
- Serialization  
- And much more!  

> ⚠️ This is not a plugin — it’s an API/library that your plugins can depend on to reduce boilerplate.

---

# Installation

You can include **SporeAPI** in your project either via **Gradle** or **Maven**.  

## Maven

#### Repository
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
#### Dependency
```xml
<dependency>
    <groupId>com.github.Clearedspore</groupId>
    <artifactId>SporeAPI</artifactId>
    <version>1.5.8</version>
</dependency>
```

## Gradle

### Groovy
#### Repository
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```
#### Dependency
```groovy
dependencies {
    implementation 'com.github.Clearedspore:SporeAPI:1.5.8'
}
```

### Kotlin
#### Repository
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```
#### Dependency
```kotlin
dependencies {
    implementation("com.github.Clearedspore:SporeAPI:1.5.8")
}
```

---

# Messages

## Colors

You can easily translate color codes by adding `.translate()` to a string.  
It supports both `&` color codes and `&#RRGGBB` hex color codes.

There are also a few pre-made color methods you can use such as `.blue()`, `.white()`, and `.red()`.

## Message utility

There are many utility methods for sending messages.

You can call the `Message` class to access them. Some methods are not shown directly because they are extensions of the `Player` class.

- `sendBossBar` – Send a bossbar:  
  ```kotlin
  player.sendBossBar(player, text, progress)
  ```

- `sendMessageWithTitle` – Send a title and subtitle:  
  ```kotlin
  player.sendMessageWithTitle(player, title, subtitle)
  ```

- `endTimedBossBar` – Send a timed bossbar:  
  ```kotlin
  player.endTimedBossBar(plugin, player, title, progress, duration)
  ```

- `sendActionBar` – Send an actionbar:  
  ```kotlin
  player.sendActionBar(player, message)
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
log(sender, permission, message)
```

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
These work the same, except the plugin name includes “Database”:

```
[TestingPlugin Database] (info) Connected to H2 database.
```

Methods:  
```kotlin
infoDB(message)
errorDB(message)
warnDB(message)
```

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
## Footer Paginated menu

If you want to use footerpaginated menus the only thing you have to add is the `true` boolean in the implemention arguments.
```kotlin
class TestingPaginatedMenu() : BasePaginatedMenu(TestingPlugin.instance, true) {
```

## Extra features

### Search item

**THIS ONLY WORKS FOR A PAGINATED MENU!**

If you want to add a search feature to your money you can do that only by typing **1** line!

You only have to call the `addSearchItem()` method and then it will add a pre-made search item. 

When you click on the item it will close the menu and add you to the chat input. Once you have typed your input then it will re-open the menu and apply the search.

**Chatinput:**

You **have** to load the chatinput class in order for this to work. You simply have to go to your main class and load the instance.

The `this` means the main class instance.

Then you can go ahead and go back to the paginated menu class and add the item. It works the same as a normal item. You provide the `x` and `y` coordinates and then the chatinput instance.

If you want to use the chatinput for something else you can simply do this.

```kotlin
chatInput.awaitChatInput(clicker) { input ->
	player.sendMessage(input)
}
```

```kotlin
class TestingPlugin : JavaPlugin() {

    companion object {
        lateinit var instance: TeamCraft
    }

    lateinit var chatInput: ChatInput

    override fun onEnable() {
        instance = this
        chatInput = ChatInput(this)
    }
} 
```

Menu Example:
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
        
        addSearchItem(5, 6, TestingPlugin.instance.chatInput)
    }

    override fun onInventoryClickEvent(
        clicker: Player,
        clickType: ClickType,
        event: InventoryClickEvent
    ) {}
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

