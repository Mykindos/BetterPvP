package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.brute.data.ThreateningShoutData;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class ThreateningShout extends Skill implements Listener, CooldownToggleSkill, AreaOfEffectSkill, OffensiveSkill {
    private final Map<Player, ThreateningShoutData> playerDataMap = new WeakHashMap<>();

    private int damageMultiplier;
    private int damageMultiplierIncreasePerLevel;
    private double damage;
    private double damageIncreasePerLevel;
    private double radius;
    private int tickDelay;
    private double startDistance;
    private int distance;

    @Inject
    public ThreateningShout(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    public int getDamageMultiplier(int level) {
        // This is an int because it's hard to format a decimal multiplier in the description, and it doesn't make much
        // sense to have a decimal multiplier for this skill.

        return damageMultiplier + ((level - 1) * damageMultiplierIncreasePerLevel);
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Release a roar, dealing " + getValueString(this::getDamage, level),
                "damage.",
                "",
                "Enemies below half health take " + getValueString(this::getDamageMultiplier, level, 1, "", 0) + "x",
                "more damage.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    @Override
    public void toggle(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 2.0F);

        final @NotNull Location start = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(startDistance));
        final List<Location> points = new ArrayList<>();

        for (int i = 0; i < distance; i++) {
            final @NotNull Location point = start.clone().add(start.getDirection().normalize().multiply(i * 1.0));
            final @NotNull Block targetBlock = point.getBlock();
            if (!UtilBlock.airFoliage(targetBlock)) break;

            points.add(point);
        }

        final ThreateningShoutData data = new ThreateningShoutData(points, 0, new HashSet<>(), new HashSet<>());
        playerDataMap.put(player, data);
    }

    @UpdateEvent
    public void onUpdate() {
        final Iterator<Map.Entry<Player, ThreateningShoutData>> iterator = playerDataMap.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, ThreateningShoutData> entry = iterator.next();

            final ThreateningShoutData data = entry.getValue();
            final List<Location> points = data.getPoints();

            int currentPointIndex = data.getPointIndex();
            if (points.isEmpty() || currentPointIndex >= points.size()) {
                iterator.remove();
                continue;
            }

            final @NotNull Player player = entry.getKey();
            final int level = getLevel(player);
            final Set<LivingEntity> damagedEntities = data.getDamagedEntities();

            for (int i = 0; i < 2 && currentPointIndex < points.size(); i++) {
                final @NotNull Location point = points.get(currentPointIndex);

                point.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 0, 0, 0, 0, 0);
                doDamageToEnemies(player, point, damagedEntities, level);

                currentPointIndex++;
            }

            data.setPointIndex(currentPointIndex);
        }
    }

    private void doDamageToEnemies(@NotNull Player player, @NotNull Location point, @NotNull Set<LivingEntity> damagedEntities, int level) {
        UtilServer.runTaskLater(champions, () -> {
            for (LivingEntity target : UtilEntity.getNearbyEnemies(player, point, radius)) {
                if (damagedEntities.contains(target)) continue;

                final double maxHealth = Objects.requireNonNull(target.getAttribute(Attribute.MAX_HEALTH)).getValue();
                final double currentHealth = target.getHealth();
                final double healthPercentage = currentHealth / maxHealth;

                double damageToDeal = getDamage(level);
                if (healthPercentage < 0.5) {
                    damageToDeal *= getDamageMultiplier(level);

                    target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 50,
                            0.5, 0.5, 0.5,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 128), 1));

                     target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 2F);
                }

                UtilDamage.doDamage(new DamageEvent(target, player, null,
                        new SkillDamageCause(ThreateningShout.this), damageToDeal, getName()));

                UtilMessage.simpleMessage(player, getName(), "You hit <yellow>%s</yellow> with <green>%s</green>",
                        target.getName(), getName());

                damagedEntities.add(target);
            }
        }, tickDelay);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public String getName() {
        return "Threatening Shout";
    }

    @Override
    public void loadSkillConfig() {
        damageMultiplier = getConfig("damageMultiplier", 3, Integer.class);
        damageMultiplierIncreasePerLevel = getConfig("damageMultiplierIncreasePerLevel", 0, Integer.class);
        radius = getConfig("radius", 1.5, Double.class);
        tickDelay = getConfig("tickDelay", 12, Integer.class);
        damage = getConfig("damage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        startDistance = getConfig("startDistance", 1.0, Double.class);
        distance = getConfig("distance", 15, Integer.class);
    }
}
