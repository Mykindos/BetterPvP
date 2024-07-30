package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class TetherShot extends PrepareArrowSkill implements InteractSkill, CooldownSkill, Listener, DebuffSkill, OffensiveSkill {

    private final Map<UUID, Arrow> tetherArrows = new HashMap<>();


    private double baseDuration;
    private double durationIncreasePerLevel;
    private double radius;

    @Inject
    public TetherShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Tether Shot";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow will ensnare enemies within x radius",
                ",preventing them from escaping. ",
                "",
                "if they use a movement ability to escape, the tether will snap",
                "dealing x damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * (level - 1));
    }

    public double getRadius(int level){
        return radius;
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
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!hasSkill(player)) return;
        if (!tetherArrows.containsValue(arrow)) return;

        int level = getLevel(player);
        Location arrowLocation = arrow.getLocation();

        player.getWorld().playSound(arrowLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 2.0F);
        doTether(player, arrowLocation, level);

        tetherArrows.remove(player.getUniqueId());
        arrow.remove();
    }

    @Override
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        tetherArrows.put(player.getUniqueId(), arrow);
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        //ignore
    }

    public void doTether(Player player, Location arrowLocation, int level){
        List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, arrowLocation, getRadius(level));



    }

    @UpdateEvent
    public void checkTether(){
        /*AttributeInstance attribute = livingEntity.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        if(attribute != null) {
            attribute.setBaseValue(0);
        }
        AttributeInstance attribute2 = livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if(attribute2 != null) {
            attribute2.setBaseValue(0);
        }*/
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.SCULK_CHARGE_POP)
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
        baseDuration = getConfig("baseDuration", 1.5, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.5, Double.class);
        radius = getConfig("radius", 4.0, Double.class);
    }
}
