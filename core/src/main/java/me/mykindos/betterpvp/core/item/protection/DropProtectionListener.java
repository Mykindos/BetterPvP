package me.mykindos.betterpvp.core.item.protection;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.SMALL_CAPS;

/**
 * Listener that enforces "drop protection" for special items.
 *
 * Behavior:
 * - When a player drops an item, this listener checks whether the item has a
 *   UUIDProperty (meaning it is a protected custom item). For unstackable items
 *   with a UUIDProperty, the DropProtectionController is consulted to determine
 *   whether the drop should be allowed or cancelled.
 * - The listener also provides a periodic ticker (UpdateEvent) that iterates the
 *   active drop-protection statuses and updates the player's action bar with how
 *   many drops remain before protection is cleared.
 */
@BPvPListener
@Singleton
public class DropProtectionListener implements Listener {

    /** Controller responsible for tracking per-player drop protection state. */
    private final DropProtectionController controller;

    /** Client manager used to retrieve gamer/client wrappers for online players. */
    private final ClientManager clientManager;

    /** Factory for converting Bukkit ItemStacks to the project's ItemInstance model. */
    private final ItemFactory itemFactory;

    /**
     * Constructs the listener with required dependencies. This is injected by Guice.
     *
     * @param controller   drop protection controller
     * @param clientManager client manager for finding gamer objects
     * @param itemFactory  factory to map ItemStack -> ItemInstance
     */
    @Inject
    private DropProtectionListener(DropProtectionController controller, ClientManager clientManager, ItemFactory itemFactory) {
        this.controller = controller;
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
    }

    /**
     * Handle player drop events.
     *
     * This method will:
     * - Ignore stackable items (only cares about unstackable items with max stack size 1).
     * - Convert the dropped ItemStack to an ItemInstance and check for a UUIDProperty.
     * - If the item has no UUIDProperty it is not protected and the method returns.
     * - Otherwise the DropProtectionController.drop(...) method is called; if it
     *   returns false, the drop is cancelled to enforce protection.
     *
     * @param event PlayerDropItemEvent fired by the server when a player drops an item
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        final ItemStack itemStack = event.getItemDrop().getItemStack();
        final Player player = event.getPlayer();

        if (itemStack.getMaxStackSize() != 1) {
            return; // We only care about unstackable items
        }

        final boolean enabled = (boolean) clientManager.search().online(player)
                .getProperty(ClientProperty.DROP_PROTECTION_ENABLED)
                .orElse(false);
        if (!enabled) {
            return; // Drop protection is disabled for this player
        }

        final ItemInstance itemInstance = itemFactory.fromItemStack(itemStack).orElseThrow();
        final Optional<UUIDProperty> component = itemInstance.getComponent(UUIDProperty.class);
        if (component.isEmpty()) {
            return; // Item is not protected by a UUID
        }

        // Disallow drop if the controller says so
        if (!controller.drop(player, itemInstance)) {
            event.setCancelled(true);
            final DropProtectionStatus status = controller.getStatus(player.getUniqueId());
            Preconditions.checkNotNull(status);
            float pitch = 2 - ((float) status.getRemainingDrops() / controller.getRequiredDrops()) + 0.2f;
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_CHIME, pitch, 1.0F).play(player);
        } else {
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_CHIME, 2, 1.0F).play(player);
        }
    }

    /**
     * Periodic tick that updates action bars for players currently under drop protection.
     *
     * The method iterates the controller's statuses map and for each online player
     * displays the remaining number of drops required to clear protection via the
     * gamer's action bar. Offline or invalid players are removed from the map.
     */
    @UpdateEvent
    public void ticker() {
        final Iterator<Map.Entry<@NotNull UUID, DropProtectionStatus>> iterator = controller.getStatuses().entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<@NotNull UUID, DropProtectionStatus> entry = iterator.next();
            final DropProtectionStatus status = entry.getValue();
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || !player.isValid()) {
                iterator.remove();
                continue;
            }

            final Gamer gamer = clientManager.search().online(player).getGamer();
            gamer.getTitleQueue().add(50, TitleComponent.subtitle(
                    0,
                    0.3, // 1 tick + 0.01 seconds
                    0,
                    false,
                    gmr -> getSubtitle(status)
            ));
        }
    }

    private @NotNull TextComponent getSubtitle(DropProtectionStatus status) {
        final ProgressColor color = new ProgressColor((float) status.getRemainingDrops() / controller.getRequiredDrops());
        return Component.empty()
                .append(Component.text("Drop", NamedTextColor.GRAY).font(SMALL_CAPS))
                .appendSpace()
                .append(Component.text(status.getRemainingDrops(), color.getTextColor(), TextDecoration.BOLD))
                .appendSpace()
                .append(Component.text("more times", NamedTextColor.GRAY).font(SMALL_CAPS));
    }
}
