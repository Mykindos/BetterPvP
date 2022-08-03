# Lunar Client Cooldown API

`The object with creating the cooldown API is to provide a really easy solution for creating cooldowns in Lunar Client.`

`Below are a few examples of how this can be implemented and what each implementation works well for.`

``Note that while the API provides an easy path for most use cases, if your use case requires something different you can always send the packet using LunarClientAPI.``

## Examples:

---
### Example 1 - Static Timers.
```java
  /**
     * Called when your plugin loads, used to register a static
     * cooldown, where the time doesn't change.
     *
     * This will allow you to reuse the object without keeping track of it yourself.
     */
    private void loadPlugin() {
        LunarClientAPICooldown.registerCooldown(new LCCooldown("CombatTag", 30, Material.DIAMOND_SWORD));
    }

    /**
     * Called when a player attacks another player in your code.
     * This will apply our previously registered 30 second timer, with a diamond sword image.
     *
     * @param attacker One of the players to receive the 30 second timer.
     * @param victim One of the players to receive the 30 second timer.
     */
    private void attackPlayer(Player attacker, Player victim) {
        LunarClientAPICooldown.sendCooldown(attacker.getPlayer(), "CombatTag");
        LunarClientAPICooldown.sendCooldown(victim.getPlayer(), "CombatTag");
    }

    /**
     * Called when a player dies (or some other action) in your code.
     * This will remove our previously applied 30 second timer if it exists.
     *
     * @param player The player to clear the timer for.
     */
    public void playerDeath(Player player) {
        LunarClientAPICooldown.clearCooldown(player, "CombatTag");
    }
```

---

### Example 2 - Dynamic Teleport Timers

```java
    /**
     * This is the least performant option, but sometimes a good option.
     * This will create a NON-TRACKED {@link LCCooldown} that will send to a player.
     *
     * This is best used if you need a one-off cooldown that has a dynamic time or icon, meaning
     * that it will change everytime. In this example, we have a player teleporting with a variable
     * amount of time with the EnderPearl icon.
     *
     * @param player The player to receive the timer.
     * @param variableTime The variable amount of time that should be applied to the cooldown.
     */
    private void teleportTimer(Player player, int variableTime) {
        new LCCooldown("Teleport", variableTime, Material.ENDER_PEARL).send(player);
    }
```

---

### Example 3 - Limited Objects with Dynamic Timers


```java
    /**
     * This is the most performant option, but will require you to keep the LCPacketCooldown object yourself.
     * If you don't understand what is going on in this, or you do not know how to persist and pass the object
     * properly, this probably isn't the best solution for you.
     *
     * This will create just a packet object, and send it to the player(s). This is best suited for extremely
     * performant servers, or servers that have a need for dynamic timers or icons and don't want the overhead of
     * 2 objects per cooldown.
     *
     * @param player The player to send the cooldown.
     * @param variableTime The amount of time (in seconds) to send in the cooldown.
     */
    private void teleportTimerLimitedObjects(Player player, int variableTime) {
        LunarClientAPI.getInstance().sendPacket(player, new LCPacketCooldown("Teleport", variableTime * 1000L, Material.ENDER_PEARL.getId()));
    }
```
