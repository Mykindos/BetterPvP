package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data.BloodSphereProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
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
public class BloodSphere extends Skill implements CooldownSkill, InteractSkill, Listener {

    private final WeakHashMap<Player, BloodSphereProjectile> projectiles = new WeakHashMap<>();

    private float growthPerSecond;
    private float growthPerSecondIncreasePerLevel;
    private double expireSeconds;
    private double expireSecondsIncreasePerLevel;
    private double applyRadius;
    private double applyRadiusIncreasePerLevel;
    private double passiveTravelSpeed;
    private double applyTravelSpeed;
    private double damagePerSecond;
    private double damageIncreasePerLevel;
    private double regenPerSecond;
    private double regenIncreasePerLevel;
    private double impactHealthMultiplier;
    private double impactHealthMultiplierIncreasePerLevel;
    private double healthSeconds;
    private double mobHealthModifier;

    @Inject
    private BloodSphere(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Right click with a Sword to activate",
                "Right click again to recall the orb",
                "",
                "Launch an orb that deals <val>" + UtilFormat.formatNumber(getDamagePerSecond(level)) + "</val> max",
                "damage to all enemies within <val>" + UtilFormat.formatNumber(getRadius(level)) + "</val> blocks.",
                "",
                "For the damage dealt, heal your",
                "allies for a max of <val>" + UtilFormat.formatNumber(getMaxHealthPerSecond(level)) + "</val> health per",
                "second.",
                "",
                "Upon recalling your orb, heal for",
                "<val>" + UtilFormat.formatNumber(getImpactHealthMultiplier(level) * 100) + "%</val> of all damage dealt.",
                "",
                "Cooldown: <val>" + getCooldown(level) + "</val> seconds."
        };
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1) * cooldownDecreasePerLevel;
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
    public void activate(Player player, int level) {
        BloodSphereProjectile projectile = new BloodSphereProjectile(player,
                0.6,
                0.5,
                player.getEyeLocation(),
                (long) ((expireSeconds + (expireSecondsIncreasePerLevel * (level - 1))) * 1000d),
                getGrowthPerSecond(level),
                getMaxHealthPerSecond(level),
                getDamagePerSecond(level),
                getRadius(level),
                getImpactHealthMultiplier(level),
                passiveTravelSpeed,
                applyTravelSpeed,
                healthSeconds,
                mobHealthModifier);
        projectile.redirect(player.getLocation().getDirection());
        projectiles.put(player, projectile);
        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>%s %d</alt>.", getName(), level);
    }

    private float getGrowthPerSecond(int level) {
        return growthPerSecond + (growthPerSecondIncreasePerLevel * (level - 1));
    }

    private double getImpactHealthMultiplier(int level) {
        return impactHealthMultiplier + (impactHealthMultiplierIncreasePerLevel * (level - 1));
    }

    private double getRadius(int level) {
        return applyRadius + (applyRadiusIncreasePerLevel * (level - 1));
    }

    private double getDamagePerSecond(int level) {
        return damagePerSecond + (damageIncreasePerLevel * (level - 1));
    }

    private double getMaxHealthPerSecond(int level) {
        return regenPerSecond + (regenIncreasePerLevel * (level - 1));
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
        this.growthPerSecond = getConfig("growthPerSecond", 0.25f, Float.class);
        this.growthPerSecondIncreasePerLevel = getConfig("growthPerSecondIncreasePerLevel", 0.1f, Float.class);
        this.expireSeconds = getConfig("expireSeconds", 5.0, Double.class);
        this.expireSecondsIncreasePerLevel = getConfig("expireSecondsIncreasePerLevel", 2.5, Double.class);
        this.applyRadius = getConfig("applyRadius", 2.0, Double.class);
        this.applyRadiusIncreasePerLevel = getConfig("applyRadiusIncreasePerLevel", 1.0, Double.class);
        this.passiveTravelSpeed = getConfig("passiveTravelSpeed", 0.8, Double.class);
        this.applyTravelSpeed = getConfig("applyTravelSpeed", 0.5, Double.class);
        this.damagePerSecond = getConfig("damagePerSecond", 0.5, Double.class);
        this.damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        this.regenPerSecond = getConfig("regenPerSecond", 1.0, Double.class);
        this.regenIncreasePerLevel = getConfig("regenIncreasePerLevel", 0.5, Double.class);
        this.impactHealthMultiplier = getConfig("impactHealthMultiplier", 0.3, Double.class);
        this.impactHealthMultiplierIncreasePerLevel = getConfig("impactHealthMultiplierIncreasePerLevel", 0.1, Double.class);
        this.healthSeconds = getConfig("healthSeconds", 0.5, Double.class);
        this.mobHealthModifier = getConfig("mobHealthModifier", 0.5, Double.class);
    }
}
