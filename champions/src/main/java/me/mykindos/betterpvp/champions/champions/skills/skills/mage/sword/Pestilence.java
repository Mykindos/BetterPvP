package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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
    @Getter
    private double poisonDuration;
    @Getter
    private double speed;
    @Getter
    private double radius;
    private double hitboxSize;
    private double expirySeconds;
    private int poisonLevel;

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
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to channel",
                "",
                "Release a <effect>Pestilence</effect> cloud that bounces,",
                "from target to target, giving them <effect>Poison " + poisonLevel + "</effect>",
                "for a maximum of <val>" + getPoisonDuration() + "</val> seconds.",
                "",
                "Cooldown: <val>" + getCooldown(),
                "Energy: <val>" + getEnergyPerSecond()
        };
    }

    private float getEnergyPerSecond() {
        return (float) energy;
    }

    public int getPoisonLevel() {
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
            if (!hasSkill(player)) {
                iterator.remove();
                continue;
            }

            // Check if they still are blocking and charge
            Gamer gamer = this.championsManager.getClientManager().search().online(player).getGamer();
            if (isHolding(player) && gamer.isHoldingRightClick() && championsManager.getEnergy().use(player, getName(), getEnergyPerSecond() / 20, true)) {
                data.tickSound(player);
                data.tick();
                continue;
            }

            UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>" + getName() + "</alt>.");
            shoot(player, data);
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

    private void shoot(Player player, ChargeData data) {
        new SoundEffect(Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.7F).play(player.getEyeLocation());

        final PestilenceProjectile projectile = new PestilenceProjectile(
                player,
                hitboxSize,
                hitboxSize * 2,
                player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.0)),
                (long) (expirySeconds * 1000),
                effectManager,
                getRadius(),
                getPoisonDuration(),
                getPoisonLevel()
        );
        projectile.redirect(player.getEyeLocation().getDirection());
        final double speed = getSpeed();
        projectile.setSpeed(Math.max(speed * 0.1, speed * data.getCharge()));
        projectiles.put(player, projectile);

        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(),
                showCooldownFinished(),
                true,
                isCancellable(),
                this::shouldDisplayActionBar);
    }

    @Override
    public void activate(Player player) {
        charging.put(player, new ChargeData(1.25f));
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
    public void loadSkillConfig() {
        poisonDuration = getConfig("poisonDuration", 3.0, Double.class);
        poisonLevel = getConfig("poisonLevel", 1, Integer.class);
        speed = getConfig("speed", 1.0, Double.class);
        radius = getConfig("radius", 8.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.7, Double.class);
        expirySeconds = getConfig("expirySeconds", 2.0, Double.class);
    }

    @Override
    public float getEnergy() {
        return energy;
    }
}

