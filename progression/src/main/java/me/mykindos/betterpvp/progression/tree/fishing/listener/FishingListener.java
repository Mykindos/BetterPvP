package me.mykindos.betterpvp.progression.tree.fishing.listener;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@BPvPListener
public class FishingListener implements Listener {

    private static final Random RANDOM = new Random();

    @Inject
    private Fishing fishing;

    @Inject
    private GamerManager gamerManager;

    private int animatedDot = 1;
    private int direction = 1;

    // Saving it so depending on fish weight the time to reel and lure time can be adjusted
    // Also make it refresh every X seconds, so it doesn't feel like you're bound to one fish
    private final LoadingCache<Player, FishingLoot> fish = Caffeine.newBuilder()
            .weakKeys()
            .build(key -> getRandomLoot());

    @UpdateEvent(delay = 500)
    public void waitCue() {
        final Iterator<Player> iterator = fish.asMap().keySet().iterator();
        Component component = Component.text()
                .append(Component.text(".", animatedDot == 1 ? NamedTextColor.WHITE : NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(".", animatedDot == 2 ? NamedTextColor.WHITE : NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(".", animatedDot == 3 ? NamedTextColor.WHITE : NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(".", animatedDot == 4 ? NamedTextColor.WHITE : NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(".", animatedDot == 5 ? NamedTextColor.WHITE : NamedTextColor.GRAY))
                .build();

        if (animatedDot >= 5) {
            direction = -1;
        } else if (animatedDot <= 1) {
            direction = 1;
        }

        animatedDot += direction;

        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final FishHook fishHook = player.getFishHook();
            if (fishHook == null) {
                iterator.remove();
                continue;
            }

            if (!fishHook.getState().equals(FishHook.HookState.BOBBING)) {
                continue; // Only display the dots when the hook is in the water
            }

            // Three dots that appear whenever your hook is in the water
            // Dots will have an animation where they will turn white, from gray, in a loop
            // The dots will be in the middle of the screen, and will be 3 dots in a row
            final TitleComponent title = TitleComponent.subtitle(0, 0.8, 0, false, gamer -> component);
            gamerManager.getObject(player.getUniqueId()).ifPresent(gamer -> gamer.getTitleQueue().add(500, title));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFish(PlayerFishEvent event) {
        final Player player = event.getPlayer();
        event.setExpToDrop(0); // Remove their xp
        switch (event.getState()) {
            case FISHING -> {
                // they cast their rod
                final FishHook hook = event.getHook();
                final FishingLoot loot = this.fish.get(player); // nullable

                // todo: implement bait to increase rate of catching fish
                // todo: make fish change wait and lure time

                hook.setWaitTime(5 * 20, 20 * 20);
                hook.setLureTime(2 * 20, 4 * 20);
                hook.setSkyInfluenced(false);
                hook.setRainInfluenced(false);
            }
            case BITE -> {
                // something bit their hook but hasn't been reeled in yet
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2F);
            }
            case REEL_IN -> {
                // They reeled in before
                fish.invalidate(player);
            }
            case FAILED_ATTEMPT -> {
                // they had a bite but missed it
                final Location hookLocation = event.getHook().getLocation();
                splash(hookLocation);
                Particle.VILLAGER_ANGRY.builder().location(hookLocation).receivers(60, true).spawn();
            }
            case CAUGHT_FISH -> {
                // they reeled in the caught fish
                final FishingLoot caught = fish.get(player);

                final Item entity = (Item) Objects.requireNonNull(event.getCaught());
                fish.invalidate(player);
                final FishHook hook = event.getHook();
                splash(hook.getLocation());

                final PlayerInventory inventory = player.getInventory();
                final Optional<FishingRodType> main = fishing.getRodType(inventory.getItemInMainHand());
                final Optional<FishingRodType> off = fishing.getRodType(inventory.getItemInOffHand());

                boolean canMainReel = main.map(rod -> rod.canReel(caught)).orElse(false);
                boolean canOffReel = off.map(rod -> rod.canReel(caught)).orElse(false);
                if (!canMainReel && !canOffReel) {
                    UtilMessage.message(event.getPlayer(), "Fishing", "<red>Your rod couldn't reel this %s!", caught.getType().getName());
                    entity.remove();
                    return; // Cancel if neither of the rods in your hand can reel
                }

                entity.setCanMobPickup(false);
                caught.processCatch(event);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 5f, 0F);

                // todo announce if they got on leaderboard and play firework sound
            }
        }
    }

    @EventHandler
    public void onBucket(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        final Optional<Gamer> gamer = gamerManager.getObject(event.getPlayer().getUniqueId());
        if (gamer.isPresent() && gamer.get().getClient().isAdministrating()) {
            return;
        }

        if (!Fish.isFishItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
    }

    private void splash(Location hookLocation) {
        Particle.WATER_SPLASH.builder().location(hookLocation).offset(0.25, 0, 0.25).receivers(60, true).count(25).spawn();
        Particle.BUBBLE_POP.builder().location(hookLocation).offset(0.25, 0, 0.25).receivers(60, true).count(5).spawn();
    }

    private FishingLoot getRandomLoot() {
        final FishingLootType type = fishing.getLootTypes().random();
        if (type == null) {
            // Return an empty loot
            return new FishingLoot() {
                @Override
                public FishingLootType getType() {
                    return null;
                }

                @Override
                public void processCatch(PlayerFishEvent event) {
                    UtilMessage.message(event.getPlayer(), "Fishing", "<red>No fish registered! Please report this to an admin!");
                }
            };
        }

        return type.generateLoot();
    }
}
