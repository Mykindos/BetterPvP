package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.BlizzardProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Blizzard extends ChannelSkill implements InteractSkill, EnergyChannelSkill, CooldownSkill, CrowdControlSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final WeakHashMap<Player, List<BlizzardProjectile>> projectiles = new WeakHashMap<>();
    private final WeakHashMap<Player, BlizzardProjectile> preparing = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this, charging);

    private int slowStrength;
    private double baseSlowDuration;
    private double slowDurationIncreasePerLevel;
    private double pushForwardStrength;
    private double pushForwardIncreasePerLevel;
    private double pushUpwardStrength;
    private double pushUpwardIncreasePerLevel;
    private double baseSpeed;
    private double speedIncreasePerLevel;
    private double projectileAliveTime;
    private double projectileHitboxSize;
    private double baseActivationEnergy;
    private double activationEnergyDecreasePerLevel;

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
                "Hold right click with a Sword to channel",
                "",
                "Charge and release a rolling blizzard projectile",
                "On impact, freezes enemies with <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect>",
                "for " + getValueString(this::getSlowDuration, level) + " seconds and pushes them back.",
                "Reflects off walls and maintains speed.",
                "",
                "Activation Energy: " + getValueString(this::getActivationEnergy, level),
                "Channel Energy: " + getValueString(this::getEnergy, level) + " per second"
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

    public double getSpeed(int level) {
        return baseSpeed + ((level - 1) * speedIncreasePerLevel);
    }

    public float getActivationEnergy(int level) {
        return (float) (baseActivationEnergy - ((level - 1) * activationEnergyDecreasePerLevel));
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

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !charging.containsKey(gamer.getPlayer()) && isHolding(gamer.getPlayer());
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1) * cooldownDecreasePerLevel;
    }

    @Override
    public void activate(Player player, int level) {
        // Consume flat activation energy
        if (!championsManager.getEnergy().use(player, getName(), getActivationEnergy(level), true)) {
            return; // Not enough energy, cancel activation
        }

        if (preparing.containsKey(player)) {
            preparing.remove(player).remove();
        }

        // Charge rate: 0.15 + (level - 1) * 0.3 per tick
        final ChargeData data = new ChargeData((float) (0.15 + (level - 1) * 0.3)) {
            @Override
            public void playChargeSound(Player player, float charge) {
                player.playSound(player.getEyeLocation(), Sound.BLOCK_SNOW_STEP, 0.5f, 0.5f + charge);
            }
        };

        // Start preparing the projectile at player feet
        final BlizzardProjectile projectile = new BlizzardProjectile(
                player,
                player.getLocation(),
                projectileHitboxSize,
                projectileHitboxSize * 2,
                (long) projectileAliveTime,
                this,
                championsManager
        );
        preparing.put(player, projectile);
        charging.put(player, data);
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
    }

    @UpdateEvent
    public void updateCharging() {
        final Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final ChargeData data = charging.get(player);

            // Check if they are still holding and right-clicking to charge
            final BlizzardProjectile projectile = preparing.get(player);
            if (projectile == null) {
                continue;
            }

            if (player == null || !player.isValid() || !player.isOnline()) {
                iterator.remove();
                projectile.remove();
                preparing.remove(player);
                continue;
            }

            // Remove if they no longer have the skill
            final int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                projectile.remove();
                preparing.remove(player);
                continue;
            }

            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (isHolding(player) && gamer.isHoldingRightClick()
                    && (data.getCharge() >= 1.0 || championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true))) {
                championsManager.getEnergy().degenerateEnergy(player, 0);
                data.tick();
                data.tickSound(player);

                // Position at player feet while charging
                final Location feetLocation = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(0.5f));
                feetLocation.subtract(0, projectileHitboxSize / 2, 0);
                projectile.getLocation().set(feetLocation.getX(), feetLocation.getY(), feetLocation.getZ());
                projectile.tick();
                continue;
            }

            final Location feetLocation = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(0.5f));
            feetLocation.add(0, projectileHitboxSize, 0);
            projectile.getLocation().set(feetLocation.getX(), feetLocation.getY(), feetLocation.getZ());

            // Released - fire the projectile
            shoot(player, projectile, data, level);
            preparing.remove(player);
            iterator.remove();
        }
    }

    private void shoot(Player player, BlizzardProjectile projectile, ChargeData data, int level) {
        if (projectile == null) return;

        // Calculate speed based on charge
        final double speed = getSpeed(level) * Math.max(0.5f, data.getCharge());

        // Fire the projectile from feet
        projectiles.computeIfAbsent(player, k -> new ArrayList<>()).add(projectile);
        projectile.redirect(player.getLocation().getDirection().setY(0).normalize().multiply(speed));
        projectile.markLaunched();

        // Feedback
        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>" + getName() + " " + level + "</alt>.");
        new SoundEffect(Sound.ENTITY_SNOW_GOLEM_SHOOT, 2F, 0.8F).play(player.getLocation());
    }

    @UpdateEvent
    public void updateProjectiles() {
        final Iterator<Player> iterator = projectiles.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final List<BlizzardProjectile> projectiles = this.projectiles.get(player);

            if (player == null || !player.isValid() || !player.isOnline() || projectiles == null || projectiles.isEmpty()) {
                iterator.remove();

                if (projectiles != null) {
                    projectiles.forEach(BlizzardProjectile::remove);
                    projectiles.clear();
                }

                continue;
            }

            final Iterator<BlizzardProjectile> projectileIterator = projectiles.iterator();
            while (projectileIterator.hasNext()) {
                final BlizzardProjectile projectile = projectileIterator.next();
                if (projectile.isExpired() || projectile.isMarkForRemoval()) {
                    projectile.remove();
                    projectileIterator.remove();
                    continue;
                }

                // Tick the projectile to update position and visuals
                projectile.tick();
            }
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseSpeed = getConfig("baseSpeed", 30.0, Double.class);
        speedIncreasePerLevel = getConfig("speedIncreasePerLevel", 5.0, Double.class);
        projectileAliveTime = getConfig("projectileAliveTime", 5000.0, Double.class);
        projectileHitboxSize = getConfig("projectileHitboxSize", 0.5, Double.class);
    }
}
