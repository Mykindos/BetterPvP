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
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEndEvent;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInputEvent;
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

@SuppressWarnings("ALL")
@CustomLog
@EqualsAndHashCode(callSuper = true)
@PluginAdapter("ProtocolLib")
@BPvPListener
@Singleton
public class EndlessQuiverAbility extends ItemAbility implements Listener, PacketListener {

    private final ItemFactory itemFactory;
    private final Map<Player, Integer> activeTicks = new MapMaker().weakKeys().makeMap();

    @Inject
    private EndlessQuiverAbility(ItemFactory itemFactory) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Core.class), "endless_quiver"),
                "Endless Quiver",
                "Automatically conjures arrows, letting any quiver fire endlessly without requiring ammunition.",
                TriggerTypes.PASSIVE);
        this.itemFactory = itemFactory;
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.HIGHEST);
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

    @UpdateEvent
    public void onTick() {
        final Iterator<Player> iterator = activeTicks.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final int ticks = activeTicks.get(player);
            if (player == null || !player.isValid()) {
                if (player != null && player.isValid()) {
                    takePacketArrow(player);
                    player.clearActiveItem();
                }

                iterator.remove();
                continue;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onItemSwap(PlayerInventorySlotChangeEvent event) {
        if (event.getRawSlot() == event.getPlayer().getInventory().getHeldItemSlot()) {
            takePacketArrow(event.getPlayer());
            activeTicks.remove(event.getPlayer());
            event.getPlayer().clearActiveItem();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onHeldItem(PlayerItemHeldEvent event) {
        takePacketArrow(event.getPlayer());
        activeTicks.remove(event.getPlayer());
        event.getPlayer().clearActiveItem();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onClick(RightClickEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        final Player player = event.getPlayer();
        if (!event.isHoldClick()) {
            final ItemStack item = player.getEquipment().getItem(event.getHand());
            final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(Objects.requireNonNull(item));
            if (instanceOpt.isEmpty()) return;

            if (item.hasData(DataComponentTypes.CHARGED_PROJECTILES)) {
                final io.papermc.paper.datacomponent.item.@Nullable ChargedProjectiles charged = item.getData(DataComponentTypes.CHARGED_PROJECTILES);
                if (!charged.projectiles().isEmpty()) return;
            }

            final ItemInstance instance = instanceOpt.get();
            final Optional<AbilityContainerComponent> containerOpt = instance.getComponent(AbilityContainerComponent.class);
            if (containerOpt.isEmpty()) return;

            final AbilityContainerComponent container = containerOpt.get();
            if (container.getAbilities().contains(this)) {
                activeTicks.put(player, item.getMaxItemUseDuration(player));
                givePacketArrow(player);
                player.startUsingItem(event.getHand());
            }
        } else if (activeTicks.containsKey(player)) {
            final int ticks = activeTicks.getOrDefault(player, 0);
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
        if (container.getAbilities().contains(this)) {
            arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) {
            return;
        }

        WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        if (packet.getAction() != DiggingAction.RELEASE_USE_ITEM) {
            return; // They didnt shoow their bow/charge their crossbow
        }

        Player player = event.getPlayer();
        if (activeTicks.containsKey(player)) {
            activeTicks.remove(player);
            takePacketArrow(player);
            player.clearActiveItem();
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
        if (!container.getAbilities().contains(this)) {
            return; // doesnt have endless quiver
        }

        // It does have endless quiver, cancel this packet and automatically charge/send
        final ItemStack itemStack = activeInstance.getItemStack();
        switch (itemStack.getType()) {
            case BOW:
                shootBow(itemStack, player, activeHand);
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

    private static void shootBow(ItemStack itemStack, Player player, EquipmentSlot activeHand) {
        final net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.unwrap(itemStack);
        final Item nmsItem = nmsStack.getItem();
        Preconditions.checkState(nmsItem instanceof BowItem, "Expected BowItem, got %s", nmsItem.getClass());
        final BowItem bowItem = (BowItem) nmsItem;

        final ServerPlayer playerHandle = ((CraftPlayer) player).getHandle();
        final ServerLevel serverLevel = ((CraftWorld) player.getWorld()).getHandle();
        final InteractionHand hand = activeHand == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        final float powerForTime = BowItem.getPowerForTime(player.getActiveItemUsedTime());

        try {
            final Method method = BowItem.class.getMethod("a", // shoot
                    ServerLevel.class,
                    ServerPlayer.class,
                    InteractionHand.class,
                    net.minecraft.world.item.ItemStack.class,
                    List.class,
                    Float.class,
                    Float.class,
                    Boolean.class,
                    LivingEntity.class,
                    Float.class);

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
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to reflectively find shoot method with extra power parameter, this may be due to a version change. Endless Quiver did not work correctly.", e);
        }
    }

    private static boolean loadCrossbow(ItemStack itemStack, Player player) {
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.unwrap(itemStack);
        Item nmsItem = nmsStack.getItem();
        Preconditions.checkState(nmsItem instanceof CrossbowItem, "Expected CrossbowItem, got %s", nmsItem.getClass());

        final float powerForTime = BowItem.getPowerForTime(player.getActiveItemUsedTime());
        if (powerForTime >= 1f) {
            nmsStack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(new net.minecraft.world.item.ItemStack(Items.ARROW)));
            return true;
        }
        return false;
    }
}