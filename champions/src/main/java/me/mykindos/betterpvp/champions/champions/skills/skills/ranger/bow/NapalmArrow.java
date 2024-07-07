package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

@Singleton
@BPvPListener
public class NapalmArrow extends PrepareArrowSkill implements ThrowableListener, FireSkill, OffensiveSkill {

    private double baseBurnDuration;
    private double burnDurationIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private double velocityMultiplier;
    private double yComponentVelocityMultiplier;
    private int damageDelay;
    private int numFlames;
    private final Map<UUID, Arrow> napalmArrows = new HashMap<>();

    @Inject
    public NapalmArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Napalm Arrow";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an ignited arrow that explodes on the ground",
                "into a field of napalm that lasts " + getValueString(this::getDuration, level) + " seconds",
                "and <effect>Ignites</effect> anyone inside for " + getValueString(this::getBurnDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level) + " seconds"
        };
    }

    public double getBurnDuration(int level) {
        return baseBurnDuration + ((level - 1) * burnDurationIncreasePerLevel);
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    private void doNapalm(Player player, Location arrowLocation, int level) {
        World world = arrowLocation.getWorld();
        List<Item> fireItems = new ArrayList<>();

        for (int i = 0; i < numFlames; i++) {
            Item fire = world.dropItem(arrowLocation.add(0.0D, 0.0D, 0.0D), new ItemStack(Material.BLAZE_POWDER));
            ThrowableItem throwableItem = new ThrowableItem(this, fire, player, getName(), (long) (getDuration(level) * 1000L));
            championsManager.getThrowables().addThrowable(throwableItem);

            double x = (Math.random() - 0.5) * velocityMultiplier;
            double y = Math.random() * yComponentVelocityMultiplier;
            double z = (Math.random() - 0.5) * velocityMultiplier;
            fire.setVelocity(new Vector(x, y, z));

            fireItems.add(fire);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (fireItems.isEmpty()) {
                    this.cancel();
                    return;
                }

                fireItems.removeIf(item -> item.isDead() || !item.isValid());

                if (!fireItems.isEmpty()) {
                    Item randomFire = fireItems.get(new Random().nextInt(fireItems.size()));
                    world.spawnParticle(Particle.LAVA, randomFire.getLocation(), 0);
                }
            }
        }.runTaskTimer(champions, 0L, 1L);
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        if (hit instanceof ArmorStand) {
            return;
        }

        Item fireItem = throwableItem.getItem();
        if (fireItem != null) {
            fireItem.remove();
        }

        if (thrower instanceof Player damager) {
            int level = getLevel(damager);
            hit.setFireTicks((int) (getBurnDuration(level) * 20));

            CustomDamageEvent cde = new CustomDamageEvent(hit, damager, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Napalm");
            cde.setDamageDelay(damageDelay);
            UtilDamage.doCustomDamage(cde);
        }
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        //ignore
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!hasSkill(player)) return;
        if (!napalmArrows.containsValue(arrow)) return;

        int level = getLevel(player);
        Location arrowLocation = arrow.getLocation();

        player.getWorld().playSound(arrowLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 2.0F);
        doNapalm(player, arrowLocation, level);

        napalmArrows.remove(player.getUniqueId());
        arrow.remove();
    }

    @Override
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        napalmArrows.put(player.getUniqueId(), arrow);
        arrow.setFireTicks(200);
    }

    @UpdateEvent
    public void updateArrowTrail() {
        for (Arrow arrow : napalmArrows.values()) {
            displayTrail(arrow.getLocation());
        }
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.FLAME)
                .location(location)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseBurnDuration = getConfig("baseBurnDuration", 1.0, Double.class);
        burnDurationIncreasePerLevel = getConfig("burnDurationIncreasePerLevel", 0.5, Double.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        baseDuration = getConfig("baseDuration", 5.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        velocityMultiplier = getConfig("velocityMultiplier", 1.0, Double.class);
        yComponentVelocityMultiplier = getConfig("yComponentVelocityMultiplier", 0.3, Double.class);
        damageDelay = getConfig("damageDelay", 0, Integer.class);
        numFlames = getConfig("numFlames", 50, Integer.class);
    }
}
