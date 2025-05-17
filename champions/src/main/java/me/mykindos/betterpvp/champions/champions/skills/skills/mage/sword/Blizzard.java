package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Blizzard extends ChannelSkill implements InteractSkill, EnergyChannelSkill, CrowdControlSkill {

    private final WeakHashMap<Snowball, Player> snow = new WeakHashMap<>();

    private int slowStrength;
    private double baseSlowDuration;
    private double slowDurationIncreasePerLevel;
    private double pushForwardStrength;
    private double pushForwardIncreasePerLevel;
    private double pushUpwardStrength;
    private double pushUpwardIncreasePerLevel;
    private double pushbackVerticalStrength;
    private double pushVerticalIncreasePerLevel;
    private double pushbackHorizontalStrength;
    private double pushHorizontalIncreasePerLevel;
    private double initialEnergyCost;

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
                "Release a blizzard that freezes enemies, giving them <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect>",
                "for " + getValueString(this::getSlowDuration, level) + " seconds and pushing them back.",
                "",
                "Energy: " + getValueString(this::getEnergy, level)
        };
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + ((level - 1) * slowDurationIncreasePerLevel);
    }

    public double getPushForwardStrength(int level) {
        return pushForwardStrength + ((level - 1) * pushForwardIncreasePerLevel);
    }

    public double getPushUpwardStrength(int level) {
        return pushUpwardStrength + ((level - 1) * pushUpwardIncreasePerLevel);
    }

    public double getPushbackHorizontalStrength(int level) {
        return pushbackHorizontalStrength + ((level - 1) * pushHorizontalIncreasePerLevel);
    }

    public double getPushbackVerticalStrength(int level) {
        return pushbackVerticalStrength + ((level - 1) * pushVerticalIncreasePerLevel);
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
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @EventHandler
    public void onHit(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getProjectile() instanceof Snowball snowball) {
            if (snowball.getShooter() instanceof Player damager) {
                if (snow.containsKey(snowball)) {
                    LivingEntity damagee = event.getDamagee();

                    int level = getLevel(damager);
                    Vector direction = snowball.getVelocity().normalize();

                    final VelocityData data = new VelocityData(direction,
                            getPushForwardStrength(level),
                            true,
                            0,
                            getPushUpwardStrength(level),
                            1.0,
                            false);
                    UtilVelocity.velocity(damagee, damager, data);

                    championsManager.getEffects().addEffect(damagee,
                            event.getDamager(),
                            EffectTypes.SLOWNESS,
                            slowStrength,
                            (long) (getSlowDuration(level) * 1000));

                    event.cancel("Snowball");
                    snow.remove(snowball);
                }
            }
        }
    }

    @UpdateEvent
    public void onUpdate() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (!player.isHandRaised()) {
                iterator.remove();
                continue;
            }

            int level = getLevel(player);
            if (!hasSkill(player)) {
                iterator.remove();
            } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true)) {
                iterator.remove();
            } else if (!isHolding(player)) {
                iterator.remove();
            } else {
                Snowball s = player.launchProjectile(Snowball.class);
                s.getLocation().add(0, 1, 0);
                s.setVelocity(player.getLocation().getDirection().add(new Vector(UtilMath.randDouble(-0.1, 0.1), UtilMath.randDouble(-0.1, 0.1), UtilMath.randDouble(-0.1, 0.1))));
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SNOW_STEP, 1f, 0.4f);
                snow.put(s, player);

                Vector vector = player.getLocation().getDirection().multiply(-1).multiply(new Vector(
                        getPushbackHorizontalStrength(level),
                        getPushbackVerticalStrength(level),
                        getPushbackHorizontalStrength(level)
                ));
                final VelocityData data = new VelocityData(vector,
                        getPushbackHorizontalStrength(level),
                        false,
                        getPushUpwardStrength(level),
                        0,
                        getPushbackVerticalStrength(level),
                        true);
                UtilVelocity.velocity(player, player, data);
            }
        }
    }

    @Override
    public void activate(Player player, int level) {
        if (championsManager.getEnergy().use(player, getName(), initialEnergyCost, true)) {
            active.add(player.getUniqueId());
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        initialEnergyCost = getConfig("initialEnergyCost", 20.0, Double.class);
        baseSlowDuration = getConfig("baseSlowDuration", 2.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slowStrength = getConfig("slowStrength", 3, Integer.class);
        pushForwardStrength = getConfig("pushForwardStrength", 0.3, Double.class);
        pushUpwardStrength = getConfig("pushUpwardStrength", 0.15, Double.class);
        pushForwardIncreasePerLevel = getConfig("pushForwardIncreasePerLevel", 0.0, Double.class);
        pushUpwardIncreasePerLevel = getConfig("pushUpwardIncreasePerLevel", 0.0, Double.class);
        pushbackVerticalStrength = getConfig("pushbackVerticalStrength", 0.1, Double.class);
        pushbackHorizontalStrength = getConfig("pushbackHorizontalStrength", 0.06, Double.class);
        pushVerticalIncreasePerLevel = getConfig("pushbackVerticalIncreasePerLevel", 0.0, Double.class);
        pushHorizontalIncreasePerLevel = getConfig("pushbackHorizontalIncreasePerLevel", 0.0, Double.class);
    }
}
