package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

@Singleton
@BPvPListener
public class Sever extends Skill implements CooldownSkill, Listener, InteractSkill {
    private double baseDuration;
    private double durationIncreasePerLevel;
    private double hitDistance;
    private double degrees;
    private double degreesIncreasePerLevel;

    @Inject
    public Sever(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Sever";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Sever the air in front of you, giving anything",
                "within <val>"+ getDegrees(level) + "</val> degrees <effect>Bleed</effect> for <stat>" + getDuration(level) + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getDegrees(int level){
        return degrees + ((level - 1) * degreesIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void activate(Player player, int level) {
        if (level <= 0) {
            return;
        }

        Vector directionVector = player.getLocation().getDirection().normalize().multiply(hitDistance / 2);
        Location midpointLocation = player.getLocation().clone().add(directionVector);
        Location playerChestLocation = player.getLocation().clone().add(0, 1, 0);

        drawParticleLine(playerChestLocation, directionVector, hitDistance, player);

        // use this to get make sure the entities are enemies
        List<KeyValue<LivingEntity, EntityProperty>> keyValueList = UtilEntity.getNearbyEntities(player, midpointLocation, hitDistance / 2, EntityProperty.FRIENDLY);

        // extract LivingEntity objects from the key values
        List<LivingEntity> nearbyEntities = keyValueList.stream()
                .map(KeyValue::getKey) // get each key from list
                .collect(Collectors.toList()); // collect to list

        Vector playerDirection = player.getLocation().getDirection().normalize();

        // filter entities not within the specified angle
        nearbyEntities.removeIf(entity -> {
            Vector toEntity = entity.getLocation().subtract(player.getLocation()).toVector().normalize();
            double angle = toEntity.angle(playerDirection);
            return Math.toDegrees(angle) > getDegrees(level);
        });

        // apply effects to remaining entities
        for (LivingEntity target : nearbyEntities) {
            championsManager.getEffects().addEffect(target, EffectType.BLEED, (long) getDuration(level) * 1000L);
            UtilMessage.simpleMessage(player, getClassType().getName(), "You severed <alt>" + target.getName() + "</alt>.");
            UtilMessage.simpleMessage(target, getClassType().getName(), "You have been severed by <alt>" + player.getName() + "</alt>.");

        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 1.5F);
    }

    private void drawParticleLine(Location startLocation, Vector directionVector, double distance, Player player) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 0.5f);
        int points = (int) (distance * 15);
        for (int i = 0; i <= points; i++) {
            double increment = i / (double) points;
            Location point = startLocation.clone().add(directionVector.clone().multiply(increment));
            player.getWorld().spawnParticle(Particle.REDSTONE, point, 1, dustOptions);
        }
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);
        hitDistance = getConfig("hitDistance", 5.0, Double.class);
        degrees = getConfig("degrees", 15.0, Double.class);
        degreesIncreasePerLevel = getConfig("degreesIncreasePerLevel", 15.0, Double.class);
    }
}
