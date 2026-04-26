package me.mykindos.betterpvp.progression.profession.mining.item.interaction;

import lombok.Setter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.progression.Progression;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InstantMineInteraction extends CooldownInteraction implements DisplayedInteraction {

    private final Map<UUID, Long> activePlayers = new ConcurrentHashMap<>();

    @Setter private double cooldown;
    @Setter private double duration;
    private final ClientManager clientManager;

    public InstantMineInteraction(CooldownManager cooldownManager, ClientManager clientManager, double cooldown, double duration) {
        super("Instant Mine", cooldownManager);
        this.cooldown = cooldown;
        this.duration = duration;
        this.clientManager = clientManager;
        final Progression plugin = JavaPlugin.getPlugin(Progression.class);
        Bukkit.getPluginManager().registerEvents(new InstantMineListener(), plugin);
    }

    public boolean isActive(UUID uuid) {
        Long expiry = activePlayers.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            activePlayers.remove(uuid);
            return false;
        }
        return true;
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
        if (!actor.isPlayer()) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CUSTOM);
        }

        Player player = (Player) actor.getEntity();
        activePlayers.put(player.getUniqueId(), System.currentTimeMillis() + (long) (duration * 1000));

        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.getTitleQueue().add(500, new TitleComponent(
                0.0,
                1.0,
                0.0,
                false,
                gmr -> Component.text("Instant Mine", NamedTextColor.GREEN),
                gmr -> Component.text("active for ", NamedTextColor.WHITE)
                        .append(Component.text(duration  + " seconds", NamedTextColor.YELLOW)
        )));

        return InteractionResult.Success.ADVANCE;
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

        private boolean isTooHard(Block block) {
            return block.getType().getHardness() >= Material.OBSIDIAN.getHardness();
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onBlockDamage(BlockDamageEvent event) {
            if (!isActive(event.getPlayer().getUniqueId())) return;
            if (isTooHard(event.getBlock())) return;
            event.setInstaBreak(true);
            new SoundEffect(Sound.BLOCK_LAVA_POP).play(event.getBlock().getLocation());
        }
    }
}
