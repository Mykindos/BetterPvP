package me.mykindos.betterpvp.core.item.component.impl.ability;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import lombok.Value;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.combat.offhand.OffhandController;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.event.PlayerItemAbilityEvent;
import me.mykindos.betterpvp.core.item.component.impl.ability.event.PlayerPreItemAbilityEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
@Singleton
public class ItemAbilityListener implements Listener {

    private final Map<UUID, HoldData> heldMap = new HashMap<>();
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final SmartBlockFactory smartBlockFactory;

    @Inject
    public ItemAbilityListener(OffhandController offhandController, ClientManager clientManager, ItemFactory itemFactory, SmartBlockFactory smartBlockFactory) {
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
        this.smartBlockFactory = smartBlockFactory;

        offhandController.setDefaultExecutor(this::onOffhandClick);
    }

    private boolean invoke(ItemAbility itemAbility, Client client, ItemInstance itemInstance, ItemStack itemStack) {
        PlayerPreItemAbilityEvent preEvent = new PlayerPreItemAbilityEvent(client, itemAbility);
        preEvent.callEvent();
        if (preEvent.isCancelled()) {
            return false;
        }

        boolean result = itemAbility.invoke(client, itemInstance, itemStack);
        if (result) {
            new PlayerItemAbilityEvent(client.getGamer().getPlayer(), itemAbility).callEvent();
        }

        return result;
    }

    private boolean onOffhandClick(@NotNull Client client, @NotNull ItemInstance itemInstance) {
        final Optional<AbilityContainerComponent> containerOpt = itemInstance.getComponent(AbilityContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return false; // No ability container component
        }

        final AbilityContainerComponent container = containerOpt.get();
        @NotNull Optional<ItemAbility> offhandAbility = container.getAbility(TriggerTypes.OFF_HAND);
        if (offhandAbility.isEmpty()) {
            return false; // No off-hand ability
        }

        final ItemAbility ability = offhandAbility.get();
        return invoke(ability, client, itemInstance, itemInstance.getItemStack());
    }

    // MARK: Left Click
    // MARK: Right Click
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final ItemStack itemStack = event.getPlayer().getEquipment().getItem(event.getHand());
        if (itemStack.getType() == Material.AIR) {
            return;
        }

        final Block block = event.getClickedBlock();
        if (block != null && (UtilBlock.isInteractable(block) || smartBlockFactory.isSmartBlock(block))) {
            return; // Prevent interaction with blocks that are interactable, so you can use them.
        }

        itemFactory.fromItemStack(itemStack).ifPresent(item -> {
            final Optional<AbilityContainerComponent> containerOpt = item.getComponent(AbilityContainerComponent.class);
            if (containerOpt.isEmpty()) {
                return;
            }

            final AbilityContainerComponent container = containerOpt.get();
            Optional<ItemAbility> itemAbility = switch (event.getAction()) {
                case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> container.getAbility(TriggerTypes.LEFT_CLICK);
                case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> container.getAbility(TriggerTypes.RIGHT_CLICK);
                default -> Optional.empty();
            };

            if (itemAbility.isPresent()) {
                final Client client = clientManager.search().online(event.getPlayer());
                final ItemAbility ability = itemAbility.get();
                final boolean result = invoke(ability, client, item, itemStack);

                if (result && ability.isConsumesItem()) {
                    UtilInventory.consumeHand(event.getPlayer());
                }
            }
        });
    }

    // MARK: Hold
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        updateHeldItem(player, newItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = event.getNewItemStack();
        if (event.getSlot() == player.getInventory().getHeldItemSlot()) {
            updateHeldItem(player, newItem);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        updateHeldItem(player, itemInMainHand);
    }

    @UpdateEvent
    public void onHold() {
        final Iterator<UUID> iterator = heldMap.keySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            final HoldData holdData = heldMap.get(uuid);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final ItemInstance itemInstance = holdData.instance;
            final List<ItemAbility> abilities = holdData.heldAbilities;
            final Client client = clientManager.search().online(player);
            for (ItemAbility ability : abilities) {
                invoke(ability, client, itemInstance, player.getInventory().getItemInMainHand());
            }
        }
    }

    @Value
    private static class HoldData {
        ItemInstance instance;
        List<ItemAbility> heldAbilities;
    }

    // Mark: HOLD Right Click
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClick(RightClickEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // Only handle main hand
        }

        final HoldData data = heldMap.get(event.getPlayer().getUniqueId());
        if (data == null) {
            return; // No hold data for this player
        }

        final ItemInstance itemInstance = data.getInstance();
        final Optional<AbilityContainerComponent> containerOpt = itemInstance.getComponent(AbilityContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return; // No ability container component
        }

        final AbilityContainerComponent container = containerOpt.get();
        final Optional<ItemAbility> holdRightClick = container.getAbility(TriggerTypes.HOLD_RIGHT_CLICK);
        final Optional<ItemAbility> holdBlock = container.getAbility(TriggerTypes.HOLD_BLOCK);
        Preconditions.checkState(!(holdRightClick.isPresent() && holdBlock.isPresent()),
                "Item %s has both HOLD_RIGHT_CLICK and HOLD_BLOCK abilities, which is not allowed.", itemInstance.getView().getName());

        if (holdRightClick.isPresent()) {
            final ItemAbility ability = holdRightClick.get();
            final Client client = clientManager.search().online(event.getPlayer());
            invoke(ability, client, itemInstance, event.getPlayer().getInventory().getItemInMainHand());
        } else if (holdBlock.isPresent()) {
            final ItemAbility ability = holdBlock.get();
            final Client client = clientManager.search().online(event.getPlayer());
            if (invoke(ability, client, itemInstance, event.getPlayer().getInventory().getItemInMainHand())) {
                event.setBlockingItem(RightClickEvent.INVISIBLE_BLOCKING_ITEM);
            }
        }
    }

    private void updateHeldItem(Player player, ItemStack itemStack) {
        // Remove player from tracking
        heldMap.remove(player.getUniqueId());

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
        final List<@NotNull ItemAbility> abilities = container.getContainer().stream()
                .filter(ability -> ability.getTriggerType() == TriggerTypes.HOLD)
                .toList();

        // Add player to tracking
        heldMap.put(player.getUniqueId(), new HoldData(item, abilities));
    }
}
