package me.mykindos.betterpvp.progression.profession.fishing.listener;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStopFishingEvent;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLootType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@BPvPListener
@Singleton
public class FishingListener implements Listener {

    private final Progression progression;
    private final ClientManager clientManager;
    private final FishingHandler fishingHandler;

    @Inject
    @Config(path = "fishing.minWaitTime", defaultValue = "5.0")
    private double minWaitTime;

    @Inject
    @Config(path = "fishing.maxWaitTime", defaultValue = "20.0")
    private double maxWaitTime;

    private int animatedDot = 1;
    private int direction = 1;

    private final ArrayListMultimap<Player, Bait> activeBaits = ArrayListMultimap.create();
    private final WeakHashMap<FishHook, Boolean> activeHooks = new WeakHashMap<>();

    // Saving it so depending on fish weight the time to reel and lure time can be adjusted
    // Also make it refresh every X seconds, so it doesn't feel like you're bound to one fish
    private final LoadingCache<Player, FishingLoot> fishLoot = Caffeine.newBuilder()
            .weakKeys()
            .build(key -> getRandomLoot());

    @Inject
    public FishingListener(Progression progression, FishingHandler fishingHandler, ClientManager clientManager) {
        this.progression = progression;
        this.fishingHandler = fishingHandler;
        this.clientManager = clientManager;
    }

    @UpdateEvent (delay = 60 * 5 * 1000, isAsync = true)
    public void saveCatches() {
        fishingHandler.getFishingRepository().saveAllFish(true);
    }
    // Title display
    @UpdateEvent(delay = 500)
    public void waitCue() {
        if (!fishingHandler.isEnabled()) return;
        final Iterator<Player> iterator = fishLoot.asMap().keySet().iterator();
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
            if (fishHook == null || !fishHook.isValid()) {
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
            clientManager.search().online(player).getGamer().getTitleQueue().add(500, title);
        }
    }

    // Fishing determination
    @UpdateEvent
    public void updateFishingStatus() {
        if (!fishingHandler.isEnabled()) return;
        // Clear baits
        activeBaits.asMap().keySet().removeIf(player -> player == null || !player.isOnline());
        activeBaits.values().removeIf(Bait::hasExpired);

        final Iterator<FishHook> iterator = activeHooks.keySet().iterator();
        while (iterator.hasNext()) {
            final FishHook fishHook = iterator.next();
            if (fishHook == null || !fishHook.isValid()) {
                iterator.remove();
                continue;
            }

            final Player player = (Player) fishHook.getShooter();
            final boolean inWater = fishHook.getState().equals(FishHook.HookState.BOBBING);
            if (inWater) {
                if (activeHooks.get(fishHook)) {
                    continue; // Already fishing
                }
                this.activeHooks.put(fishHook, true);
                Bukkit.getScheduler().runTaskLater(progression, () -> startFishing(player, fishHook), 1L);
            }
        }
    }

    private void startFishing(Player player, FishHook hook) {
        final FishingLoot loot = this.fishLoot.get(player);// store a new fish in the cache for them
        UtilServer.callEvent(new PlayerStartFishingEvent(player, loot, hook));

        // Process baits
        activeBaits.values().stream()
                .filter(bait -> bait.doesAffect(hook))
                .collect(Collectors.toMap(Bait::getType, // This makes sure that there can't be repeated types
                        Function.identity(),
                        (existing, replacement) -> existing))
                .values()
                .forEach(bait -> bait.track(hook));
        hook.setWaitTime(Math.max(1, hook.getWaitTime())); // If it gets to 0, it will be stuck in the water
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCatch(PlayerStopFishingEvent event) {
        if (!fishingHandler.isEnabled()) return;
        if (!event.getReason().equals(PlayerStopFishingEvent.FishingResult.CATCH)) {
            return;
        }

        final FishingLoot loot = event.getLoot();
        if (!(loot instanceof Fish fish)) {
            return;
        }

        fishingHandler.addFish(event.getPlayer(), fish);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFish(PlayerFishEvent event) {
        if (!fishingHandler.isEnabled()) return;
        final Player player = event.getPlayer();
        event.setExpToDrop(0); // Remove their xp
        switch (event.getState()) {
            case FISHING -> {
                // they cast their rod
                final FishHook hook = event.getHook();
                // Set defaults
                hook.setWaitTime((int) (minWaitTime * 20), (int) (maxWaitTime * 20));
                hook.setLureTime(20, 40);
                hook.setSkyInfluenced(false);
                hook.setRainInfluenced(false);
                activeHooks.put(hook, false);
            }
            case BITE -> {
                // something bit their hook but hasn't been reeled in yet
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2F);
            }
            case REEL_IN -> {
                // They reeled in before
                UtilServer.callEvent(new PlayerStopFishingEvent(player, fishLoot.getIfPresent(player), PlayerStopFishingEvent.FishingResult.EARLY_REEL));
                fishLoot.invalidate(player);
            }
            case FAILED_ATTEMPT -> {
                // they had a bite but missed it
                final Location hookLocation = event.getHook().getLocation();
                splash(hookLocation);
                Particle.HAPPY_VILLAGER.builder().location(hookLocation).receivers(60, true).spawn();
            }
            case CAUGHT_FISH -> {
                final FishingLoot loot = fishLoot.get(player);
                UtilServer.callEvent(new PlayerCaughtFishEvent(event.getPlayer(), loot, event.getHook(), event.getCaught()));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onCatch (PlayerCaughtFishEvent event) {
        if (!fishingHandler.isEnabled()) return;
        // this is the final step
        Player player = event.getPlayer();
        FishHook hook = event.getHook();
        final Item entity = (Item) Objects.requireNonNull(event.getCaught());

        final FishingLoot caught = event.getLoot();
        fishLoot.invalidate(player);

        splash(hook.getLocation());

        entity.setCanMobPickup(false);
        caught.processCatch(event);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 5f, 0F);

        UtilServer.callEvent(new PlayerStopFishingEvent(player, caught, PlayerStopFishingEvent.FishingResult.CATCH));
    }

    @EventHandler
    public void onBucket(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        final Client client = clientManager.search().online(event.getPlayer());
        if (client.isAdministrating()) {
            return;
        }

        if (!Fish.isFishItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBait (PlayerThrowBaitEvent event) {
        if (!fishingHandler.isEnabled()) return;
        final Vector velocity = event.getPlayer().getLocation().getDirection().normalize().multiply(new Vector(1.5, 2.0, 1.5));
        final Location location = event.getPlayer().getEyeLocation();
        event.getBait().spawn(progression, location, velocity);
        activeBaits.put(event.getPlayer(), event.getBait());
    }

    private void splash(Location hookLocation) {
        Particle.SPLASH.builder().location(hookLocation).offset(0.25, 0, 0.25).receivers(60, true).count(25).spawn();
        Particle.BUBBLE_POP.builder().location(hookLocation).offset(0.25, 0, 0.25).receivers(60, true).count(5).spawn();
    }

    private FishingLoot getRandomLoot() {
        final FishingLootType type = fishingHandler.getLootTypes().random();
        if (type == null) {
            // Return an empty loot
            return new FishingLoot() {
                @Override
                public FishingLootType getType() {
                    return null;
                }

                @Override
                public void processCatch(PlayerCaughtFishEvent event) {
                    UtilMessage.message(event.getPlayer(), "Fishing", "<red>No fish registered! Please report this to an admin!");
                    clientManager.sendMessageToRank("Progression", UtilMessage.deserialize("<yellow>%s</yellow> <red>caught a FishingLootType null fish. Is the config correct? Please report this internally.", event.getPlayer()), Rank.HELPER);
                }
            };
        }

        return type.generateLoot();
    }
}
