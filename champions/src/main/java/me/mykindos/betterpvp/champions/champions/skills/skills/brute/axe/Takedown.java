package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Takedown extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();
    private double damage;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private int slownessStrength;
    private double recoilDamage;
    private double recoilDamageIncreasePerLevel;
    private double damageIncreasePerLevel;

    @Inject
    public Takedown(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Takedown";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Hurl yourself forwards, dealing <val>" + getDamage(level) + "</val> damage,",
                "taking <val>" + getRecoilDamage(level)+"</val> damage, and applying <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength + 1) + "</effect>",
                "to yourself and the target for <val>" + getDuration(level) + "</val> seconds",
                "",
                "Cannot be used while grounded",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDamage(int level){
        return damage + (level * damageIncreasePerLevel);
    }

    public double getRecoilDamage(int level){
        return recoilDamage + level * recoilDamageIncreasePerLevel;
    }

    public double getDuration(int level) {
        return baseDuration + level * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }


    @UpdateEvent
    public void checkCollision() {

        Iterator<Entry<Player, Long>> it = active.entrySet().iterator();
        while (it.hasNext()) {

            Entry<Player, Long> next = it.next();
            Player player = next.getKey();
            if (player.isDead()) {
                it.remove();
                continue;
            }

            if(isCollision(player)) {
                it.remove();
                continue;
            }


            if (UtilBlock.isGrounded(player)) {
                if (UtilTime.elapsed(next.getValue(), 750L)) {
                    it.remove();
                }
            }
        }

    }

    public boolean isCollision(Player player) {
        for (LivingEntity other : UtilEntity.getNearbyEnemies(player, player.getLocation(), 1.5)) {
            if (other.isDead()) continue;

            if (UtilMath.offset(player, other) < 1.5) {

                doTakedown(player, other);
                return true;

            }
        }

        return false;
    }

    public void doTakedown(Player player, LivingEntity target) {
        int level = getLevel(player);

        UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt>" + target.getName() + "</alt> with <alt>" + getName());

        UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt>" + target.getName() + "</alt> with <alt>" + getName());
        UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null, DamageCause.CUSTOM, damage, false, "Takedown"));

        UtilMessage.simpleMessage(target, getClassType().getName(), "<alt>" + player.getName() + "</alt> hit you with <alt>" + getName());
        UtilDamage.doCustomDamage(new CustomDamageEvent(player, target, null, DamageCause.CUSTOM, damage, false, "Takedown Recoil"));

        PotionEffect pot = new PotionEffect(PotionEffectType.SLOW, (int) (getDuration(getLevel(player))) * 20, slownessStrength);
        championsManager.getEffects().addEffect(player, EffectType.NO_JUMP, (long) ((baseDuration + (level * durationIncreasePerLevel)) * 1000L));
        championsManager.getEffects().addEffect(target, EffectType.NO_JUMP, (long) ((baseDuration + (level * durationIncreasePerLevel)) * 1000L));
        player.addPotionEffect(pot);
        target.addPotionEffect(pot);
    }

    @Override
    public boolean canUse(Player p) {

        if (UtilBlock.isGrounded(p)) {
            UtilMessage.simpleMessage(p, getClassType().getName(), "You cannot use <alt>" + getName() + "</alt> while grounded.");
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player, int leel) {
        Vector vec = player.getLocation().getDirection();
        VelocityData velocityData = new VelocityData(vec, 1.8D, false, 0.0D, 0.4D, 0.6D, false);
        UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);
        active.put(player, System.currentTimeMillis());
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        damage = getConfig("damage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 3, Integer.class);
        recoilDamage = getConfig("recoilDamage", 1.5, Double.class);
        recoilDamageIncreasePerLevel = getConfig("recoilDamageIncreasePerLevel", 0.5, Double.class);
    }
}
