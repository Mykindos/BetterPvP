package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data.BoneSpike;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
@BPvPListener
public class Gravebind extends Skill implements CooldownToggleSkill, Listener, CrowdControlSkill, AreaOfEffectSkill, DebuffSkill {

    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private double telegraphDuration;
    private double caughtSpikeRadius;
    private int fieldSpikes;
    private int caughtSpikes;

    @Inject
    public Gravebind(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Gravebind";
    }

    @Override
    public Component[] getDescription(int level) {
        Component radius = getValueComponent(this::getRadius, level, 1);
        Component duration = getValueComponent(this::getDuration, level, 2);
        Component telegraph = getValueComponent(lvl -> telegraphDuration, level, 2);
        Component cooldown = getValueComponent(this::getCooldown, level);
        return Translations.componentLines("champions.skill.warlock.gravebind.description", radius, telegraph, duration, cooldown);
    }

    public double getRadius(int level) {
        return baseRadius + (level - 1) * radiusIncreasePerLevel;
    }

    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    @Override
    public void toggle(Player player, int level) {
        final Location origin = player.getLocation().clone();
        final int telegraphTicks = Math.max(1, (int) Math.round(telegraphDuration * 20));
        new SoundEffect(Sound.ENTITY_SKELETON_AMBIENT, 0.6f, 1.4f).play(origin);

        new BukkitRunnable() {

            private final List<BoneSpike> spikes = new ArrayList<>();
            private int ticks;

            @Override
            public void run() {
                if (ticks < telegraphTicks) {
                    telegraph(origin, getRadius(level));
                } else if (ticks == telegraphTicks) {
                    erupt(player, origin, level, spikes);
                } else {
                    spikes.forEach(BoneSpike::tick);
                    if (spikes.stream().allMatch(BoneSpike::isFinished)) {
                        cancel();
                    }
                }

                ticks++;
            }
        }.runTaskTimer(champions, 0L, 1L);
    }

    /**
     * Surfacing bone dust that tells everyone standing in the radius exactly where the spikes are about
     * to come up, and gives them the window to jump clear.
     */
    private void telegraph(Location origin, double radius) {
        for (int i = 0; i < 6; i++) {
            groundPointNear(origin, radius).ifPresent(point -> Particle.BLOCK.builder()
                    .location(point)
                    .data(Material.BONE_BLOCK.createBlockData())
                    .offset(0.25, 0, 0.25)
                    .count(4)
                    .extra(0)
                    .allPlayers()
                    .spawn());
        }
    }

    private void erupt(Player player, Location origin, int level, List<BoneSpike> spikes) {
        final double radius = getRadius(level);
        final long durationMillis = (long) (getDuration(level) * 1000);
        final int lingerTicks = (int) Math.round(getDuration(level) * 20);

        new SoundEffect(Sound.BLOCK_BONE_BLOCK_BREAK, 0.5f, 1.5f).play(origin);
        new SoundEffect(Sound.ENTITY_SKELETON_HURT, 0.7f, 1.2f).play(origin);

        for (int i = 0; i < fieldSpikes; i++) {
            groundPointNear(origin, radius).ifPresent(point -> spikes.add(new BoneSpike(point, lingerTicks)));
        }

        for (Player enemy : UtilPlayer.getNearbyEnemies(player, origin, radius)) {
            if (!UtilBlock.isGrounded(enemy)) continue;

            championsManager.getEffects().addEffect(enemy, player, EffectTypes.ROOTED, getName(), level, durationMillis);

            // A denser thicket around whoever got caught, so being held is obvious to both sides
            for (int i = 0; i < caughtSpikes; i++) {
                groundPointNear(enemy.getLocation(), caughtSpikeRadius).ifPresent(point -> spikes.add(new BoneSpike(point, lingerTicks)));
            }

            UtilMessage.message(enemy, getClassType().getDisplayName(), "champions.skill.hit-by",
                    Component.text(player.getName(), NamedTextColor.YELLOW), getDisplayName().color(NamedTextColor.GREEN));
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.hit-target",
                    Component.text(enemy.getName(), NamedTextColor.YELLOW), getDisplayName().color(NamedTextColor.GREEN));
        }
    }

    /**
     * @return a surface location uniformly scattered across the disc around {@code center}, or empty if
     * there is no reachable ground there
     */
    private Optional<Location> groundPointNear(Location center, double radius) {
        final double angle = UtilMath.RANDOM.nextDouble() * Math.PI * 2;
        final double distance = Math.sqrt(UtilMath.RANDOM.nextDouble()) * radius;
        final Location offset = center.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        return UtilLocation.getClosestSurfaceBlock(offset, 3.0, true).map(location -> location.add(0, 1.1, 0));
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 6.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.5, Double.class);
        baseDuration = getConfig("baseDuration", 1.5, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.25, Double.class);
        telegraphDuration = getConfig("telegraphDuration", 0.6, Double.class);
        caughtSpikeRadius = getConfig("caughtSpikeRadius", 1.3, Double.class);
        fieldSpikes = getConfig("fieldSpikes", 26, Integer.class);
        caughtSpikes = getConfig("caughtSpikes", 8, Integer.class);
    }
}
