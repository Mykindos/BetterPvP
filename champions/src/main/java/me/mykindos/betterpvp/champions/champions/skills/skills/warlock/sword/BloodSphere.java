package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data.BloodSphereProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class BloodSphere extends Skill implements CooldownSkill, InteractSkill, Listener, HealthSkill, DamageSkill, OffensiveSkill, TeamSkill {

    private final WeakHashMap<Player, BloodSphereProjectile> projectiles = new WeakHashMap<>();

    private float growthPerSecond;
    private double expireSeconds;
    private double applyRadius;
    private double passiveTravelSpeed;
    private double applyTravelSpeed;
    private double damagePerSecond;
    private double maxDamage;
    private double impactHealthMultiplier;
    private double healthSeconds;
    private double mobHealthModifier;

    @Inject
    private BloodSphere(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to activate",
                "Right click again to recall the orb",
                "",
                "Launch an orb that deals <val>" + getDamagePerSecond() + "</val> damage",
                "per second. The maximum total damage",
                "this orb can deal is <val>" + getMaxDamage() + "</val>.",
                "",
                "Upon recalling your orb, heal for",
                "<val>" + UtilFormat.formatNumber(getImpactHealthMultiplier() * 100, 0) + "%</val> of all damage dealt.",
                "",
                "Cooldown: <val>" + getCooldown() + "</val> seconds."
        };
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "Blood Sphere";
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean displayWhenUsed() {
        return false;
    }

    @Override
    public boolean canUse(Player player) {
        if (projectiles.containsKey(player)) {
            final BloodSphereProjectile projectile = projectiles.get(player);
            if (!projectile.isImpacted()) {
                UtilMessage.simpleMessage(player, getName(), "You recalled <alt>%s</alt>.", getName());
                projectile.impact();
            }
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player) {
        BloodSphereProjectile projectile = new BloodSphereProjectile(player,
                0.6,
                player.getEyeLocation(),
                (long) (expireSeconds * 1000d),
                getGrowthPerSecond(),
                getMaxDamage(),
                getDamagePerSecond(),
                getRadius(),
                getImpactHealthMultiplier(),
                passiveTravelSpeed,
                applyTravelSpeed,
                healthSeconds,
                mobHealthModifier);
        projectile.redirect(player.getLocation().getDirection());
        projectiles.put(player, projectile);
        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>%s</alt>.", getName());
    }

    private float getGrowthPerSecond() {
        return growthPerSecond;
    }

    private double getImpactHealthMultiplier() {
        return impactHealthMultiplier;
    }

    private double getRadius() {
        return applyRadius;
    }

    private double getDamagePerSecond() {
        return damagePerSecond;
    }

    private double getMaxDamage() {
        return maxDamage;
    }

    @UpdateEvent
    public void ticker() {
        final Iterator<Map.Entry<Player, BloodSphereProjectile>> iterator = projectiles.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, BloodSphereProjectile> next = iterator.next();
            // Remove if player is offline
            if (next.getKey() == null || !next.getKey().isOnline()) {
                iterator.remove();
                continue;
            }

            final BloodSphereProjectile projectile = next.getValue();
            projectile.tick();
            if (projectile.isExpired() || projectile.isMarkForRemoval()) {
                iterator.remove();
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        this.growthPerSecond = getConfig("growthPerSecond", 20.0f, Float.class);
        this.expireSeconds = getConfig("expireSeconds", 3.0, Double.class);
        this.applyRadius = getConfig("applyRadius", 3.0, Double.class);
        this.passiveTravelSpeed = getConfig("passiveTravelSpeed", 0.8, Double.class);
        this.applyTravelSpeed = getConfig("applyTravelSpeed", 0.5, Double.class);
        this.damagePerSecond = getConfig("damagePerSecond", 3.0, Double.class);
        this.maxDamage = getConfig("maxDamage", 15.0, Double.class);
        this.impactHealthMultiplier = getConfig("impactHealthMultiplier", 1.0, Double.class);
        this.healthSeconds = getConfig("healthSeconds", 0.5, Double.class);
        this.mobHealthModifier = getConfig("mobHealthModifier", 0.5, Double.class);
    }
}
