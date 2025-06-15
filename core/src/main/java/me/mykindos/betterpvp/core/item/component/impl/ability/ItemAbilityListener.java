package me.mykindos.betterpvp.core.item.component.impl.ability;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@BPvPListener
@Singleton
public class ItemAbilityListener implements Listener {

    private final Map<Player, HoldData> heldMap = new WeakHashMap<>();
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;

    @Inject
    public ItemAbilityListener(ClientManager clientManager, ItemFactory itemFactory) {
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
    }

    // MARK: Left Click
    // MARK: Right Click
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        final Block block = event.getClickedBlock();
        if (block != null && UtilBlock.isInteractable(block)) {
            return; // Prevent interaction with blocks that are interactable, so you can use them.
        }

        itemFactory.fromItemStack(itemStack).ifPresent(item -> {
            final Optional<AbilityContainerComponent> containerOpt = item.getComponent(AbilityContainerComponent.class);
            if (containerOpt.isEmpty()) {
                return;
            }

            final AbilityContainerComponent container = containerOpt.get();
            Optional<ItemAbility> itemAbility = switch (event.getAction()) {
                case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> container.getAbility(TriggerType.LEFT_CLICK);
                case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> container.getAbility(TriggerType.RIGHT_CLICK);
                default -> Optional.empty();
            };

            if (itemAbility.isPresent()) {
                final Client client = clientManager.search().online(event.getPlayer());
                final ItemAbility ability = itemAbility.get();
                final boolean result = ability.invoke(client, item, itemStack);

                if (result && ability.isConsumesItem()) {
                    UtilInventory.consumeHand(event.getPlayer());
                }
            }
        });
    }

    // MARK: Hold
    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        updateHeldItem(player, newItem);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        updateHeldItem(player, itemInMainHand);
    }

    @UpdateEvent
    public void onHold() {
        final Iterator<Player> iterator = heldMap.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final HoldData holdData = heldMap.get(player);
            if (holdData == null) {
                iterator.remove();
                continue;
            }

            final ItemInstance itemInstance = holdData.instance;
            final List<ItemAbility> abilities = holdData.heldAbilities;
            final Client client = clientManager.search().online(player);
            for (ItemAbility ability : abilities) {
                ability.invoke(client, itemInstance, player.getInventory().getItemInMainHand());
            }
        }
    }

    @Value
    private static class HoldData {
        ItemInstance instance;
        List<ItemAbility> heldAbilities;
    }

    // Mark: HOLD Right Click
    @EventHandler
    public void onRightClick(RightClickEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // Only handle main hand
        }

        final HoldData data = heldMap.get(event.getPlayer());
        if (data == null) {
            return; // No hold data for this player
        }

        final ItemInstance itemInstance = data.getInstance();
        final Optional<AbilityContainerComponent> containerOpt = itemInstance.getComponent(AbilityContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return; // No ability container component
        }

        final AbilityContainerComponent container = containerOpt.get();
        final Optional<ItemAbility> holdRightClick = container.getAbility(TriggerType.HOLD_RIGHT_CLICK);
        final Optional<ItemAbility> holdBlock = container.getAbility(TriggerType.HOLD_BLOCK);
        Preconditions.checkState(!(holdRightClick.isPresent() && holdBlock.isPresent()),
                "Item %s has both HOLD_RIGHT_CLICK and HOLD_BLOCK abilities, which is not allowed.", itemInstance.getView().getName());

        if (holdRightClick.isPresent()) {
            final ItemAbility ability = holdRightClick.get();
            final Client client = clientManager.search().online(event.getPlayer());
            ability.invoke(client, itemInstance, event.getPlayer().getInventory().getItemInMainHand());
        } else if (holdBlock.isPresent()) {
            final ItemAbility ability = holdBlock.get();
            final Client client = clientManager.search().online(event.getPlayer());
            event.setUseShield(true);
            event.setShieldModelData(RightClickEvent.INVISIBLE_SHIELD);
            ability.invoke(client, itemInstance, event.getPlayer().getInventory().getItemInMainHand());
        }
    }

    private void updateHeldItem(Player player, ItemStack itemStack) {
        // Remove player from tracking
        heldMap.remove(player);

        // Check if they're now holding our item
        if (itemStack == null) {
            return;
        }

        final Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(itemStack);
        if (itemOpt.isEmpty()) {
            return; // Not an item we care about
        }

        final ItemInstance item = itemOpt.get();
        final BaseItem baseItem = item.getBaseItem();

        final Optional<AbilityContainerComponent> containerOpt = baseItem.getComponent(AbilityContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return; // No ability container component
        }

        final AbilityContainerComponent container = containerOpt.get();
        final List<@NotNull ItemAbility> abilities = container.getAbilities().stream()
                .filter(ability -> ability.getTriggerType() == TriggerType.HOLD)
                .toList();

        // Add player to tracking
        heldMap.put(player, new HoldData(item, abilities));
    }
}
