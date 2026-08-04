package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
@BPvPListener
public class Rally extends Skill implements CooldownToggleSkill, Listener, BuffSkill, TeamSkill, AreaOfEffectSkill, DefensiveSkill {

    private final Map<UUID, Long> buffedUntil = new HashMap<>();

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double pulseInterval;
    private int resistanceStrength;
    private int emergeTicks;

    @Inject
    public Rally(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Rally";
    }

    @Override
    public Component[] getDescription(int level) {
        Component duration = getValueComponent(this::getDuration, level);
        Component radius = getValueComponent(this::getRadius, level, 1);
        Component resistance = Translations.component("champions.skill.effect.resistance",
                Component.text(UtilFormat.getRomanNumeral(resistanceStrength))).color(NamedTextColor.WHITE);
        Component cooldown = getValueComponent(this::getCooldown, level);
        return Translations.componentLines("champions.skill.knight.rally.description", duration, radius, resistance, cooldown);
    }

    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    public double getRadius(int level) {
        return baseRadius + (level - 1) * radiusIncreasePerLevel;
    }

    @Override
    public void toggle(Player player, int level) {
        final Location playerLocation = player.getLocation();
        final Optional<Location> closest = UtilLocation.getClosestSurfaceBlock(playerLocation, 3.0, true);
        final Location center = closest.orElse(playerLocation);
        if (closest.isPresent()) {
            center.add(0, 1, 0); // Move up one block to be above the surface
        }
        center.setYaw(playerLocation.getYaw());
        center.setPitch(0);

        final double radius = getRadius(level);
        final int durationTicks = (int) Math.round(getDuration(level) * 20);
        final int pulseTicks = Math.max(1, (int) Math.round(pulseInterval * 20));

        new SoundEffect("betterpvp", "game.domination.point_lost", 0f).play(center);
        new SoundEffect("minecraft", "item.mace.smash_ground_heavy", 1f).play(center);

        final BlockDisplay banner = center.getWorld().spawn(center.clone(), BlockDisplay.class, spawned -> {
            final BlockData data = Material.BLACK_BANNER.createBlockData();
            spawned.setBlock(data);
            spawned.setPersistent(false);

            // Starts buried, so the interpolation below reads as the banner being driven up out of the dirt
            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set(1.2f);
            transformation.getTranslation().set(
                    -0.5 * transformation.getScale().x(),
                    -1.8f,
                    -0.5f * transformation.getScale().z()
            );
            spawned.setTransformation(transformation);
        });

        UtilServer.runTaskLater(champions, () -> {
            if (!banner.isValid()) return;
            banner.setInterpolationDelay(0);
            banner.setInterpolationDuration(emergeTicks);

            Transformation transformation = banner.getTransformation();
            transformation.getTranslation().set(
                    -0.5f * transformation.getScale().x(),
                    0f,
                    -0.5f * transformation.getScale().z()
            );
            banner.setTransformation(transformation);
        }, 1L);

        new BukkitRunnable() {

            private int ticks;

            @Override
            public void run() {
                if (ticks > durationTicks || !player.isOnline() || player.isDead()) {
                    banner.remove();
                    cancel();
                    return;
                }

                final Collection<Player> receivers = center.getNearbyPlayers(48);
                drawRing(center, radius, receivers, Particle.INFESTED);
                buff(player, center, radius);

                if (ticks % pulseTicks == 0) {
                    pulse(center, radius, receivers);
                }

                ticks++;
            }
        }.runTaskTimer(champions, 0L, 1L);
    }

    /**
     * Refreshes the aura on everyone standing inside the ring. The buff is re-applied every tick with a
     * short lifetime so that walking out of the radius drops it almost immediately.
     */
    private void buff(Player player, Location center, double radius) {
        for (Player ally : UtilPlayer.getNearbyAllies(player, center, radius)) {
            hold(ally);
        }

        final Location location = player.getLocation();
        if (location.getWorld().equals(center.getWorld()) && location.distanceSquared(center) <= radius * radius) {
            hold(player);
        }
    }

    private void hold(Player player) {
        championsManager.getEffects().addEffect(player, player, EffectTypes.RESISTANCE, getName(), resistanceStrength, 80L, true, false);
        buffedUntil.put(player.getUniqueId(), System.currentTimeMillis() + 400L);

        Particle.ENCHANTED_HIT.builder()
                .location(player.getLocation().add(0, 1, 0))
                .offset(0.4, 0.6, 0.4)
                .count(2)
                .extra(0)
                .receivers(48)
                .spawn();
    }

    /**
     * The heartbeat of the banner — a horn, a puff of dust off the pole, and a shockwave that races out
     * to the edge of the radius so everyone can read how far the aura reaches.
     */
    private void pulse(Location center, double radius, Collection<Player> receivers) {
        new SoundEffect("littleroom_goblin_pack_vol2", "littleroom.goblinpackvol2.shaman_doppelganger", 1f, 0.5f).play(center);

        Particle.DUST_PILLAR.builder()
                .location(center.clone().add(0, 0.2, 0))
                .data(Material.BLACK_CONCRETE.createBlockData())
                .offset(0.5, 0.4, 0.5)
                .count(100)
                .extra(0.02)
                .receivers(receivers)
                .spawn();

        new BukkitRunnable() {

            private double current = 0.5;

            @Override
            public void run() {
                if (current >= radius) {
                    cancel();
                    return;
                }

                drawRing(center, current, receivers, Particle.WAX_OFF);
                current += radius / 5;
            }
        }.runTaskTimer(champions, 0L, 1L);
    }

    private void drawRing(Location center, double radius, Collection<Player> receivers, Particle particle) {
        double offset = Math.random() * 45;
        for (int degree = 0; degree < 360; degree += 8) {
            final double radians = Math.toRadians(degree + offset);
            final Location point = center.clone().add(Math.cos(radians) * radius, 0.15, Math.sin(radians) * radius);
            particle.builder()
                    .location(point)
                    .count(1)
                    .extra(0)
                    .receivers(receivers)
                    .spawn();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;

        final Long expiry = buffedUntil.get(damagee.getUniqueId());
        if (expiry == null || expiry < System.currentTimeMillis()) return;

        event.setKnockback(false);

        final Location location = damagee.getLocation().add(0, 1, 0);
        new SoundEffect("emaginationfallenknights", "custom.spell.swordclash", 1f).play(location);
        Particle.ENCHANTED_HIT.builder()
                .location(location)
                .offset(0.4, 0.4, 0.4)
                .count(12)
                .extra(0)
                .receivers(48)
                .spawn();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        buffedUntil.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        buffedUntil.remove(event.getEntity().getUniqueId());
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 8.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        baseRadius = getConfig("baseRadius", 6.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.5, Double.class);
        pulseInterval = getConfig("pulseInterval", 1.5, Double.class);
        resistanceStrength = getConfig("resistanceStrength", 1, Integer.class);
        emergeTicks = getConfig("emergeTicks", 3, Integer.class);
    }
}
