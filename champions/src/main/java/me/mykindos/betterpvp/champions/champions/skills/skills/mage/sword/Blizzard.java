package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
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

    @Getter
    private double slowDuration;
    private int slowStrength;
    @Getter
    private double pushForwardStrength;
    @Getter
    private double pushUpwardStrength;

    @Inject
    public Blizzard(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Blizzard";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Hold right click with a Sword to channel.",
                "",
                "Release a blizzard that freezes enemies, giving them <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect>",
                "for <val>" + getSlowDuration() + "</val> seconds and pushing them back",
                "",
                "Energy: <val>" + getEnergy()
        };
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
    public float getEnergy() {
        return (float) energy;
    }


    @EventHandler
    public void onHit(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getProjectile() instanceof Snowball snowball) {
            if (snowball.getShooter() instanceof Player damager) {
                if (snow.containsKey(snowball)) {
                    LivingEntity damagee = event.getDamagee();

                    Vector direction = snowball.getVelocity().normalize();

                    Vector pushVelocity = direction.multiply(getPushForwardStrength());
                    UtilVelocity.velocity(damagee, damager, new VelocityData(pushVelocity, 1, true, getPushUpwardStrength(), 0, 0, false));

                    championsManager.getEffects().addEffect(damagee, event.getDamager(), EffectTypes.SLOWNESS, slowStrength, (long) (getSlowDuration() * 1000));

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

            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()) {
                iterator.remove();
                continue;
            }

            if (!hasSkill(player)) {
                iterator.remove();
            } else if (!championsManager.getEnergy().use(player, getName(), getEnergy() / 20, true)) {
                iterator.remove();
            } else if (!isHolding(player)) {
                iterator.remove();
            } else {
                Snowball s = player.launchProjectile(Snowball.class);
                s.getLocation().add(0, 1, 0);
                s.setVelocity(player.getLocation().getDirection().add(new Vector(UtilMath.randDouble(-0.1, 0.1), UtilMath.randDouble(-0.1, 0.1), UtilMath.randDouble(-0.1, 0.1))));
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SNOW_STEP, 1f, 0.4f);
                snow.put(s, player);
            }
        }
    }

    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        slowDuration = getConfig("slowDuration", 2.0, Double.class);
        slowStrength = getConfig("slowStrength", 3, Integer.class);

        pushForwardStrength = getConfig("pushForwardStrength", 0.3, Double.class);
        pushUpwardStrength = getConfig("pushUpwardStrength", 0.15, Double.class);
    }
}
