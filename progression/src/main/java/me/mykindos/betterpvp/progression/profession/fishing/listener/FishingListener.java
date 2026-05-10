package me.mykindos.betterpvp.progression.profession.fishing.listener;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.fishing.event.FishingTreasureChanceCalculationEvent;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStopFishingEvent;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profession.fishing.loot.FishLoot;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@BPvPListener
@Singleton
@CustomLog
public class FishingListener implements Listener {

    private final Progression progression;
    private final ClientManager clientManager;
    private final FishingHandler fishingHandler;
    private final ProfessionProfileManager professionProfileManager;
    private final ProfessionNodeManager progressionSkillManager;

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

    /**
     * Maps each player to their pre-rolled loot bundle for the current cast.
     * Rolled when the hook enters the water and cleared on reel-in or catch.
     */
    private final WeakHashMap<Player, LootBundle> fishLoot = new WeakHashMap<>();

    public ArrayListMultimap<Player, Bait> getActiveBaits() {
        return activeBaits;
    }

    @Inject
    public FishingListener(Progression progression, FishingHandler fishingHandler, ClientManager clientManager,
                           ProfessionProfileManager professionProfileManager, ProfessionNodeManager progressionSkillManager) {
        this.progression = progression;
        this.fishingHandler = fishingHandler;
        this.clientManager = clientManager;
        this.professionProfileManager = professionProfileManager;
        this.progressionSkillManager = progressionSkillManager;
    }

    @UpdateEvent(delay = 60 * 5 * 1000, isAsync = true)
    public void saveCatches() {
        fishingHandler.getFishingRepository().saveAllFish(true);
    }

    @UpdateEvent(delay = 500)
    public void waitCue() {
        if (!fishingHandler.isEnabled()) return;
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

        for (Player player : fishLoot.keySet()) {
            final FishHook fishHook = player.getFishHook();
            if (fishHook == null || !fishHook.isValid()) {
                continue;
            }
            if (!fishHook.getState().equals(FishHook.HookState.BOBBING)) {
                continue;
            }
            final TitleComponent title = TitleComponent.subtitle(0, 0.8, 0, false, gamer -> component);
            clientManager.search().online(player).getGamer().getTitleQueue().add(500, title);
        }
    }

    @UpdateEvent
    public void updateFishingStatus() {
        if (!fishingHandler.isEnabled()) return;
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
                    continue;
                }
                this.activeHooks.put(fishHook, true);
                org.bukkit.Bukkit.getScheduler().runTaskLater(progression, () -> startFishing(player, fishHook), 1L);
            }
        }
    }

    private void startFishing(Player player, FishHook hook) {
        final LootBundle bundle = fishingHandler.getRandomLoot(player, hook.getLocation());
        fishLoot.put(player, bundle);
        UtilServer.callEvent(new PlayerStartFishingEvent(player, bundle, hook));

        activeBaits.values().stream()
                .filter(bait -> bait.doesAffect(hook))
                .collect(Collectors.toMap(Bait::getType, Function.identity(), (existing, replacement) -> existing))
                .values()
                .forEach(bait -> bait.track(hook));
        hook.setWaitTime(Math.max(1, hook.getWaitTime()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCatch(PlayerStopFishingEvent event) {
        if (!fishingHandler.isEnabled()) return;
        if (!event.getReason().equals(PlayerStopFishingEvent.FishingResult.CATCH)) {
            return;
        }
        // Fish XP/leaderboards are handled in onCatch(PlayerCaughtFishEvent) via FishLoot entries.
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFish(PlayerFishEvent event) {
        if (!fishingHandler.isEnabled()) return;
        final Player player = event.getPlayer();
        event.setExpToDrop(0);
        switch (event.getState()) {
            case FISHING -> {
                final FishHook hook = event.getHook();
                hook.setWaitTime((int) (minWaitTime * 20), (int) (maxWaitTime * 20));
                hook.setLureTime(20, 40);
                hook.setSkyInfluenced(false);
                hook.setRainInfluenced(false);
                activeHooks.put(hook, false);
            }
            case BITE -> {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2F);
            }
            case REEL_IN -> {
                UtilServer.callEvent(new PlayerStopFishingEvent(player, fishLoot.get(player), PlayerStopFishingEvent.FishingResult.EARLY_REEL));
                fishLoot.remove(player);
            }
            case FAILED_ATTEMPT -> {
                final Location hookLocation = event.getHook().getLocation();
                splash(hookLocation);
                Particle.HAPPY_VILLAGER.builder().location(hookLocation).receivers(60, true).spawn();
            }
            case CAUGHT_FISH -> {
                final Entity caught = event.getCaught();
                if (caught != null) {
                    caught.remove(); // remove default
                }

                final LootBundle bundle = fishLoot.get(player);
                if (bundle == null) {
                    UtilMessage.message(player, "Fishing", "<red>No loot bundle found — please report this to an admin!");
                    return;
                }

                // Find the first FishLoot in the bundle to attach to the event (for skill hooks).
                FishLoot primaryFishLoot = null;
                for (Loot<?, ?> entry : bundle) {
                    if (entry instanceof FishLoot fl) {
                        primaryFishLoot = fl;
                        fl.rollFish(); // re-roll weight for this specific catch
                        break;
                    }
                }

                PlayerCaughtFishEvent caughtFishEvent = new PlayerCaughtFishEvent(
                        player, bundle, primaryFishLoot, event.getHook());

                Optional<ProfessionNode> progressionSkillOptional = progressionSkillManager.getSkill("Base Fishing");
                progressionSkillOptional.ifPresent(progressionSkill ->
                        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
                            var profession = profile.getProfessionDataMap().get("Fishing");
                            if (profession != null && profession.getBuild().getSkillLevel(progressionSkill) >= 1) {
                                caughtFishEvent.setBaseFishingUnlocked(true);
                            }
                        }));

                UtilServer.callEvent(caughtFishEvent);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onCatch(PlayerCaughtFishEvent event) {
        if (!fishingHandler.isEnabled()) return;
        final Player player = event.getPlayer();
        final FishHook hook = event.getHook();
        final LootBundle bundle = event.getBundle();
        fishLoot.remove(player);

        splash(hook.getLocation());

        // Award all loot entries in the bundle.
        for (Loot<?, ?> loot : bundle) {
            final LootContext context = bundle.getContext();
            if (loot.award(context) instanceof Item item) {
                UtilItem.reserveItem(item, player, 30);
                UtilServer.runTaskLater(progression, () -> {
                    if (item.isValid()) {
                        item.remove();
                    }
                }, 20L * 60L);
            }

            if (loot instanceof FishLoot fishLoot) {
                // rollFish() was already called in onFish CAUGHT_FISH before the event was fired.
                // Skills may have mutated the weight. Now award and grant XP.
                final Fish fish = fishLoot.getCurrentFish();
                if (fish != null) {
                    fishingHandler.addFish(player, fish);
                    UtilMessage.message(player, "Fishing", "You caught a <alt>%s</alt> (<alt2>%slb</alt2>)!",
                            fish.getTypeName(), UtilFormat.formatNumber(fish.getWeight()));
                }
            }

        }

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 5f, 0F);
        UtilServer.callEvent(new PlayerStopFishingEvent(player, bundle, PlayerStopFishingEvent.FishingResult.CATCH));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropFish(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.COD) {
            UtilServer.runTaskLater(progression, () -> {
                if (event.getItemDrop().isValid()) {
                    event.getItemDrop().remove();
                }
            }, 20L * 30L);
        }
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
    public void onBait(PlayerThrowBaitEvent event) {
        if (!fishingHandler.isEnabled()) return;
        final Vector velocity = event.getPlayer().getLocation().getDirection().normalize().multiply(new Vector(1.5, 2.0, 1.5));
        final Location location = event.getPlayer().getEyeLocation();
        event.getBait().spawn(progression, location, velocity);
        activeBaits.put(event.getPlayer(), event.getBait());
    }

    @EventHandler
    public void onTreasureDrop(FishingTreasureChanceCalculationEvent event) {
        if (!fishingHandler.isEnabled()) return;
        for (Bait activeBait : activeBaits.values()) {
            if (!activeBait.getType().equalsIgnoreCase("Lucky")) continue;
            if (activeBait.getLocation().getWorld().equals(event.getLocation().getWorld())) {
                if (activeBait.getLocation().distanceSquared(event.getLocation()) <= Math.pow(activeBait.getRadius(), 2)) {
                    event.setTreasureChance(event.getTreasureChance() + activeBait.getMultiplier());
                    break;
                }
            }

        }
    }

    private void splash(Location hookLocation) {
        Particle.SPLASH.builder().location(hookLocation).offset(0.25, 0, 0.25).receivers(60, true).count(25).spawn();
        Particle.BUBBLE_POP.builder().location(hookLocation).offset(0.25, 0, 0.25).receivers(60, true).count(5).spawn();
    }
}
