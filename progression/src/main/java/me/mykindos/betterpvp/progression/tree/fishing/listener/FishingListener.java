package me.mykindos.betterpvp.progression.tree.fishing.listener;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.model.Fish;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;
import java.util.Random;

@BPvPListener
public class FishingListener implements Listener {

    private static final Random RANDOM = new Random();

    @Inject
    private Fishing fishing;

    // Saving it so depending on fish weight the time to reel and lure time can be adjusted
    // Also make it refresh every X seconds, so it doesn't feel like you're bound to one fish
    private final LoadingCache<Player, Fish> fish = Caffeine.newBuilder()
            .weakKeys()
            .build(key -> getRandomFish());

    @EventHandler(priority = EventPriority.HIGH)
    public void onFish(PlayerFishEvent event) {
        final Player player = event.getPlayer();
        event.setExpToDrop(0); // Remove their xp
        switch (event.getState()) {
            case FISHING -> {
                // they cast their rod
                final FishHook hook = event.getHook();
                if (!hook.getState().equals(FishHook.HookState.BOBBING)) {
                    return;
                }

                final Fish fish = this.fish.get(player);

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
            case FAILED_ATTEMPT -> {
                // they had a bite but missed it
                final Location hookLocation = event.getHook().getLocation();
                splash(hookLocation);
                Particle.VILLAGER_ANGRY.builder().location(hookLocation).receivers(60, true).spawn();
            }
            case CAUGHT_FISH -> {
                // they reeled in the caught fish
                final FishHook hook = event.getHook();
                splash(hook.getLocation());

                final Fish caught = fish.get(player);
                final PlayerInventory inventory = player.getInventory();
                final Optional<FishingRodType> main = fishing.getRodType(inventory.getItemInMainHand());
                final Optional<FishingRodType> off = fishing.getRodType(inventory.getItemInOffHand());

                boolean canMainReel = main.map(rod -> rod.canReel(caught)).orElse(false);
                boolean canOffReel = off.map(rod -> rod.canReel(caught)).orElse(false);
                if (!canMainReel && !canOffReel) {
                    return; // Cancel if neither of the rods in your hand can reel
                }

                final ItemStack itemStack = caught.generateItem();
                if (inventory.firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1F);
                    UtilMessage.message(player, "Fishing", "You caught a " + caught.getType().getName() + " but your inventory was full!");
                } else {
                    inventory.addItem(itemStack);
                    UtilMessage.message(player, "Fishing", "You caught a " + caught.getType().getName() + "!");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 5f, 0F);
                }

                // todo announce if they got on leaderboard and play firework sound
            }
        }
    }

    private void splash(Location hookLocation) {
        Particle.WATER_SPLASH.builder().location(hookLocation).offset(0.25, 0, 0.25).receivers(60, true).count(25).spawn();
        Particle.BUBBLE_POP.builder().location(hookLocation).offset(0.25, 0, 0.25).receivers(60, true).count(5).spawn();
    }

    private Fish getRandomFish() {
        final FishType type = fishing.getFishTypes().random();
        final int weight = RANDOM.ints(type.getMinWeight(), type.getMaxWeight() + 1)
                .findFirst()
                .orElse(1);

        return new Fish(type, weight);
    }
}
