package me.mykindos.betterpvp.core.repair;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Right-clicking a placed stonecutter with a salvageable piece of gear consumes the
 * gear and yields 1–3 matching-tier Reinforcements. Items of {@link ItemRarity#EPIC}
 * or higher go through a {@link ConfirmationMenu} first — these are scarce, so a
 * misclick should not silently destroy them.
 * <br>
 * The listener watches the stonecutter material directly rather than verifying the
 * block is a registered {@link SalvageStation} placement; this mirrors how
 * {@code BuildListener} treats any enchanting table as a build editor on this server,
 * and keeps us out of the persistent-block-storage path for what's a stateless action.
 */
@BPvPListener
@Singleton
public class SalvageStationListener implements Listener {

    @Inject
    private ItemFactory itemFactory;

    @Inject
    private SalvageService salvageService;

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        final Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.STONECUTTER) return;

        // Block the vanilla stonecutter GUI unconditionally — this stonecutter is a
        // salvage station, not a recipe selector. Whether the player is holding a
        // salvageable item, an unrelated item, or nothing at all, the GUI never opens.
        event.setCancelled(true);

        final Player player = event.getPlayer();
        final ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            tellNotSalvageable(player);
            return;
        }

        final ItemInstance instance = itemFactory.fromItemStack(held).orElse(null);
        if (instance == null) {
            tellNotSalvageable(player);
            return;
        }

        salvageService.resolve(instance, held).ifPresentOrElse(
                plan -> handlePlan(player, held, plan),
                () -> tellNotSalvageable(player));
    }

    /**
     * Executes immediately for sub-Epic items; otherwise opens a confirmation menu
     * showing the rolled yield. The yield is rolled before the prompt so the player
     * can see exactly what they will get — there's no second roll on confirm.
     */
    private void handlePlan(Player player, ItemStack held, SalvagePlan plan) {
        if (requiresConfirmation(plan)) {
            promptConfirmation(player, held, plan);
        } else {
            execute(player, held, plan);
        }
    }

    /**
     * Whether salvaging this item is destructive enough to warrant a confirmation menu.
     * Epic+ rarity items are scarce; attuned high-purity items are similarly hard to
     * replace since purity rolls cannot be re-rolled once revealed.
     */
    private boolean requiresConfirmation(SalvagePlan plan) {
        if (plan.getTier().isAtLeast(ItemRarity.EPIC)) {
            return true;
        }
        return plan.getSource().getComponent(PurityComponent.class)
                .filter(PurityComponent::isAttuned)
                .map(component -> component.getPurity().isAtLeast(ItemPurity.PRISTINE))
                .orElse(false);
    }

    private void promptConfirmation(Player player, ItemStack held, SalvagePlan plan) {
        final String sourceName = plain(displayName(plan.getSource()));
        final ItemInstance preview = itemFactory.createPreview(plan.getReinforcement());
        final String rewardName = plain(displayName(preview));
        final String description = String.format(
                "Salvage %s for %s? This will destroy the item.",
                sourceName, rewardName);

        new ConfirmationMenu(description, confirmed -> {
            if (!confirmed) return;
            // Re-fetch the held stack — the player may have swapped items while the
            // menu was open. Identity equality on the stack reference catches both
            // a swap (different stack) and a drop (held becomes AIR -> different instance).
            final ItemStack nowHeld = player.getInventory().getItemInMainHand();
            if (!nowHeld.equals(held)) {
                UtilMessage.message(player, "Salvage", Component.text(
                        "You are no longer holding the item you wanted to salvage.", NamedTextColor.RED));
                return;
            }
            execute(player, nowHeld, plan);
        }).show(player);
    }

    private void execute(Player player, ItemStack held, SalvagePlan plan) {
        // Consume a single unit. For gear (maxStackSize == 1) this empties the stack;
        // for stackable items like runes only the one clicked unit is taken.
        held.setAmount(held.getAmount() - 1);

        final ItemInstance produced = itemFactory.create(plan.getReinforcement());
        final ItemStack stack = produced.createItemStack();
        stack.setAmount(plan.getYield());
        UtilItem.insert(player, stack);

        final Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.UI_STONECUTTER_TAKE_RESULT, 1.0f, 1.0f);

        UtilMessage.message(player, "Salvage", Component.text("You salvaged ", NamedTextColor.GRAY)
                .append(Component.text("x" + plan.getYield() + " ", NamedTextColor.YELLOW))
                .append(displayName(produced))
                .append(Component.text(".", NamedTextColor.GRAY)));
    }

    private static Component displayName(ItemInstance instance) {
        return instance.getBaseItem().getItemNameRenderer().createName(instance);
    }

    private void tellNotSalvageable(Player player) {
        UtilMessage.message(player, "Salvage",
                Component.text("Hold a piece of repairable gear to salvage it here.", NamedTextColor.GRAY));
    }

    private static String plain(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }
}
