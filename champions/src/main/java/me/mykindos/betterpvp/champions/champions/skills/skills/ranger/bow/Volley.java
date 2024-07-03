package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

@Singleton
@BPvPListener
public class Volley extends PrepareArrowSkill implements OffensiveSkill {

    private int baseNumArrows;
    private int numArrowsIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;

    @Inject
    public Volley(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Volley";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next shot shoots a volley",
                "of arrows in the direction",
                "you are facing",
                "",
                "Each arrow will deal " + getValueString(this::getDamage, level) + " damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public int getNumArrows(int level) {
        return baseNumArrows + ((level - 1) * numArrowsIncreasePerLevel);
    }

    private double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
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
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        event.setCancelled(true);

        Location location = player.getLocation();
        Vector vector;

        for (int i = 0; i < getNumArrows(level); i += 2) {
            Arrow n = player.launchProjectile(Arrow.class);
            n.setShooter(player);
            location.setYaw(location.getYaw() + i);
            vector = location.getDirection();
            n.setVelocity(vector.multiply(2));
            arrows.add(n);
        }
        location = player.getLocation();
        for (int i = 0; i < getNumArrows(level); i += 2) {
            Arrow n = player.launchProjectile(Arrow.class);
            n.setShooter(player);
            location.setYaw(location.getYaw() - i);
            vector = location.getDirection();
            n.setVelocity(vector.multiply(2));
            arrows.add(n);
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 3F, 1F);
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        // Do nothing
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.CRIT)
                .location(location)
                .count(1)
                .offset(0, 0, 0)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if(!(event.getDamager() instanceof Player player)) return;
        if(!(event.getProjectile() instanceof Arrow arrow)) return;
        if(!arrows.contains(arrow)) return;


        event.setDamage(getDamage(getLevel(player)));
        event.addReason(getName());
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseNumArrows = getConfig("baseNumArrows", 10, Integer.class);
        numArrowsIncreasePerLevel = getConfig("numArrowsIncreasePerLevel", 0, Integer.class);
        baseDamage = getConfig("baseDamage", 8.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
    }
}
