package me.mykindos.betterpvp.champions.item.corebrand;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageModifier;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.model.override.ItemModelOverrideService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.component.PermanentComponent;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Cascade is the Corebrand's awakened state. Fuel banks up out of combat pressure; swapping hands spends it
 * in a short window where every hit lands empowered and {@link FissionLunge} becomes available.
 * <p>
 * Fuel is spent by actions, never by time — the duration is a separate ceiling on the window. That keeps the
 * budget honest: a full bar really is {@code 1 / hit-cost} empowered hits or {@code 1 / lunge-cost} lunges.
 */
@Singleton
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@BPvPListener
public class CascadeAbility extends AbstractInteraction implements Listener, DisplayedInteraction {

    private static final int FUEL_BAR_LENGTH = 15;

    private double fuelPerDamageDealt;
    private double fuelPerDamageTaken;
    private double fuelDecayPerSecond;
    private double duration;
    private double cascadeDecayPerSecond;
    private double cascadeDecayGrace;
    private double hitCost;
    private double hitBonusDamage;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final ClientManager clientManager;
    @EqualsAndHashCode.Exclude
    private final ItemModelOverrideService modelOverrideService;
    @EqualsAndHashCode.Exclude
    private final Map<UUID, CorebrandData> playerData = new HashMap<>();
    @EqualsAndHashCode.Exclude
    private Corebrand corebrand;

    @EqualsAndHashCode.Exclude
    private final DisplayObject<Component> actionBar = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();
        if (player == null || corebrand == null || !corebrand.isHoldingWeapon(player)) {
            return null;
        }

        final CorebrandData data = playerData.get(player.getUniqueId());
        if (data == null) {
            return null;
        }

        return ProgressBar.withLength(data.getFuel(), FUEL_BAR_LENGTH)
                .withProgressColor(data.isCascading() ? TextColor.color(0xFF3B30) : TextColor.color(0x4FC3FF))
                .withRemainingColor(NamedTextColor.DARK_GRAY)
                .build();
    });

    @Inject
    private CascadeAbility(Champions champions, ClientManager clientManager, ItemModelOverrideService modelOverrideService) {
        super("cascade");
        this.champions = champions;
        this.clientManager = clientManager;
        this.modelOverrideService = modelOverrideService;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("champions.ability.cascade.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("champions.ability.cascade.description");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                   @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final CorebrandData data = playerData.get(player.getUniqueId());
        if (data == null) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (data.isCascading()) {
            sheath(player, data);
            return InteractionResult.Success.ADVANCE;
        }

        // Any amount of fuel will do — the window simply lasts as long as what you brought pays for
        if (data.getFuel() <= 0) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        unsheath(player, data);
        return InteractionResult.Success.ADVANCE;
    }

    private void unsheath(Player player, CorebrandData data) {
        data.setCascading(true);
        data.setCascadeStart(System.currentTimeMillis());
        data.setLastAction(System.currentTimeMillis());
        data.setTicks(0);

        new SoundEffect("emaginationfallendefender", "custom.spell.unsheath", 2F).play(player.getLocation());
        playTrail(UtilPlayer.getMidpoint(player), true);
    }

    private void sheath(Player player, CorebrandData data) {
        if (!data.isCascading()) {
            return;
        }

        data.setCascading(false);
        data.setFuel(0);
        UtilPlayer.clearWarningEffect(player);

        new SoundEffect("emaginationfallendefender", "custom.spell.sheath", 2F).play(player.getLocation());
        playTrail(UtilPlayer.getMidpoint(player), false);
    }

    /**
     * @return true if the player is in Cascade and can still pay for actions
     */
    public boolean isCascading(Player player) {
        final CorebrandData data = playerData.get(player.getUniqueId());
        return data != null && data.isCascading();
    }

    public CorebrandData getData(Player player) {
        return playerData.get(player.getUniqueId());
    }

    /**
     * Expanding (unsheath) or contracting (sheath) ring of red and blue motes around the wielder.
     */
    private void playTrail(Location origin, boolean outward) {
        final Location centre = origin.clone().add(0, 1, 0);
        for (int step = 0; step < 3; step++) {
            final double radius = outward ? 0.6 + (step * 0.7) : 2.0 - (step * 0.7);
            for (int point = 0; point < 16; point++) {
                final double angle = (Math.PI * 2 / 16) * point;
                final double yOffset = Math.random() * radius * 2 - radius;
                final Location at = centre.clone().add(Math.cos(angle) * Math.random() * radius, yOffset, Math.sin(angle) * Math.random() * radius);
                Particle.TRAIL.builder()
                        .location(outward ? origin : at)
                        .data(new Particle.Trail(outward ? at : origin,
                                point % 2 == 0
                                ? Color.fromRGB(0xC8, 0x22, 0x2A)
                                : Color.fromRGB(0x4F, 0xC3, 0xFF), (int) (Math.random() * 10 + 5)))
                        .count(5)
                        .offset(0.1, 0.1, 0.1)
                        .receivers(60)
                        .spawn();
            }
        }
    }

    /**
     * Blue burst on a player struck by an empowered hit.
     */
    private void playHitEffect(Entity target) {
        final Location at = target.getLocation().add(0, 1, 0);
        new SoundEffect("emaginationfallendefender", "custom.spell.sfeproj_hit", 2F).play(at);
        new SoundEffect(Sound.BLOCK_CONDUIT_DEACTIVATE, 2F).play(at);

        Particle.DUST.builder()
                .location(at)
                .data(new Particle.DustOptions(Color.fromRGB(0x4F, 0xC3, 0xFF), 1.4F))
                .count(30)
                .offset(0.5, 0.7, 0.5)
                .extra(0.05)
                .receivers(60)
                .spawn();
        Particle.DUST.builder()
                .location(at)
                .data(new Particle.DustOptions(Color.fromRGB(0x2E, 0xF2, 0xE0), 1.0F))
                .count(15)
                .offset(0.4, 0.6, 0.4)
                .receivers(60)
                .spawn();
    }

    @UpdateEvent(priority = 999)
    public void doCascade() {
        final Iterator<Map.Entry<UUID, CorebrandData>> iterator = playerData.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, CorebrandData> entry = iterator.next();
            final Player player = Bukkit.getPlayer(entry.getKey());
            final CorebrandData data = entry.getValue();

            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // Cascade ends on the duration ceiling, on an empty tank, or when the weapon leaves their hand
            if (data.isCascading()) {
                final boolean expired = UtilTime.elapsed(data.getCascadeStart(), (long) (duration * 1000L));
                if (expired || data.getFuel() <= 0 || !corebrand.isHoldingWeapon(player)) {
                    sheath(player, data);
                } else {
                    data.setTicks(data.getTicks() + 1);
                    tickCascade(player, data);
                }
            } else if (data.getFuel() > 0) {
                data.decay(fuelDecayPerSecond / 20d);
            }

            syncModel(player, data);

            if (data.isMarkForRemoval() && !data.isCascading()) {
                clearModel(player, data);
                iterator.remove();
            }
        }
    }

    /**
     * Everything the awakened blade does every tick: it burns, it screams, it counts down, and it decays —
     * but only once the wielder has gone quiet for the grace period.
     */
    private void tickCascade(Player player, CorebrandData data) {
        if (UtilTime.elapsed(data.getLastAction(), (long) (cascadeDecayGrace * 1000L))) {
            data.decay(cascadeDecayPerSecond / 20d);
        }

        UtilPlayer.setWarningEffect(player, 1);
        playHandFlame(player);
        playTimer(player, data);

        if (System.currentTimeMillis() >= data.getNextTickSound()) {
            new SoundEffect("emaginationfallendefender", "custom.spell.fftick", 1F).play(player.getLocation());
            // Pseudo-random 2-5 tick gap so the burn never settles into a metronome
            data.setNextTickSound(System.currentTimeMillis() + (long) ((2 + Math.random() * 3) * 50));
        }
    }

    /**
     * Flame streaming outward from whichever hand actually holds the sword.
     */
    private void playHandFlame(Player player) {
        Particle.SOUL_FIRE_FLAME.builder()
                .location(UtilPlayer.getMidpoint(player))
                .count(0)
                .offset(Math.random() * 2 - 1, Math.random() * 2 - 1, Math.random() * 2 - 1)
                .extra(0.2)
                .receivers(60)
                .spawn();
    }

    /**
     * Flashing countdown in the subtitle, with a tick that climbs in pitch and a heartbeat underneath it.
     */
    private void playTimer(Player player, CorebrandData data) {
        final double remaining = Math.max(0, duration - ((System.currentTimeMillis() - data.getCascadeStart()) / 1000d));
        final int ticks = data.getTicks();
        final TextColor color = ticks % 10 < 5 ? NamedTextColor.RED : NamedTextColor.GOLD;

        final TitleComponent title = new TitleComponent(
                0.0,
                4 / 20d,
                0.0,
                false,
                gmr -> Component.empty(),
                gmr -> Component.text(UtilFormat.formatNumber(remaining, 1, true) + "s", color));
        data.getGamer().getTitleQueue().add(200, title);

        final float urgency = (float) (1 - (remaining / duration));
        if (ticks % 10 == 0) {
            new SoundEffect(Sound.UI_BUTTON_CLICK, 1.0F + urgency, 0.6F).play(player);
        }

        if (ticks % 20 == 0) {
            new SoundEffect(Sound.ENTITY_WARDEN_HEARTBEAT, 1.0F + (urgency * 0.5F), 0.8F).play(player);
        }
    }

    /**
     * Drives the held model from the player's actual state. Only fires on a transition, so the charged cue
     * plays once rather than every tick.
     */
    private void syncModel(Player player, CorebrandData data) {
        final CorebrandData.State desired = data.isCascading() || data.isFull()
                ? CorebrandData.State.CHARGED
                : CorebrandData.State.DORMANT;

        if (desired == data.getState()) {
            return;
        }

        data.setState(desired);
        final Key dormant = corebrand.getDormantModel();
        if (desired == CorebrandData.State.CHARGED) {
            modelOverrideService.set(player, dormant, corebrand.getChargedModel());
            new SoundEffect("emaginationfallenheroes", "custom.spell.soulfirechannel", 1F).play(player.getLocation());
        } else {
            modelOverrideService.clear(player, dormant);
        }
    }

    private void clearModel(Player player, CorebrandData data) {
        if (data.getState() != CorebrandData.State.DORMANT) {
            data.setState(CorebrandData.State.DORMANT);
            modelOverrideService.clear(player, corebrand.getDormantModel());
        }
    }

    /**
     * Fuel in, damage out. Outside Cascade the blade banks combat pressure from both directions; inside it,
     * every melee hit spends fuel for a flat damage bonus until the tank runs dry.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(DamageEvent event) {
        if (event.isCancelled() || corebrand == null) {
            return;
        }

        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) {
            handleFuelGain(event);
            return;
        }

        if (event.getDamager() instanceof Player damager && corebrand.isHoldingWeapon(damager)) {
            final CorebrandData data = playerData.get(damager.getUniqueId());
            if (data != null && data.isCascading()) {
                data.spend(hitCost);
                event.addModifier(new InteractionDamageModifier.Flat(this, hitBonusDamage));
                playHitEffect(event.getDamagee());
                return; // Empowered hits never also charge the bar
            }
        }

        handleFuelGain(event);
    }

    private void handleFuelGain(DamageEvent event) {
        if (event.getDamager() instanceof Player damager && corebrand.isHoldingWeapon(damager)) {
            gain(damager, event.getDamage() * fuelPerDamageDealt);
        }

        if (event.getDamagee() instanceof Player damagee && corebrand.isHoldingWeapon(damagee)) {
            gain(damagee, event.getDamage() * fuelPerDamageTaken);
        }
    }

    private void gain(Player player, double amount) {
        final CorebrandData data = playerData.get(player.getUniqueId());
        if (data != null && !data.isCascading()) {
            data.gain(amount);
        }
    }

    private void active(Player player) {
        playerData.compute(player.getUniqueId(), (playerId, previous) -> {
            if (previous != null) {
                previous.setMarkForRemoval(false);
                previous.getGamer().getActionBar().add(350, actionBar);
                return previous;
            }

            final Gamer gamer = clientManager.search().online(playerId).orElseThrow().getGamer();
            final CorebrandData data = new CorebrandData(gamer);
            gamer.getActionBar().add(350, actionBar);
            return data;
        });
    }

    private void deactivate(Player player) {
        final CorebrandData data = playerData.get(player.getUniqueId());
        if (data == null) {
            return;
        }

        sheath(player, data);
        clearModel(player, data);
        data.setMarkForRemoval(true);
        data.getGamer().getActionBar().remove(actionBar);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (corebrand != null && corebrand.isHoldingWeapon(event.getPlayer())) {
            active(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerData.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        final CorebrandData data = playerData.get(event.getEntity().getUniqueId());
        if (data != null) {
            data.setCascading(false);
            data.setFuel(0);
            data.setState(CorebrandData.State.DORMANT);
        }
    }

    @EventHandler
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        if (corebrand == null) {
            return;
        }

        if (corebrand.isCorebrand(event.getNewItemStack())) {
            active(event.getPlayer());
        } else if (corebrand.isCorebrand(event.getOldItemStack())
                && Arrays.stream(event.getPlayer().getInventory().getContents()).noneMatch(corebrand::isCorebrand)) {
            deactivate(event.getPlayer());
        }
    }

    @EventHandler
    public void onSwapWeapon(PlayerItemHeldEvent event) {
        if (corebrand == null) {
            return;
        }

        final Player player = event.getPlayer();
        if (corebrand.isCorebrand(player.getInventory().getItem(event.getNewSlot()))) {
            active(player);
        } else {
            final CorebrandData data = playerData.get(player.getUniqueId());
            if (data != null) {
                sheath(player, data);
                clearModel(player, data);
            }
        }
    }

    /**
     * Direction a Fission victim is thrown: the lunge reflected off the ground plane, floored so a flat lunge
     * still lifts. Diving onto a target sends them up, and the wielder has to turn and climb to chain again.
     */
    public static Vector reflect(Vector lungeDirection, double minimumLift) {
        final Vector reflected = new Vector(lungeDirection.getX(), -lungeDirection.getY(), lungeDirection.getZ());
        if (reflected.getY() < minimumLift) {
            reflected.setY(minimumLift);
        }
        return reflected.normalize();
    }
}
