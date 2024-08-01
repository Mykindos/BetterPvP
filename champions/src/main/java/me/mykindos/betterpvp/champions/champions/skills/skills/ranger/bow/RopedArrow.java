package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scheduler.BPVPTask;
import me.mykindos.betterpvp.core.scheduler.TaskScheduler;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class RopedArrow extends Skill implements InteractSkill, CooldownSkill, Listener, MovementSkill {

    private final TaskScheduler taskScheduler;

    private double fallDamageLimit;
    private double velocityStrength;
    private final WeakHashMap<Arrow, Player> arrows = new WeakHashMap<>();
    private final WeakHashMap<Arrow, ArmorStand> arrowArmorStands = new WeakHashMap<>();

    @Inject
    public RopedArrow(Champions champions, ChampionsManager championsManager, TaskScheduler taskScheduler) {
        super(champions, championsManager);
        this.taskScheduler = taskScheduler;
    }

    @Override
    public String getName() {
        return "Roped Arrow";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to activate",
                "",
                "Your next arrow will pull you",
                "towards the location it hits",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
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

    @Override
    public boolean canUse(Player player) {
        if (!UtilInventory.contains(player, Material.ARROW, 1)) {
            UtilMessage.message(player, getName(), "You need at least <alt2>1 Arrow</alt2> to use this skill.");
            return false;
        }

        return super.canUse(player);
    }

    @Override
    public void activate(Player player, int level) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            UtilInventory.remove(player, Material.ARROW, 1);
        }

        Arrow proj = player.launchProjectile(Arrow.class);
        proj.setShooter(player);
        arrows.put(proj, player);

        // Create an invisible entity at the arrow's location
        List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, proj.getLocation(), 20.0);

        // Create an invisible entity at the arrow's location
        ArmorStand center = proj.getWorld().spawn(proj.getLocation(), ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
            stand.setMarker(true);
        });

        enemies.add(player);

        // Attach leads from enemies to the invisible entity
        for (LivingEntity enemy : enemies) {
            center.setLeashHolder(enemy);
        }
        center.setLeashHolder(player);

        proj.addPassenger(center);

        arrowArmorStands.put(proj, center);

        proj.setVelocity(player.getLocation().getDirection().multiply(1.6D));
        player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
        player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!arrows.containsKey(arrow)) return;
        if (!hasSkill(player)) return;

        Vector vec = UtilVelocity.getTrajectory(player, arrow);

        VelocityData velocityData = new VelocityData(vec, velocityStrength, false, 0.0D, 0.5D, 1.2D, true);
        UtilVelocity.velocity(player, null, velocityData);

        arrow.getWorld().playSound(arrow.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);

        // Remove the armor stand and lead
        ArmorStand armorStand = arrowArmorStands.remove(arrow);
        if (armorStand != null) {
            armorStand.remove();
        }

        arrows.remove(arrow);

        taskScheduler.addTask(new BPVPTask(player.getUniqueId(), uuid -> !UtilBlock.isGrounded(uuid), uuid -> {
            Player target = Bukkit.getPlayer(uuid);
            if(target != null) {
                championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL,getName(), (int) fallDamageLimit,
                        50L, true, true, UtilBlock::isGrounded);
            }
        }, 1000));

    }

    @UpdateEvent
    public void onTick() {
        arrows.entrySet().removeIf(entry -> {
            final Arrow arrow = entry.getKey();
            final Player shooter = entry.getValue();
            if (arrow.isDead() || arrow.isOnGround() || shooter == null || !shooter.isOnline()) {
                ArmorStand armorStand = arrowArmorStands.remove(arrow);
                if (armorStand != null) {
                    armorStand.remove();
                }
                return true;
            }

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1);
            new ParticleBuilder(Particle.DUST)
                    .location(arrow.getLocation())
                    .count(1)
                    .offset(0.1, 0.1, 0.1)
                    .extra(0)
                    .receivers(60)
                    .data(dustOptions)
                    .spawn();

            return false;
        });
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1) * cooldownDecreasePerLevel;
    }

    @Override
    public void loadSkillConfig() {
        fallDamageLimit = getConfig("fallDamageLimit", 8.0, Double.class);
        velocityStrength = getConfig("velocityStrength", 1.6, Double.class);
    }
}
