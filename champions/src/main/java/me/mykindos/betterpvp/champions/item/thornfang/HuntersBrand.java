package me.mykindos.betterpvp.champions.item.thornfang;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageModifier;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class HuntersBrand extends AbstractInteraction implements Listener, DisplayedInteraction {

    private double poisonBonusDamage;
    private double resetCounterTimeoutSeconds;
    private double frenzyDurationSeconds;
    private int frenzyLevel;

    private transient final Champions champions;
    private transient final ItemFactory itemFactory;
    private transient final EffectManager effectManager;
    private transient final Thornfang thornfang;
    private transient final WeakHashMap<Player, ResetTrackingData> resetTracking = new WeakHashMap<>();

    protected HuntersBrand(Champions champions, ItemFactory itemFactory, EffectManager effectManager, Thornfang thornfang) {
        super("hunters_brand");
        this.champions = champions;
        this.itemFactory = itemFactory;
        this.effectManager = effectManager;
        this.thornfang = thornfang;
        Bukkit.getPluginManager().registerEvents(this, champions);
        UtilServer.runTaskTimer(champions, this::tick, 0, 1);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Hunter's Brand");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Land 2 consecutive Needlegrasp resets on the same target to trigger a frenzy state. Also deals bonus damage to poisoned enemies.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // Passive ability - no active trigger
        return InteractionResult.Success.ADVANCE;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMeleeDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof LivingEntity target)) return;

        // Verify player is holding Thornfang
        itemFactory.fromItemStack(damager.getEquipment().getItemInMainHand()).ifPresent(item -> {
            if (item.getBaseItem() != thornfang) return;

            // Check if target is poisoned
            if (!effectManager.hasEffect(target, EffectTypes.POISON)) return;

            // Add bonus damage
            event.addModifier(new InteractionDamageModifier.Flat(this, poisonBonusDamage));
        });
    }

    /**
     * Called by Needlegrasp when cooldown is reset after landing a melee hit during grace period.
     * Tracks consecutive resets on the same target.
     */
    public void onNeedlegraspReset(Player player, LivingEntity target) {
        final Optional<ItemInstance> heldItemOpt = itemFactory.fromItemStack(player.getEquipment().getItemInMainHand());
        if (heldItemOpt.isEmpty() || heldItemOpt.get().getBaseItem() != thornfang) return;

        UUID targetUuid = target.getUniqueId();

        ResetTrackingData data = resetTracking.computeIfAbsent(player, k -> new ResetTrackingData());

        // Check if same target as last reset
        if (data.getCurrentTargetUuid() != null && !data.getCurrentTargetUuid().equals(targetUuid)) {
            // Different target - reset counter
            data.setCurrentTargetUuid(targetUuid);
            data.setResetCount(0);
            data.setLastResetTime(System.currentTimeMillis());
            return;
        }

        // Same target or first reset - increment counter
        data.setCurrentTargetUuid(targetUuid);
        data.setResetCount(data.getResetCount() + 1);
        data.setLastResetTime(System.currentTimeMillis());

        // Check if reached threshold (2 resets)
        if (data.getResetCount() >= 2) {
            triggerFrenzy(player, target);
            // Reset counter after triggering
            data.setResetCount(0);
        }
    }

    /**
     * Triggers the special effect after 2 consecutive resets on the same target.
     * Currently uses broadcast for debugging - replace with actual effect later.
     */
    private void triggerFrenzy(Player player, LivingEntity target) {
        effectManager.addEffect(target, player, EffectTypes.FRENZY, "Hunter's Brand", frenzyLevel, (long) (frenzyDurationSeconds * 1000L));
    }

    /**
     * Periodic cleanup of stale tracking data.
     * Removes entries that have exceeded the timeout period.
     */
    private void tick() {
        long timeoutMillis = (long) (resetCounterTimeoutSeconds * 1000);
        Iterator<Map.Entry<Player, ResetTrackingData>> iterator = resetTracking.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Player, ResetTrackingData> entry = iterator.next();
            final Player player = entry.getKey();
            if (player == null || !player.isValid()) {
                iterator.remove();
                continue;
            }

            ResetTrackingData data = entry.getValue();

            // Remove if inactive for too long
            if (UtilTime.elapsed(data.getLastResetTime(), timeoutMillis)) {
                iterator.remove();
            }
        }
    }

    @Data
    private static class ResetTrackingData {
        private UUID currentTargetUuid;
        private int resetCount;
        private long lastResetTime;
    }
}
