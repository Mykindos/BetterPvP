package me.mykindos.betterpvp.progression.profession.fishing.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@BPvPListener
@Singleton
@Getter
@Setter
public class SharkbaitAuraAbility extends AbstractInteraction implements Listener {

    private final Progression progression;
    private final ItemFactory itemFactory;
    private final List<FishHook> activeHooks = new ArrayList<>();

    private double catchSpeedMultiplier;
    private double radius;

    @Inject
    private SharkbaitAuraAbility(Progression progression, ItemFactory itemFactory) {
        super("Fishing Aura",
                "Increases fishing catch speed for all nearby fishermen");

        this.progression = progression;
        this.itemFactory = itemFactory;

        // Default values, will be overridden by config
        this.catchSpeedMultiplier = 0.7;
        this.radius = 6.0;
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // This is a passive ability, no active invocation needed
        return InteractionResult.Success.NO_ADVANCE;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStartFishing(PlayerStartFishingEvent event) {
        Player player = event.getPlayer();

        // Check if player is holding Sharkbait
        itemFactory.fromItemStack(player.getInventory().getItemInMainHand()).ifPresent(item -> {
            // Check if this ability is on the item
            Optional<InteractionContainerComponent> abilityContainer = item.getComponent(InteractionContainerComponent.class);
            if (abilityContainer.isPresent() && abilityContainer.get().getChain().hasRoot(this)) {
                event.getHook().setWaitTime((int) (event.getHook().getWaitTime() * catchSpeedMultiplier));
                activeHooks.add(event.getHook());
                return;
            }
        });

        // Apply effect to nearby players if in range of an active hook
        for (FishHook hook : activeHooks) {
            if (hook != null && hook.isValid()) {
                if (hook.getLocation().distance(event.getHook().getLocation()) <= radius) {
                    event.getHook().setWaitTime((int) (event.getHook().getWaitTime() * catchSpeedMultiplier));
                    return;
                }
            }
        }
    }

    @UpdateEvent(delay = 1000)
    public void invalidateHooks() {
        if (activeHooks.isEmpty()) return;
        Iterator<FishHook> iterator = activeHooks.iterator();
        while (iterator.hasNext()) {
            FishHook hook = iterator.next();
            if (hook == null || !hook.isValid()) {
                iterator.remove();
                return;
            }

            Location center = hook.getLocation();

            new BukkitRunnable() {
                final Collection<Player> receivers = center.getWorld().getNearbyPlayers(center, 48);
                double currentRadius = radius - 3.5;
                int i = 0;
                final Color color1 = Color.fromRGB(125, 122, 180);
                final Color color2 = Color.fromRGB(125, 88, 255);
                @Override
                public void run() {

                    if (i >= 0 && i <= 1) {
                        createCircle(center, currentRadius, 360, receivers, 1, color1, color2);
                        currentRadius += 1;
                    } else if (i > 2) {
                        createCircle(center, currentRadius, 11 - i, receivers, 60, color1, color2);
                    }

                    if (i > 10) {
                        this.cancel();
                    }
                    currentRadius += 0.2;
                    i++;
                }
            }.runTaskTimer(JavaPlugin.getPlugin(Progression.class), 0, 1);
        }
    }

    private void createCircle(Location center, final double radius, int modulusRange, Collection<Player> receivers, int angleFreq, Color color1, Color color2) {
        for (int degree = 0; degree <= 360; degree += 2) {
            if (!(degree % angleFreq < modulusRange || (degree % angleFreq) > angleFreq - modulusRange)) {
                continue;
            }
            double dx = radius * Math.sin(Math.toRadians(degree));
            double dz = radius * Math.cos(Math.toRadians(degree));
            Location newLoc = new Location(center.getWorld(), center.getX() + dx, center.getY(), center.getZ() + dz);
            Particle.DUST_COLOR_TRANSITION.builder()
                    .colorTransition(color1, color2)
                    .location(newLoc)
                    .receivers(receivers)
                    .spawn();
        }
    }
} 