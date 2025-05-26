package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Singleton
@BPvPListener
public class PopUp extends Skill implements PassiveSkill, OffensiveSkill, CrowdControlSkill, CooldownSkill, Listener {

    private final Set<UUID> playersWithChargedSkill = new HashSet<>();

    private double velocity;
    private double yMax;
    private double yAdd;
    private double ySet;

    @Inject
    public PopUp(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Pop Up";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Landing a melee hit on an airborne enemy",
                "launches them briefly upward again.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }


    @EventHandler
    public void onCustomVelocity(CustomEntityVelocityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getVelocityType() != VelocityType.KNOCKBACK && event.getVelocityType() != VelocityType.KNOCKBACK_CUSTOM) return;

        int level = getLevel(player);
        if (level <= 0) return;
        if (championsManager.getCooldowns().hasCooldown(player, getName())) return;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1f, 2f);
        spawnParticles(player);

        playersWithChargedSkill.add(player.getUniqueId());
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (event.isCancelled()) return;
        if (!playersWithChargedSkill.contains(player.getUniqueId())) return;

        boolean isPlayerGrounded = UtilBlock.isGrounded(player, 1);
        //if (isPlayerGrounded || UtilBlock.isInWater(player)) return;

        int level = getLevel(player);

        // #1 make velocity owrk #2 make it right #3 make the initial activator particles purple and make them be around you until it expires # 4 make it expire
        playersWithChargedSkill.remove(player.getUniqueId());
        championsManager.getCooldowns().use(player, getName(), getCooldown(level), false, true, isCancellable());

        Location location = player.getLocation().add(0, 1, 0);
        List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, location, 5);
        LivingEntity target = event.getDamagee();

        for (LivingEntity enemy : enemies) {
            double yTranslate = location.add(0, -1, 0).getY();
            Location enemyLocation = enemy.getLocation();
            enemyLocation.setY(yTranslate);
            Vector direction = enemyLocation.toVector().subtract(location.toVector()).normalize();
            VelocityData enemyVelocityData = new VelocityData(direction, velocity, false, ySet, yAdd, yMax, true);
            UtilVelocity.velocity(enemy, player, enemyVelocityData, VelocityType.CUSTOM);
        }

        event.addReason(getName());

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_DEFLECT, 1.0F, 1.0F);

        for (int i = 0; i < 20; i++) {
            final Location playerLoc = target.getLocation().add(0, 1, 0);
            Particle.CRIT.builder()
                    .count(3)
                    .extra(0)
                    .offset(0.4, 1.0, 0.4)
                    .location(playerLoc)
                    .receivers(60)
                    .spawn();
        }
    }


    private void spawnParticles(Player player) {
        Particle.BLOCK.builder()
                .location(player.getLocation().clone().add(0, player.getHeight()/2, 0))
                .data(Material.CHISELED_STONE_BRICKS.createBlockData())
                .receivers(32)
                .count(6)
                .spawn();
    }

    @Override
    public void loadSkillConfig() {
        velocity = getConfig("velocity", 2.0, Double.class);
        yAdd = getConfig("yAdd", 0.6, Double.class);
        yMax = getConfig("yMax", 0.8, Double.class);
        ySet = getConfig("ySet", 0.0D, Double.class);
    }
}
