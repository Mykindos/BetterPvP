package me.mykindos.betterpvp.progression.profession.mining.item.interaction;

import lombok.Setter;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.blockbreak.event.ScriptedBlockPlaceEvent;
import me.mykindos.betterpvp.core.framework.blockbreak.global.GlobalBlockBreakRules;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.preset.BlockGroups;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VeinEchoInteraction extends AbstractInteraction implements DisplayedInteraction {

    private static final Random RANDOM = new Random();

    private final Map<UUID, EchoState> states = new ConcurrentHashMap<>();
    private final BPvPPlugin plugin;
    private final GlobalBlockBreakRules globalRules;

    @Setter private double durationSeconds;
    @Setter private double oreRespawnChance;
    @Setter private int oreRespawnBonusStacks;
    @Setter private int maxStacks;
    /** Framework-scaled speed bonus added per active stack to the player's STONES global rule. */
    @Setter private int speedPerStack;

    public VeinEchoInteraction(BPvPPlugin plugin,
                               GlobalBlockBreakRules globalRules,
                               double durationSeconds,
                               double oreRespawnChance,
                               int oreRespawnBonusStacks,
                               int maxStacks,
                               int speedPerStack) {
        super("Vein Echo");
        this.plugin = plugin;
        this.globalRules = globalRules;
        this.durationSeconds = durationSeconds;
        this.oreRespawnChance = oreRespawnChance;
        this.oreRespawnBonusStacks = oreRespawnBonusStacks;
        this.maxStacks = maxStacks;
        this.speedPerStack = speedPerStack;

        // small pulse — only sweeps the (small) states map and removes expired entries.
        UtilServer.runTaskTimer(plugin, this::pulseExpiry, 0L, 5L);
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor,
                                                    @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance,
                                                    @Nullable ItemStack itemStack) {
        if (!actor.isPlayer()) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        Player player = (Player) actor.getEntity();
        Block broken = context.get(InputMeta.BROKEN_BLOCK).orElse(null);
        if (broken == null || !UtilBlock.isStoneBased(broken)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }
        if (itemStack == null) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        // Capture full block data *now* — the block is about to become AIR.
        final BlockData originalData = broken.getBlockData();
        final boolean isOre = UtilBlock.isOre(originalData.getMaterial());

        int gained = 1;
        if (isOre && RANDOM.nextDouble() < oreRespawnChance) {
            scheduleRespawn(player, broken, originalData);
            gained += oreRespawnBonusStacks;
        }

        final UUID playerId = player.getUniqueId();
        EchoState state = states.computeIfAbsent(playerId, id -> new EchoState());
        long now = System.currentTimeMillis();
        if (state.expiresAtMs < now) {
            state.stacks = 0;
        }
        state.stacks = Math.min(maxStacks, state.stacks + gained);
        state.expiresAtMs = now + (long) (durationSeconds * 1000);
        state.heldStack = itemStack;

        applyBuff(player, state);

        if (gained > 1) {
            Particle.END_ROD.builder()
                    .count(10)
                    .offset(0.6, 0.6, 0.6)
                    .extra(0.02)
                    .location(broken.getLocation().toCenterLocation())
                    .receivers(40)
                    .spawn();
            new SoundEffect(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.2f).play(broken.getLocation());
            new SoundEffect(Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1.4f, 1.2f).play(broken.getLocation());
            new SoundEffect(Sound.ITEM_SHIELD_BREAK, 0.4f, 0.4f).play(broken.getLocation());
        }

        return InteractionResult.Success.ADVANCE;
    }

    private void scheduleRespawn(Player player, Block block, BlockData originalData) {
        UtilServer.runTaskLater(plugin, () -> {
            if (block.getType() != Material.AIR) {}

            final ScriptedBlockPlaceEvent event = new ScriptedBlockPlaceEvent(
                    player,
                    block,
                    block.getBlockData(),
                    originalData,
                    "progression:deep_resonator/vein_echo");
            UtilServer.callEvent(event);
            if (event.isCancelled()) return;

            block.setBlockData(event.getReplacementData());
            UtilBlock.playBlockEffect(block, originalData);
        }, 1L);
    }

    /**
     * Swaps the player's active Vein Echo speed rule for one matching the new stack count.
     * <p>
     * The rule's matcher is {@link BlockGroups#STONES} — the same matcher Deep Resonator's
     * tool rule uses — so {@code BlockBreakProperties.merge} adds the bonus on top of the
     * tool's base 180 speed for stones, leaving non-stone breaking unaffected.
     * <p>
     * We always remove the previous rule before adding the new one. The global-rule registry
     * rejects overlapping matchers, so we can't just "update" — remove-then-add is the
     * supported pattern, and is cheap (single ArrayList op per side).
     */
    private void applyBuff(Player player, EchoState state) {
        final UUID playerId = player.getUniqueId();
        if (state.activeRule != null) {
            globalRules.removeRule(playerId, state.activeRule);
            state.activeRule = null;
        }

        final int bonus = Math.max(BlockBreakProperties.MIN_SPEED, state.stacks * speedPerStack);
        final BlockBreakRule rule = BlockBreakRule.of(
                BlockGroups.STONES,
                BlockBreakProperties.breakable(bonus),
                p -> {
                    final ItemStack held = p.getInventory().getItemInMainHand();
                    return state.heldStack != null && state.heldStack.equals(held);
                });
        globalRules.addRule(playerId, rule);
        state.activeRule = rule;
    }

    /**
     * Iterates {@link #states} once every half-second. Entries past their expiry have their
     * global rule removed and their state dropped. Player quits are already handled by
     * {@code GlobalBlockBreakRulesImpl#onQuit} clearing all rules — so we don't need to
     * track quit events here, only timeouts.
     */
    private void pulseExpiry() {
        if (states.isEmpty()) return;
        final long now = System.currentTimeMillis();
        final Iterator<Map.Entry<UUID, EchoState>> it = states.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<UUID, EchoState> entry = it.next();
            final EchoState state = entry.getValue();
            if (state.expiresAtMs >= now) continue;
            if (state.activeRule != null) {
                globalRules.removeRule(entry.getKey(), state.activeRule);
            }
            it.remove();
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Vein Echo");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Breaking stone grants stacking mining speed. Ores may resonate, respawning instantly and granting bonus stacks.");
    }

    private static final class EchoState {
        int stacks;
        long expiresAtMs;
        BlockBreakRule activeRule;
        ItemStack heldStack;
    }
}
