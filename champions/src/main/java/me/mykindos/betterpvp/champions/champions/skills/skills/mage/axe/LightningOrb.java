package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class LightningOrb extends Skill implements InteractSkill, CooldownSkill, Listener, ThrowableListener {

    private int maxTargets;
    private double baseSpreadDistance;

    private double spreadDistanceIncreasePerLevel;

    private double baseSlowDuration;

    private double slowDurationIncreasePerLevel;

    private int slowStrength;
    private double baseShockDuration;

    private double shockDurationIncreasePerLevel;

    private double baseDamage;

    private double damageIncreasePerLevel;

    @Inject
    public LightningOrb(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Lightning Orb";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Launch an electric orb that upon directly hitting a player",
                "will strike up to <stat>" + maxTargets + "</stat> targets within <val>" + getSpreadDistance(level) + "</val> blocks",
                "with lightning, dealing <stat>" + getDamage(level) + "</stat> damage, <effect>Shocking</effect> them for <stat>" + getShockDuration(level) + "</stat> seconds, and",
                "giving them <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength + 1) + "</effect> for <stat>" + getSlowDuration(level) + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }
    
    public double getSpreadDistance(int level) {
        return baseSpreadDistance + level * spreadDistanceIncreasePerLevel;
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + ((level-1) * slowDurationIncreasePerLevel);
    }

    public double getShockDuration(int level) {
        return baseShockDuration + ((level-1) * shockDurationIncreasePerLevel);
    }

    public double getDamage(int level) {
        return baseDamage + ((level-1) * damageIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level-1) * cooldownDecreasePerLevel);
    }


    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        Player playerThrower = (Player) thrower;

        int level = getLevel(playerThrower);
        if (level > 0) {
            int count = 0;
            for (LivingEntity ent : UtilEntity.getNearbyEnemies(playerThrower, throwableItem.getItem().getLocation(), getSpreadDistance(level))) {

                if (count >= maxTargets) continue;
                throwableItem.getImmunes().add(ent);
                if (ent instanceof Player target) {
                    championsManager.getEffects().addEffect(target, EffectType.SHOCK, (long) (getShockDuration(level) * 1000));
                }

                ent.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getSlowDuration(level) * 20), slowStrength));

                playerThrower.getLocation().getWorld().strikeLightning(ent.getLocation());
                UtilDamage.doCustomDamage(new CustomDamageEvent(ent, playerThrower, null, DamageCause.CUSTOM, getDamage(level), false, getName()));

                //ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.0F);
                count++;
            }
        }

        throwableItem.getItem().remove();
    }


    @Override
    public void activate(Player player, int level) {
        Item orb = player.getWorld().dropItem(player.getEyeLocation().add(player.getLocation().getDirection()), new ItemStack(Material.DIAMOND_BLOCK));
        orb.setVelocity(player.getLocation().getDirection());
        orb.setCanPlayerPickup(false);
        orb.setCanMobPickup(false);
        ThrowableItem throwableItem = new ThrowableItem(this, orb, player, "Lightning Orb", 10000, false);
        championsManager.getThrowables().addThrowable(throwableItem);
    }

    @Override
    public void loadSkillConfig() {
        maxTargets = getConfig("maxTargets", 3, Integer.class);
        baseSpreadDistance = getConfig("spreadDistance", 3.0, Double.class);
        spreadDistanceIncreasePerLevel = getConfig("spreadDistanceIncreasePerLevel", 0.5, Double.class);

        baseSlowDuration = getConfig("slowDuration", 4.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slowStrength = getConfig("slowStrength", 1, Integer.class);

        baseShockDuration = getConfig("baseShockDuration", 2.0, Double.class);
        shockDurationIncreasePerLevel = getConfig("shockDurationIncreasePerLevel", 0.0, Double.class);

        baseDamage = getConfig("baseDamage", 11.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

}
