package me.mykindos.betterpvp.core.item.armor;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * An abstract base class for events that describe an interaction when the armor
 * of a Player is changing.
 * <p>
 * <a href="https://github.com/Unp1xelt/ArmorChangeEvent/">Taken from GitHub</a>
 */
public abstract class ArmorEvent extends Event implements Cancellable {

    private Result result = Result.DEFAULT;

    private final ItemStack item;
    private final EquipmentSlot slot;
    private final Player who;
    private final ArmorAction action;

    public ArmorEvent(@NotNull final Player who, @NotNull final ItemStack item,
                      @NotNull final EquipmentSlot slot, @NotNull final ArmorAction action) {
        this.who = who;
        this.item = item;
        this.slot = slot;
        this.action = action;
    }

    /**
     * Get the player on which the armor was changed.
     *
     * @return The armor changing entity.
     */
    @NotNull
    public final Player getPlayer() {
        return who;
    }

    /**
     * Gets the item that is being equipped or unequipped. Modifying the
     * returned item will have no effect.
     *
     * @return An ItemStack for the item being equipped or unequipped.
     */
    @NotNull
    public final ItemStack getItem() {
        return item;
    }

    /**
     * Gets the armor equipment slot were the item is equipped or unequipped.
     *
     * @return The armor equipment slot.
     */
    @NotNull
    public final EquipmentSlot getArmorSlot() {
        return slot;
    }

    /**
     * Gets the action used to equip or unequip an item.
     *
     * @return The action used.
     */
    @NotNull
    public final ArmorAction getAction() {
        return action;
    }

    /**
     * Sets the result of this event. This will change whether this event is
     * considered cancelled.
     *
     * @param newResult the new {@link org.bukkit.event.Event.Result} for this event
     * @see #isCancelled()
     */
    public void setResult(@NotNull Result newResult) {
        result = newResult;
    }

    /**
     * Gets the {@link org.bukkit.event.Event.Result} of this event. The Result
     * describes the behavior that will be applied to the inventory in relation
     * to this event.
     *
     * @return the Result of this event.
     */
    @NotNull
    public Result getResult() {
        return result;
    }

    /**
     * Gets whether this event is cancelled. This is based off of the
     * Result value returned by {@link #getResult()}.  Result.ALLOW and
     * Result.DEFAULT will result in a returned value of false, but
     * Result.DENY will result in a returned value of true.
     * <p>
     * {@inheritDoc}
     *
     * @return whether the event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return getResult() == Result.DENY;
    }

    /**
     * Proxy method to {@link #setResult(org.bukkit.event.Event.Result)} for the
     * Cancellable interface. {@link #setResult(org.bukkit.event.Event.Result)}
     * is preferred, as it allows you to specify the Result beyond Result.DENY
     * and Result.ALLOW.
     * <p>
     * {@inheritDoc}
     *
     * @param toCancel result becomes DENY if true, ALLOW if false
     */
    @Override
    public void setCancelled(boolean toCancel) {
        setResult(toCancel ? Result.DENY : Result.ALLOW);
    }

    /**
     * Updates an event that is cancellable to the result of this event.
     *
     * @param toCancel The cancellable event to update.
     */
    protected void updateCancellable(@NotNull Cancellable toCancel) {
        toCancel.setCancelled(isCancelled());
    }

    public enum ArmorAction {
        /**
         * When item is manually equip or unequip.
         */
        CLICK,
        /**
         * When shift clicking an armor piece to equip or unequip.
         */
        SHIFT_CLICK,
        /**
         * When item is unequip through double click, which collect's it to the
         * cursor.
         */
        DOUBLE_CLICK,
        /**
         * When to drag and drop the item to equip or unequip.
         */
        DRAG,
        /**
         * When pressing the drop key (defaults to Q) over an armor slot to
         * unequip.
         */
        DROP,
        /**
         * When right clicking an armor piece in the hotbar without the inventory
         * open to equip.
         */
        HOTBAR,
        /**
         * When pressing the number key (0-8) or F while hovering over the armor
         * slot to equip or unequip.
         */
        HOTBAR_SWAP,
        /**
         * When in range of a dispenser that shoots an armor piece to equip.
         */
        DISPENSED,
        /**
         * When an armor piece is removed due to it losing all durability.
         */
        BROKE,
        /**
         * When you die causing all armor to unequip.
         */
        DEATH,
        /**
         * Use this if you call this event by yourself.
         */
        CUSTOM;
    }
}