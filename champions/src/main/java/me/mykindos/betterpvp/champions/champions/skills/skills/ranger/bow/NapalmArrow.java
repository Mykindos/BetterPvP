package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;


@Singleton
@BPvPListener
public class NapalmArrow extends PrepareArrowSkill implements ThrowableListener, FireSkill, OffensiveSkill {

    @Getter
    private double burnDuration;
    @Getter
    private double damage;
    @Getter
    private double duration;

    private double velocityMultiplier;
    private double yComponentVelocityMultiplier;
    private int damageDelay;
    private int numFlames;
    private final WeakHashMap<UUID, Arrow> napalmArrows = new WeakHashMap<>();
    private final Random random = new Random();

    @Inject
    public NapalmArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Napalm Arrow";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an ignited arrow that explodes on the ground",
                "into a field of napalm that lasts <val>" + getDuration() + "</val> seconds",
                "and <effect>Ignites</effect> anyone inside for <val>" + getBurnDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown() + "</val> seconds"
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    private void doNapalm(Player player, Location arrowLocation) {
        World world = arrowLocation.getWorld();
        List<Item> fireItems = new ArrayList<>();

        for (int i = 0; i < numFlames; i++) {
            Item fire = world.dropItem(arrowLocation.add(0.0D, 0.0D, 0.0D), new ItemStack(Material.BLAZE_POWDER));
            ThrowableItem throwableItem = new ThrowableItem(this, fire, player, getName(), (long) (getDuration() * 1000L));
            throwableItem.setRemoveInWater(true);
            championsManager.getThrowables().addThrowable(throwableItem);

            double x = (random.nextDouble() - 0.5) * velocityMultiplier;
            double y = random.nextDouble() * yComponentVelocityMultiplier;
            double z = (random.nextDouble() - 0.5) * velocityMultiplier;
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
                    Item randomFire = fireItems.get(random.nextInt(fireItems.size()));
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

        if (hit.equals(thrower)) {
            return;
        }

        if (thrower instanceof Player damager) {
            UtilEntity.setFire(hit, damager, (long) getBurnDuration() * 1000L);

            CustomDamageEvent cde = new CustomDamageEvent(hit, damager, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(), false, "Napalm");
            cde.setDamageDelay(damageDelay);
            UtilDamage.doCustomDamage(cde);
        }
    }

    @Override
    public void activate(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public void onHit(Player damager, LivingEntity target) {
        //ignore
    }

    @EventHandler
    public void onArrowDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!napalmArrows.containsValue(arrow)) return;

        if (hasSkill(player)) {
            UtilServer.runTaskLater(champions, () -> event.getDamagee().setFireTicks((int) (getBurnDuration() * 20)), 1);
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!napalmArrows.containsValue(arrow)) return;

        if (hasSkill(player)) {
            Location arrowLocation = arrow.getLocation();

            player.getWorld().playSound(arrowLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 2.0F);
            doNapalm(player, arrowLocation);

            arrow.remove();
            UtilServer.runTaskLater(champions, () -> napalmArrows.remove(player.getUniqueId()), 1);
        }
    }

    @Override
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, Arrow arrow) {
        napalmArrows.put(player.getUniqueId(), arrow);
        arrow.setFireTicks(200);
    }

    @UpdateEvent
    public void updateArrowTrail() {
        Iterator<Map.Entry<UUID, Arrow>> iterator = napalmArrows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Arrow> entry = iterator.next();
            Arrow arrow = entry.getValue();
            if (arrow.isDead() || !arrow.isValid()) {
                iterator.remove();
            } else {
                displayTrail(arrow.getLocation());
            }
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
    public void loadSkillConfig() {
        burnDuration = getConfig("burnDuration", 1.0, Double.class);
        damage = getConfig("damage", 1.0, Double.class);
        duration = getConfig("duration", 3.0, Double.class);
        velocityMultiplier = getConfig("velocityMultiplier", 0.4, Double.class);
        yComponentVelocityMultiplier = getConfig("yComponentVelocityMultiplier", 1.0, Double.class);
        damageDelay = getConfig("damageDelay", 50, Integer.class);
        numFlames = getConfig("numFlames", 75, Integer.class);
    }
}