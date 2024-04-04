package me.mykindos.betterpvp.core.logging.type;

public enum UUIDLogType {
    /**
     * An UUIDItem is spawned
     */
    ITEM_SPAWN,
    /**
     * Someone kills  of the UUIDItem holder
     */
    ITEM_KILL,
    /**
     * Someone participates in the kill of an UUIDItem holder
     */
    ITEM_CONTRIBUTOR,
    /**
     * UUIDItem holder dies
     */
    ITEM_DEATH,
    /**
     * UUIDItem holder dies to a player
     */
    ITEM_DEATH_PLAYER,
    /**
     * An UUIDItem is picked up or moved from an inventory
     */
    ITEM_RETREIVE,
    /**
     * An UUIDItem is stored in a container
     */
    ITEM_CONTAINER_STORE,
    /**
     * An UUIDItem is dropped due to a container being destroyed
     */
    ITEM_CONTAINER_BREAK,
    /**
     * An UUIDItem is dropped to a container being blown up
     */
    ITEM_CONTAINER_EXPLODE,
    /**
     * An UUIDItem is picked up by an inventory
     */
    ITEM_INVENTORY_PICKUP,
    /**
     * An UUIDItem is moved by one inventory to another
     */
    ITEM_INVENTORY_MOVE,
    /**
     * An UUIDItem is dispensed from a block
     */
    ITEM_BLOCK_DISPENSE,
    /**
     * An UUIDItem holder logs out
     */
    ITEM_LOGOUT,
    /**
     * An UUIDItem holder logs in
     */
    ITEM_LOGIN,
    /**
     * An UUIDItem holder drops their UUIDItem
     */
    ITEM_DROP,
    /**
     * A player pick up an UUIDItem
     */
    ITEM_PICKUP,
    /**
     * An UUIDItem despawns
     */
    ITEM_DESPAWN,
    /**
     * A custom, manual generated log
     */
    ITEM_CUSTOM
}
