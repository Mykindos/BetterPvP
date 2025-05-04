package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.PestilenceProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.util.Iterator;
import java.util.WeakHashMap;

@Slf4j
@Singleton
@BPvPListener
public class Pestilence extends ChannelSkill implements InteractSkill, CooldownSkill, EnergyChannelSkill, CrowdControlSkill, OffensiveSkill, DebuffSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final WeakHashMap<Player, PestilenceProjectile> projectiles = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this, charging);

    private final EffectManager effectManager;
    private double poisonDuration;
    private int poisonLevel;
    private double poisonDurationIncreasePerLevel;
    private double speed;
    private double radius;
    private double radiusIncreasePerLevel;
    private double hitboxSize;
    private double expirySeconds;

    @Inject
    public Pestilence(Champions champions, ChampionsManager championsManager, EffectManager effectManager) {
        super(champions, championsManager);
        this.effectManager = effectManager;
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public String getName() {
        return "Pestilence";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to channel",
                "",
                "Release a <effect>Pestilence</effect> cloud that bounces,",
                "from target to target, giving them <effect>Poison " + poisonLevel + "</effect>",
                "for a maximum of " + getValueString(this::getPoisonDuration, level) + " seconds.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "Energy: " + getValueString(this::getEnergyPerSecond, level)
        };
    }

    public double getPoisonDuration(int level) {
        return poisonDuration + ((level - 1) * poisonDurationIncreasePerLevel);
    }

    private float getEnergyPerSecond(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    public double getRadius(int level) {
        return radius + ((level - 1) * radiusIncreasePerLevel);
    }

    public double getSpeed(int level) {
        return speed;
    }

    public int getPoisonLevel(int level) {
        return poisonLevel;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @UpdateEvent
    public void updatePestilence() {
        final Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final ChargeData data = charging.get(player);
            if (player == null || !player.isValid()) {
                iterator.remove();
                continue;
            }

            // Remove if they no longer have the skill
            final int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            // Check if they still are blocking and charge
            Gamer gamer = this.championsManager.getClientManager().search().online(player).getGamer();
            if (isHolding(player) && gamer.isHoldingRightClick() && championsManager.getEnergy().use(player, getName(), getEnergyPerSecond(level) / 20, true)) {
                data.tickSound(player);
                data.tick();
                continue;
            }

            UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>" + getName() + " " + level + "</alt>.");
            shoot(player, data, level);
            iterator.remove();
        }

        final Iterator<Player> projectilesIterator = projectiles.keySet().iterator();
        while (projectilesIterator.hasNext()) {
            final Player player = projectilesIterator.next();
            if (player == null || !player.isValid()) {
                projectilesIterator.remove();
                continue;
            }

            final PestilenceProjectile projectile = projectiles.get(player);
            if (projectile == null || projectile.isMarkForRemoval() || projectile.isExpired()) {
                projectilesIterator.remove();
                continue;
            }

            projectile.tick();
        }
    }

    private void shoot(Player player, ChargeData data, int level) {
        new SoundEffect(Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.7F).play(player.getEyeLocation());

        final PestilenceProjectile projectile = new PestilenceProjectile(
                player,
                hitboxSize,
                player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.0)),
                (long) (expirySeconds * 1000),
                effectManager,
                getRadius(level),
                getPoisonDuration(level),
                getPoisonLevel(level)
        );
        final double speed = Math.max(getSpeed(level) * 0.1, getSpeed(level) * data.getCharge());
        projectile.redirect(player.getEyeLocation().getDirection().multiply(speed));
        projectiles.put(player, projectile);

        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                showCooldownFinished(),
                true,
                isCancellable(),
                this::shouldDisplayActionBar);
    }

    @Override
    public void activate(Player player, int level) {
        charging.put(player, new ChargeData((float) (0.1 + (level - 1) * 0.05) * 5));
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
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        poisonDuration = getConfig("poisonDuration", 3.0, Double.class);
        poisonDurationIncreasePerLevel = getConfig("poisonDurationIncreasePerLevel", 0.5, Double.class);
        poisonLevel = getConfig("poisonLevel", 1, Integer.class);
        speed = getConfig("speed", 1.0, Double.class);
        radius = getConfig("radius", 8.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.7, Double.class);
        expirySeconds = getConfig("expirySeconds", 2.0, Double.class);
    }

    @Override
    public float getEnergy(int level) {
        return energy;
    }
}

