# SporeAPI

SporeAPI is a Kotlin minecraft API that you can use in your project. It has features such as menus, messages, logger and much more!

# Features

- [Advanced Menu system](#Menu-system)
- Message util
- Logger util
- Chat input feature
- Serialization
- And much more!

> ⚠️ This is not a full plugin — instead, it’s an API / library that your plugins can depend on to reduce boilerplate.

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
	    <version>1.4</version>
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
	        implementation 'com.github.Clearedspore:SporeAPI:1.4'
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
	        implementation("com.github.Clearedspore:SporeAPI:1.4")
	}
```

# Colors

You can easily translate color codes by adding `.translate()` after a string.


# Menu system

Here I will tell you about the basics on how to make a normal menu and a paginated menu.

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

You **have** to load the chatinput class in order for this to work. You simply have to go to your main class and load the instance.

The `this` means the main class instance.

Then you can go ahead and go back to the paginated menu class and add the item. It works the same as a normal item. You provide the `x` and `y` coordinates and then the chatinput instance.

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

## License

SporeAPI is licensed under the [MIT License](LICENSE).
