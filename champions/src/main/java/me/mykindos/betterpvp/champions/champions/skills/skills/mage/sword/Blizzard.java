package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Blizzard extends ChannelSkill implements InteractSkill, EnergySkill {

    private final WeakHashMap<Snowball, Player> snow = new WeakHashMap<>();

    private double baseSlowDuration;

    private double slowDurationIncreasePerLevel;

    private int slowStrength;

    @Inject
    public Blizzard(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Blizzard";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Sword to channel.",
                "",
                "While channeling, release a blizzard",
                "that gives <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength + 1) + "</effect> to anyone hit ",
                "for <stat>" + getSlowDuration(level) + "</stat> seconds",
                "",
                "Energy: <val>" + getEnergy(level)
        };
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + level * slowDurationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {

        return SkillType.SWORD;
    }


    @Override
    public float getEnergy(int level) {

        return energy - ((level - 1));
    }


    @EventHandler
    public void onHit(CustomDamageEvent event) {
        if (event.getProjectile() instanceof Snowball snowball) {
            if (snow.containsKey(snowball)) {
                LivingEntity damagee = event.getDamagee();

                if (damagee.hasPotionEffect(PotionEffectType.SLOW)) {
                    damagee.removePotionEffect(PotionEffectType.SLOW);
                }

                int level = getLevel((Player) event.getDamager());

                damagee.setVelocity(event.getProjectile().getVelocity().multiply(0.1).add(new Vector(0, 0.25, 0)));
                damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) getSlowDuration(level), slowStrength));

                event.cancel("Snowball");
                snow.remove(snowball);
            }

        }

    }

    @UpdateEvent
    public void onUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (active.contains(player.getUniqueId())) {
                if (player.isHandRaised()) {
                    int level = getLevel(player);
                    if (level <= 0) {
                        active.remove(player.getUniqueId());
                    } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 4, true)) {
                        active.remove(player.getUniqueId());
                    } else if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                        active.remove(player.getUniqueId());
                    } else {
                        Snowball s = player.launchProjectile(Snowball.class);
                        s.getLocation().add(0, 1, 0);
                        s.setVelocity(player.getLocation().getDirection().add(new Vector(UtilMath.randDouble(-0.3, 0.3), UtilMath.randDouble(-0.2, 0.4), UtilMath.randDouble(-0.3, 0.3))));
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SNOW_STEP, 1f, 0.4f);
                        snow.put(s, player);
                    }
                } else {
                    active.remove(player.getUniqueId());
                }
            }
        }
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    public void loadSkillConfig() {
        baseSlowDuration = getConfig("baseSlowDuration", 2.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slowStrength = getConfig("slowStrength", 2, Integer.class);
    }
}
