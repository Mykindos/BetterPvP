package me.mykindos.betterpvp.core.item.impl.ability;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEndEvent;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@SuppressWarnings("ALL")
@CustomLog
@EqualsAndHashCode(callSuper = true)
@PluginAdapter("PacketEvents")
@Getter
@Setter
@Singleton
public class EndlessQuiverAbility extends ItemAbility implements Listener, PacketListener {

    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;
    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.NONE)
    private final Map<Player, Integer> activeTicks = new MapMaker().weakKeys().makeMap();
    @EqualsAndHashCode.Exclude
    private static Core core;
    private Consumer<LivingEntity> useFunction;
    private Predicate<LivingEntity> useCheck;

    @Inject
    private EndlessQuiverAbility(Core plugin, ItemFactory itemFactory) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Core.class), "endless_quiver"),
                "Endless Quiver",
                "Automatically conjures arrows, letting any quiver fire endlessly without requiring ammunition.",
                TriggerTypes.PASSIVE);
        this.itemFactory = itemFactory;
        this.core = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.HIGHEST);
        UtilServer.runTaskTimer(plugin, this::onTick, 0, 1);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        // This is a passive ability and doesn't need active invocation
        return true;
    }

    private void givePacketArrow(Player player) {
        final com.github.retrooper.packetevents.protocol.item.ItemStack item =
                com.github.retrooper.packetevents.protocol.item.ItemStack.builder().type(ItemTypes.ARROW).build();
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(0, 0, 35, item);
        PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(packet);
    }

    private void takePacketArrow(Player player) {
        final ItemStack bukkitStack = player.getInventory().getItem(35);
        final com.github.retrooper.packetevents.protocol.item.ItemStack item;
        if (bukkitStack == null) {
            item = com.github.retrooper.packetevents.protocol.item.ItemStack.builder().type(ItemTypes.AIR).build();
        } else {
            item = SpigotConversionUtil.fromBukkitItemStack(bukkitStack);
        }
        final WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(35, item);
        PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(packet);
    }

    private boolean isAmmo(ItemStack itemStack) {
        return itemStack.getType() == Material.ARROW || itemStack.getType() == Material.FIREWORK_ROCKET;
    }

    private void showArrows(Player player) {
         for (int i = 0; i < player.getInventory().getContents().length; i++) {
            ItemStack content = player.getInventory().getContents()[i];
            if (content == null || !isAmmo(content)) continue;

            final WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(i, SpigotConversionUtil.fromBukkitItemStack(content));
            PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(packet);
        }
    }

    private void hideArrows(Player player) {
        for (int i = 0; i < player.getInventory().getContents().length; i++) {
            ItemStack content = player.getInventory().getContents()[i];
            if (content == null || !isAmmo(content)) continue;

            ItemStack mock = new ItemStack(Material.PAPER);
            if (content.hasData(DataComponentTypes.ITEM_MODEL)) {
                mock.setData(DataComponentTypes.ITEM_MODEL, content.getData(DataComponentTypes.ITEM_MODEL));
            }
            if (content.hasData(DataComponentTypes.CUSTOM_MODEL_DATA)) {
                mock.setData(DataComponentTypes.CUSTOM_MODEL_DATA, content.getData(DataComponentTypes.CUSTOM_MODEL_DATA));
            }
            mock.setAmount(content.getAmount());
            final WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(i, SpigotConversionUtil.fromBukkitItemStack(mock));
            PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(packet);
        }
    }

    private void onTick() {
        final Iterator<Player> iterator = activeTicks.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final int ticks = activeTicks.get(player);
            if (player == null || !player.isValid() || player.getActiveItemRemainingTime() <= 0) {
                if (player != null && player.isValid()) {
                    takePacketArrow(player);
                    player.clearActiveItem();
                }

                iterator.remove();
                continue;
            }

            player.setActiveItemRemainingTime(ticks);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onItemSwap(PlayerInventorySlotChangeEvent event) {
        if (event.getRawSlot() != event.getPlayer().getInventory().getHeldItemSlot()) {
            return; // Not modifying the held item slot
        }

        // If it has endless quiver and the old one doesn't, take their arrows
        // If it doesn't have endless quiver and the new one does, refund arrows
        final ItemInstance oldItem = itemFactory.fromItemStack(event.getOldItemStack()).orElseThrow();
        final ItemInstance newItem = itemFactory.fromItemStack(event.getNewItemStack()).orElseThrow();

        final Optional<AbilityContainerComponent> oldContainerOpt = oldItem.getComponent(AbilityContainerComponent.class);
        boolean oldHasEndlessQuiver = oldContainerOpt.isPresent() && oldContainerOpt.get().getContainer().contains(this);

        final Optional<AbilityContainerComponent> newContainerOpt = newItem.getComponent(AbilityContainerComponent.class);
        boolean newHasEndlessQuiver = newContainerOpt.isPresent() && newContainerOpt.get().getContainer().contains(this);

        if (!oldHasEndlessQuiver && newHasEndlessQuiver) {
            takePacketArrow(event.getPlayer());
            hideArrows(event.getPlayer());
            activeTicks.remove(event.getPlayer());
            event.getPlayer().clearActiveItem();
            return;
        }

        if (oldHasEndlessQuiver && !newHasEndlessQuiver) {
            showArrows(event.getPlayer());
            takePacketArrow(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onHeldItem(PlayerItemHeldEvent event) {
        // The new item has endless quiver
        // Take their arrows
        final ItemStack newItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (newItem != null) {
            final ItemInstance itemInstance = itemFactory.fromItemStack(newItem).orElseThrow();
            final Optional<AbilityContainerComponent> containerOpt = itemInstance.getComponent(AbilityContainerComponent.class);
            if (containerOpt.isPresent()) {
                final AbilityContainerComponent container = containerOpt.get();
                if (container.getContainer().contains(this)) {
                    takePacketArrow(event.getPlayer());
                    hideArrows(event.getPlayer());
                    activeTicks.remove(event.getPlayer());
                    event.getPlayer().clearActiveItem();
                    event.getPlayer().completeUsingActiveItem();
                    return;
                }
            }
        }

        // The new item doesn't have endless quiver
        // If the old one does, refund arrows
        final ItemStack oldItem = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
        if (oldItem != null) {
            final ItemInstance itemInstance = itemFactory.fromItemStack(oldItem).orElseThrow();
            final Optional<AbilityContainerComponent> containerOpt = itemInstance.getComponent(AbilityContainerComponent.class);
            if (containerOpt.isPresent()) {
                final AbilityContainerComponent container = containerOpt.get();
                if (container.getContainer().contains(this)) {
                    showArrows(event.getPlayer());
                    takePacketArrow(event.getPlayer());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onClick(RightClickEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        final Player player = event.getPlayer();
        if (activeTicks.containsKey(player)) {
            final int ticks = activeTicks.get(player);
            activeTicks.put(player, ticks - 1);

            if (player.getActiveItem().getType() == Material.CROSSBOW) {
                loadCrossbow(player.getActiveItem(), player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onClickEnd(RightClickEndEvent event) {
        if (activeTicks.containsKey(event.getPlayer())) {
            takePacketArrow(event.getPlayer());
            activeTicks.remove(event.getPlayer());
            event.getPlayer().clearActiveItem();
        }
    }

    // Disables picking up arrows
    // And calls useFunction
    @EventHandler(priority = EventPriority.LOWEST)
    void onShoot(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        // if it's a endless quiver bow, make the arrow not pickupable
        final ItemStack item = event.getBow();
        final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(Objects.requireNonNull(item));
        if (instanceOpt.isEmpty()) return;

        final ItemInstance instance = instanceOpt.get();
        final Optional<AbilityContainerComponent> containerOpt = instance.getComponent(AbilityContainerComponent.class);
        if (containerOpt.isEmpty()) return;

        final AbilityContainerComponent container = containerOpt.get();
        if (container.getContainer().contains(this)) {
            arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
            if (useFunction != null) {
                useFunction.accept(event.getEntity());
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        switch (event.getPacketType()) {
            case PacketType.Play.Client.PLAYER_DIGGING -> release(event);
            case PacketType.Play.Client.USE_ITEM -> press(event);
            default -> { }
        }
    }

    private void press(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
        final ItemStack activeItem = player.getEquipment().getItem(EquipmentSlot.values()[packet.getHand().ordinal()]);
        final EquipmentSlot activeHand = EquipmentSlot.HAND;
        final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(activeItem);
        if (instanceOpt.isEmpty()) {
            return;
        }

        final ItemInstance activeInstance = instanceOpt.get();
        final Optional<AbilityContainerComponent> containerOpt = activeInstance.getComponent(AbilityContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return;
        }

        final AbilityContainerComponent container = containerOpt.get();
        if (!container.getContainer().contains(this)) {
            return;
        }

        if (useCheck != null && !useCheck.test(player)) {
            // Can't use
            activeTicks.remove(player);
            event.setCancelled(true);
            player.clearActiveItem();
            hideArrows(player);
            takePacketArrow(player);
        } else {
            // Start using
            if (activeItem.hasData(DataComponentTypes.CHARGED_PROJECTILES)) {
                final io.papermc.paper.datacomponent.item.@Nullable ChargedProjectiles charged = activeItem.getData(DataComponentTypes.CHARGED_PROJECTILES);
                if (!charged.projectiles().isEmpty()) {
                    activeTicks.remove(player);
                    event.setCancelled(true);
                    player.clearActiveItem();
                    hideArrows(player);
                    takePacketArrow(player);
                    return;
                }
            }

            if (useCheck != null && !useCheck.test(player)) {
                activeTicks.remove(player);
                event.setCancelled(true);
                player.clearActiveItem();
                hideArrows(player);
                takePacketArrow(player);
                return;
            }

            activeTicks.put(player, activeItem.getMaxItemUseDuration(player));
            player.startUsingItem(activeHand);
            player.setActiveItemRemainingTime(activeItem.getMaxItemUseDuration(player));
            hideArrows(player);
            givePacketArrow(player);
        }
        return;
    }

    private void release(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        if (packet.getAction() != DiggingAction.RELEASE_USE_ITEM) {
            return; // They didnt shoow their bow/charge their crossbow
        }

        // These are the necessary conditions for this packet to be sent as a
        // bow shot or crossbow charge as per
        // https://minecraft.wiki/w/Java_Edition_protocol/Packets#Player_Action
        final Vector3i position = packet.getBlockPosition();
        if (!(position.x == 0 && position.y == 0 && position.z == 0
                && packet.getBlockFace() == BlockFace.DOWN
                && packet.getSequence() == 0)) {
            return; // not a bowshot
        }

        // If it is, then check if they have endless quiver. If they do, shoot their bow for them and cancel the packet
        final ItemStack activeItem = player.getEquipment().getItemInMainHand();
        final EquipmentSlot activeHand = EquipmentSlot.HAND;
        final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(activeItem);
        if (instanceOpt.isEmpty()) {
            return;
        }

        final ItemInstance activeInstance = instanceOpt.get();
        final Optional<AbilityContainerComponent> containerOpt = activeInstance.getComponent(AbilityContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return;
        }

        final AbilityContainerComponent container = containerOpt.get();
        if (!container.getContainer().contains(this)) {
            return; // doesnt have endless quiver
        }

        if (useCheck != null && !useCheck.test(player)) {
            activeTicks.remove(player);
            takePacketArrow(player);
            hideArrows(player);
            player.clearActiveItem();
            return;
        }

        int usedTime = player.getActiveItemUsedTime();
        // this fixes a rollover caused by a single tap to the bow
        if (activeTicks.containsKey(player)) {
            activeTicks.remove(player);
            takePacketArrow(player);
            player.clearActiveItem();
        }

        // It does have endless quiver, cancel this packet and automatically charge/send
        final ItemStack itemStack = activeInstance.getItemStack();
        switch (itemStack.getType()) {
            case BOW:
                shootBow(itemStack, player, activeHand, usedTime);
                event.setCancelled(true);
                break;
            case CROSSBOW:
                loadCrossbow(itemStack, player);
                event.setCancelled(true);
                break;
            default:
                throw new IllegalStateException("Endless Quiver was triggered on type " + activeItem.getType());
        }
    }

    private static void shootBow(ItemStack itemStack, Player player, EquipmentSlot activeHand, int usedTime) {
        final net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.unwrap(itemStack);
        final Item nmsItem = nmsStack.getItem();
        Preconditions.checkState(nmsItem instanceof BowItem, "Expected BowItem, got %s", nmsItem.getClass());
        final BowItem bowItem = (BowItem) nmsItem;

        final ServerPlayer playerHandle = ((CraftPlayer) player).getHandle();
        final ServerLevel serverLevel = ((CraftWorld) player.getWorld()).getHandle();
        final InteractionHand hand = activeHand == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        final float powerForTime = BowItem.getPowerForTime(usedTime);

        try {
            final Method method = ProjectileWeaponItem.class.getDeclaredMethod("shoot", // shoot
                    ServerLevel.class,
                    net.minecraft.world.entity.LivingEntity.class,
                    InteractionHand.class,
                    net.minecraft.world.item.ItemStack.class,
                    List.class,
                    float.class,
                    float.class,
                    boolean.class,
                    net.minecraft.world.entity.LivingEntity.class,
                    float.class
            );

            method.setAccessible(true);
            UtilServer.runTask(core, () -> {
                try {
                    method.invoke(bowItem, serverLevel,
                            playerHandle,
                            hand,
                            nmsStack,
                            List.of(new net.minecraft.world.item.ItemStack(Items.ARROW)),
                            powerForTime * 3.0f,
                            1.0F,
                            powerForTime == 1.0f,
                            null,
                            powerForTime);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error("Failed to invoke shoot method reflectively, Endless Quiver did not work correctly.", e).submit();
                }
            });
        } catch (NoSuchMethodException e) {
            log.error("Failed to reflectively find shoot method with extra power parameter, this may be due to a version change. Endless Quiver did not work correctly.", e).submit();
        }
    }

    private static boolean loadCrossbow(ItemStack itemStack, Player player) {
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.unwrap(itemStack);
        Item nmsItem = nmsStack.getItem();
        Preconditions.checkState(nmsItem instanceof CrossbowItem, "Expected CrossbowItem, got %s", nmsItem.getClass());

        final float powerForTime = BowItem.getPowerForTime(player.getActiveItemUsedTime());
        if (powerForTime >= 1f) {
            UtilServer.runTask(core, () -> {
                nmsStack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(new net.minecraft.world.item.ItemStack(Items.ARROW)));
            });
            return true;
        }
        return false;
    }
}