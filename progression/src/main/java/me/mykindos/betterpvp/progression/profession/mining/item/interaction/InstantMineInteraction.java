package me.mykindos.betterpvp.progression.profession.mining.item.interaction;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockMatcher;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.progression.Progression;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class InstantMineInteraction extends CooldownInteraction implements DisplayedInteraction {

    private record ActiveSession(long expiry, BaseItem sourceItem) {}

    private final Cache<UUID, ActiveSession> activePlayers = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Setter private double cooldown;
    @Setter private double duration;
    @Setter private BlockMatcher targetBlocks;
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final Progression plugin;

    public InstantMineInteraction(CooldownManager cooldownManager, ClientManager clientManager,
                                   ItemFactory itemFactory, double cooldown, double duration,
                                   BlockMatcher targetBlocks) {
        super("Instant Mine", cooldownManager);
        this.cooldown = cooldown;
        this.duration = duration;
        this.targetBlocks = targetBlocks;
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
        this.plugin = JavaPlugin.getPlugin(Progression.class);
        Bukkit.getPluginManager().registerEvents(new InstantMineListener(), plugin);
    }

    public boolean isActive(UUID uuid) {
        ActiveSession session = activePlayers.getIfPresent(uuid);
        if (session == null) return false;
        if (System.currentTimeMillis() >= session.expiry()) {
            activePlayers.invalidate(uuid);
            return false;
        }
        return true;
    }

    private boolean isHoldingSourceItem(Player player) {
        ActiveSession session = activePlayers.getIfPresent(player.getUniqueId());
        if (session == null) return false;
        return itemFactory.fromItemStack(player.getInventory().getItemInMainHand())
                .map(ItemInstance::getBaseItem)
                .filter(b -> b.equals(session.sourceItem()))
                .isPresent();
    }

    private double getRemaining(UUID uuid) {
        ActiveSession session = activePlayers.getIfPresent(uuid);
        if (session == null) return 0;
        return (double) (session.expiry() - System.currentTimeMillis()) / 1000;
    }

    @Override
    public double getCooldown(InteractionActor actor) {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor,
                                                            @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance,
                                                            @Nullable ItemStack itemStack) {
        if (!actor.isPlayer() || itemInstance == null) return new InteractionResult.Fail(InteractionResult.FailReason.CUSTOM);
        activate((Player) actor.getEntity(), itemInstance.getBaseItem());
        return InteractionResult.Success.ADVANCE;
    }

    private void activate(Player player, BaseItem sourceItem) {
        activePlayers.put(player.getUniqueId(), new ActiveSession(
                System.currentTimeMillis() + (long) (duration * 1000), sourceItem));

        new SoundEffect(Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 0f, 2f).play(player);
        new SoundEffect("betterpvp", "game.capture_the_flag.flag.stolen", 0f, 2f).play(player);

        final Gamer gamer = clientManager.search().online(player).getGamer();
        final int maxTicks = (int) (duration * 20L);
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    activePlayers.invalidate(player.getUniqueId());
                    cancel();
                    return;
                }

                if (ticks++ >= maxTicks) {
                    activePlayers.invalidate(player.getUniqueId());
                    new SoundEffect(Sound.ENTITY_ALLAY_DEATH, 0f, 2f).play(player);
                    cancel();
                    return;
                }

                if (isHoldingSourceItem(player)) updateTitle(gamer, ticks);
                if (ticks % 20 == 0) {
                    new SoundEffect(Sound.UI_BUTTON_CLICK, 2f - (float) ticks / maxTicks, 1f).play(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void updateTitle(Gamer gamer, int ticks) {
        final double remainingSeconds = getRemaining(gamer.getUniqueId());
        final TextColor color = ticks % 40 < 20 ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.DARK_PURPLE;
        final String remaining = UtilFormat.formatNumber(remainingSeconds, 1, true);
        final TitleComponent title = new TitleComponent(
                0.0,
                4 / 20d, // 2 ticks
                0.0,
                false,
                gmr -> Component.text("Instant  Mine", color).font(Resources.Font.SMALL_CAPS),
                gmr -> Component.empty()
                        .append(Component.text("active for ", NamedTextColor.WHITE))
                        .append(Component.text(remaining + " seconds", NamedTextColor.YELLOW)));
        gamer.getTitleQueue().add(200, title);
    }
    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Instant Mine");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Instantly destroy any breakable block at no durability cost.");
    }

    private class InstantMineListener implements Listener {

        @EventHandler(priority = EventPriority.NORMAL)
        public void onBlockDamage(BlockDamageEvent event) {
            Player player = event.getPlayer();
            if (!isActive(player.getUniqueId())) return;
            if (!isHoldingSourceItem(player)) return;
            if (!targetBlocks.matches(event.getBlock())) return;
            event.setInstaBreak(true);
            new SoundEffect(Sound.BLOCK_LAVA_POP).play(event.getBlock().getLocation());
            new SoundEffect(Sound.BLOCK_AMETHYST_BLOCK_RESONATE).play(event.getBlock().getLocation());
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBreak(BlockBreakEvent event) {
            Player player = event.getPlayer();
            if (!isActive(player.getUniqueId())) return;
            if (!isHoldingSourceItem(player)) return;
            if (!targetBlocks.matches(event.getBlock())) return;

            // Spawn particles to make it more visually appealing
            Block block = event.getBlock();
            Particle.REVERSE_PORTAL.builder()
                    .count(10)
                    .extra(0.2)
                    .location(block.getLocation().toCenterLocation())
                    .receivers(60)
                    .spawn();
            Particle.ENCHANT.builder()
                    .count(5)
                    .extra(0.1)
                    .location(block.getLocation().toCenterLocation())
                    .offset(0.5, 0.5, 0.5)
                    .receivers(60)
                    .spawn();
        }
    }
}
