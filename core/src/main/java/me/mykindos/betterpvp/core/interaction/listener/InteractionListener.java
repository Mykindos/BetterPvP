package me.mykindos.betterpvp.core.interaction.listener;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.context.InteractionItemContext;
import me.mykindos.betterpvp.core.interaction.executor.InteractionExecutor;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.interaction.state.InteractionStateManager;
import me.mykindos.betterpvp.core.interaction.tracker.ActiveInteractionTracker;
import me.mykindos.betterpvp.core.interaction.tracker.HoldTracker;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Listener that captures inputs and routes them to the interaction system.
 * Delegates execution logic to {@link InteractionExecutor},
 * running interactions to {@link ActiveInteractionTracker},
 * and hold management to {@link HoldTracker}.
 */
@CustomLog
@BPvPListener
@Singleton
public class InteractionListener implements Listener, PacketListener {

    private static final long ATTACK_COOLDOWN_MS = 50; // Rate limit attack packets (~20/sec max)

    private final ItemFactory itemFactory;
    private final SmartBlockFactory smartBlockFactory;
    private final InteractionStateManager stateManager;
    private final InteractionExecutor executor;
    private final ActiveInteractionTracker activeInteractionTracker;
    private final HoldTracker holdTracker;
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();

    @Inject
    private InteractionListener(ItemFactory itemFactory, SmartBlockFactory smartBlockFactory,
                                InteractionStateManager stateManager, InteractionExecutor executor,
                                ActiveInteractionTracker activeInteractionTracker, HoldTracker holdTracker) {
        this.itemFactory = itemFactory;
        this.smartBlockFactory = smartBlockFactory;
        this.stateManager = stateManager;
        this.executor = executor;
        this.activeInteractionTracker = activeInteractionTracker;
        this.holdTracker = holdTracker;
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            onLeftClickEntity(event);
        }
    }

    // MARK: Click Handlers

    /**
     * Handle left-click on entity via packet.
     * We do this here and not DamageEvent because we want it to stop happening altogether.
     * Additionally, if the resulting execution ALSO damages using our own damage system
     * then listening to this in a DamageEvent would cause a stack overflow.
     */
    public void onLeftClickEntity(PacketReceiveEvent event) {
        final Player damager = event.getPlayer();
        final WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return; // only care for attacks
        }

        // Rate limit attack packets to prevent spam
        long now = System.currentTimeMillis();
        Long lastAttack = lastAttackTime.get(damager.getUniqueId());
        if (lastAttack != null && (now - lastAttack) < ATTACK_COOLDOWN_MS) {
            return;
        }
        lastAttackTime.put(damager.getUniqueId(), now);

        final Entity damagee = SpigotConversionUtil.getEntityById(damager.getWorld(), packet.getEntityId());
        if (damagee == null) {
            return; // invalid packet, will be handled by PlayerInteractEvent
        }

        ItemStack itemStack = damager.getEquipment().getItemInMainHand();
        Optional<InteractionItemContext> contextOpt = InteractionItemContext.from(itemStack, itemFactory);
        if (contextOpt.isEmpty()) {
            return;
        }

        InteractionItemContext ctx = contextOpt.get();
        InteractionInput input = damager.isSneaking() ? InteractionInputs.SHIFT_LEFT_CLICK : InteractionInputs.LEFT_CLICK;

        // Set up execution data callback for TARGET
        Consumer<InteractionContext> dataSetup = damagee instanceof LivingEntity living
                ? c -> c.set(InputMeta.TARGET, living)
                : null;

        // We have to stall this thread to wait for this processInput
        // It doesn't matter if we stall, because we otherwise would be running
        // it on this thread anyway, which would be blocking too.
        CompletableFuture.runAsync(() -> {
            if (processInput(damager, input, ctx.itemInstance(), ctx.itemStack(), dataSetup)) {
                event.setCancelled(true);
            }
        }, Bukkit.getScheduler().getMainThreadExecutor(JavaPlugin.getPlugin(Core.class))).join();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack itemStack = event.getPlayer().getEquipment().getItem(event.getHand());
        if (itemStack.getType().isAir()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block != null && (UtilBlock.isInteractable(block) || smartBlockFactory.isSmartBlock(block))) {
            return;
        }

        Optional<InteractionItemContext> contextOpt = InteractionItemContext.from(itemStack, itemFactory);
        if (contextOpt.isEmpty()) {
            return;
        }

        InteractionItemContext ctx = contextOpt.get();
        Player player = event.getPlayer();

        InteractionInput input = switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK ->
                    player.isSneaking() ? InteractionInputs.SHIFT_LEFT_CLICK : InteractionInputs.LEFT_CLICK;
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK ->
                    player.isSneaking() ? InteractionInputs.SHIFT_RIGHT_CLICK : InteractionInputs.RIGHT_CLICK;
            default -> null;
        };

        if (input != null && processInput(player, input, ctx.itemInstance(), ctx.itemStack(), null)) {
            event.setCancelled(true);
        }
    }

    // MARK: Hold Handlers (delegated to HoldTracker)

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeld(EntityEquipmentChangedEvent event) {
        if (!event.getEquipmentChanges().containsKey(EquipmentSlot.HAND)) {
            return;
        }

        final LivingEntity entity = event.getEntity();
        ItemStack newItem = event.getEquipmentChanges().get(EquipmentSlot.HAND).newItem();
        holdTracker.updateHeldItem(entity, newItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = event.getNewItemStack();
        if (event.getSlot() == player.getInventory().getHeldItemSlot()) {
            holdTracker.updateHeldItem(player, newItem);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        holdTracker.updateHeldItem(player, itemInMainHand);
    }

    // MARK: Passive Trigger Handlers

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDealt(DamageEvent event) {
        final LivingEntity damager = event.getDamager();
        if (damager == null) {
            return;
        }

        Optional<InteractionItemContext> contextOpt = InteractionItemContext.fromMainHand(damager, itemFactory);
        if (contextOpt.isEmpty()) {
            return;
        }

        InteractionItemContext ctx = contextOpt.get();

        // Set up execution data callback for damage info
        Consumer<InteractionContext> dataSetup = c -> {
            c.set(InputMeta.DAMAGE_EVENT, event);
            c.set(InputMeta.DAMAGE_AMOUNT, event.getDamage());
            c.set(InputMeta.FINAL_DAMAGE, event.getModifiedDamage());
            c.set(InputMeta.DAMAGER, damager);
            if (event.isDamageeLiving()) {
                c.set(InputMeta.TARGET, Objects.requireNonNull(event.getLivingDamagee()));
            }
        };

        processInput(damager, InteractionInputs.DAMAGE_DEALT, ctx.itemInstance(), ctx.itemStack(), dataSetup);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageTaken(DamageEvent event) {
        if (!event.isDamageeLiving()) {
            return;
        }

        final LivingEntity damagee = event.getLivingDamagee();
        Optional<InteractionItemContext> contextOpt = InteractionItemContext.fromMainHand(damagee, itemFactory);
        if (contextOpt.isEmpty()) {
            return;
        }

        InteractionItemContext ctx = contextOpt.get();

        // Set up execution data callback for damage taken info
        Consumer<InteractionContext> dataSetup = c -> {
            c.set(InputMeta.DAMAGE_EVENT, event);
            c.set(InputMeta.DAMAGE_AMOUNT, event.getDamage());
            c.set(InputMeta.FINAL_DAMAGE, event.getModifiedDamage());
            c.set(InputMeta.TARGET, damagee);
            if (event.getDamager() instanceof LivingEntity damager) {
                c.set(InputMeta.DAMAGER, damager);
            }
        };

        processInput(damagee, InteractionInputs.DAMAGE_TAKEN, ctx.itemInstance(), ctx.itemStack(), dataSetup);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        Optional<InteractionItemContext> contextOpt = InteractionItemContext.fromMainHand(killer, itemFactory);
        if (contextOpt.isEmpty()) {
            return;
        }

        InteractionItemContext ctx = contextOpt.get();

        // Set up execution data callback for kill info
        Consumer<InteractionContext> dataSetup = c -> {
            c.set(InputMeta.KILLED_ENTITY, event.getEntity());
            c.set(InputMeta.KILLER, killer);
        };

        processInput(killer, InteractionInputs.KILL, ctx.itemInstance(), ctx.itemStack(), dataSetup);
    }

    @EventHandler
    public void onGamerJoin(ClientJoinEvent event) {
        event.getClient().getGamer().setOffhandExecutor(this::onSwapHand);
    }

    public boolean onSwapHand(@NotNull Client client, @NotNull ItemInstance itemInstance) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
            return false;
        }

        Optional<InteractionItemContext> contextOpt = InteractionItemContext.from(itemStack, itemFactory);
        if (contextOpt.isEmpty()) {
            return false;
        }

        InteractionItemContext ctx = contextOpt.get();
        return processInput(player, InteractionInputs.SWAP_HAND, ctx.itemInstance(), ctx.itemStack(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        Optional<InteractionItemContext> contextOpt = InteractionItemContext.fromMainHand(player, itemFactory);
        if (contextOpt.isEmpty()) {
            return;
        }

        InteractionItemContext ctx = contextOpt.get();
        InteractionInput input = event.isSneaking() ? InteractionInputs.SNEAK_START : InteractionInputs.SNEAK_END;

        // Set up execution data callback for sneak state
        Consumer<InteractionContext> dataSetup = c -> c.set(InputMeta.IS_SNEAKING, event.isSneaking());

        processInput(player, input, ctx.itemInstance(), ctx.itemStack(), dataSetup);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityJump(EntityJumpEvent event) {
        final LivingEntity entity = event.getEntity();
        Optional<InteractionItemContext> contextOpt = InteractionItemContext.fromMainHand(entity, itemFactory);
        if (contextOpt.isEmpty()) {
            return;
        }

        InteractionItemContext ctx = contextOpt.get();
        processInput(entity, InteractionInputs.JUMP, ctx.itemInstance(), ctx.itemStack(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJump(PlayerJumpEvent event) {
        final Player player = event.getPlayer();
        Optional<InteractionItemContext> contextOpt = InteractionItemContext.fromMainHand(player, itemFactory);
        if (contextOpt.isEmpty()) {
            return;
        }

        InteractionItemContext ctx = contextOpt.get();
        processInput(player, InteractionInputs.JUMP, ctx.itemInstance(), ctx.itemStack(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }
        if (!(trident.getShooter() instanceof Player player)) {
            return;
        }

        ItemStack itemStack = trident.getItemStack();
        Optional<InteractionItemContext> contextOpt = InteractionItemContext.from(itemStack, itemFactory);
        if (contextOpt.isEmpty()) {
            return;
        }

        InteractionItemContext ctx = contextOpt.get();
        processInput(player, InteractionInputs.THROW, ctx.itemInstance(), ctx.itemStack(), null);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        holdTracker.removeActor(playerId);
        stateManager.removeActor(playerId);
        activeInteractionTracker.removeActor(playerId);
        lastAttackTime.remove(playerId);
    }

    /**
     * Process an input, delegating to InteractionExecutor.
     */
    private boolean processInput(@NotNull LivingEntity entity, @NotNull InteractionInput input,
                                 @NotNull ItemInstance itemInstance, @NotNull ItemStack itemStack,
                                 Consumer<InteractionContext> executionDataSetup) {
        return executor.processInput(entity, input, itemInstance, itemStack,
                activeInteractionTracker.getActiveInteractions(), executionDataSetup);
    }
}
