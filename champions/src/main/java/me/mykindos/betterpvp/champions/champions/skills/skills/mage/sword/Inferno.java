package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.InfernoProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
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
public class Inferno extends ChannelSkill implements InteractSkill, EnergyChannelSkill, FireSkill, OffensiveSkill, DamageSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final WeakHashMap<Player, List<InfernoProjectile>> projectiles = new WeakHashMap<>();
    private final WeakHashMap<Player, InfernoProjectile> preparing = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this, charging);

    private double baseImpactDamage;
    private double impactDamageIncreasePerLevel;
    private double baseSpreadDamage;
    private double spreadDamageIncreasePerLevel;
    private double baseFireTicks;
    private double fireTicksIncreasePerLevel;
    private double baseMaxRadius;
    private double maxRadiusIncreasePerLevel;
    private double baseExpansionRate;
    private double expansionRateIncreasePerLevel;
    private double baseSpeed;
    private double speedIncreasePerLevel;
    private double projectileAliveTime;
    private double projectileHitboxSize;
    private double baseActivationEnergy;
    private double activationEnergyDecreasePerLevel;

    @Inject
    public Inferno(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Inferno";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "Charge and release a blazing inferno projectile",
                "that deals " + getValueString(this::getImpactDamage, level) + " impact damage at max charge.",
                "On impact, creates an expanding ring of fire",
                "that deals " + getValueString(this::getSpreadDamage, level) + " damage and ignites",
                "enemies for " + getValueString(l -> getFireTicks(l) / 20d, level) + " seconds.",
                "",
                "Activation Energy: " + getValueString(this::getActivationEnergy, level),
                "Channel Energy: " + getValueString(this::getEnergy, level) + " per second"
        };
    }

    public double getImpactDamage(int level) {
        return baseImpactDamage + ((level - 1) * impactDamageIncreasePerLevel);
    }

    public double getSpreadDamage(int level) {
        return baseSpreadDamage + ((level - 1) * spreadDamageIncreasePerLevel);
    }

    public int getFireTicks(int level) {
        return (int) (baseFireTicks + ((level - 1) * fireTicksIncreasePerLevel));
    }

    public double getMaxRadius(int level) {
        return baseMaxRadius + ((level - 1) * maxRadiusIncreasePerLevel);
    }

    public double getExpansionRate(int level) {
        return baseExpansionRate + ((level - 1) * expansionRateIncreasePerLevel);
    }

    public double getSpeed(int level) {
        return baseSpeed + ((level - 1) * speedIncreasePerLevel);
    }

    public float getActivationEnergy(int level) {
        return (float) (baseActivationEnergy - ((level - 1) * activationEnergyDecreasePerLevel));
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
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
    public void activate(Player player, int level) {
        // Consume flat activation energy
        if (!championsManager.getEnergy().use(player, getName(), getActivationEnergy(level), true)) {
            return; // Not enough energy, cancel activation
        }

        if (preparing.containsKey(player)) {
            preparing.remove(player).remove();
        }

        // Faster charge rate: 0.15 + (level - 1) * 0.05 per tick, multiplied by 20 to get per second
        final ChargeData data = new ChargeData((float) (0.15 + (level - 1) * 0.3)) {
            @Override
            public void playChargeSound(Player player, float charge) {
                player.playSound(player.getEyeLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1f + charge);
            }
        };

        // Start preparing the projectile
        final InfernoProjectile projectile = new InfernoProjectile(
                player,
                player.getEyeLocation(),
                () -> getImpactDamage(level) * Math.max(0.5f, data.getCharge()),
                () -> getSpreadDamage(level) * Math.max(0.5f, data.getCharge()),
                () -> getFireTicks(level),
                () -> getMaxRadius(level) * Math.max(0.5f, data.getCharge()),
                () -> getExpansionRate(level),
                projectileHitboxSize,
                0.1f,
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
            final InfernoProjectile projectile = preparing.get(player);
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

                // If they're still holding right-click, scale their projectile up
                // We can assume it's not null because they SHOULD have one

                final float increment = (float) (projectileHitboxSize * 2) * 0.9f;
                projectile.setDisplaySize(0.1f + (increment * data.getCharge()));
                final Location eyeLocation = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.1f));
                projectile.getLocation().set(eyeLocation.getX(), eyeLocation.getY(), eyeLocation.getZ());
                projectile.tick();
                continue;
            }

            // Released - fire the projectile
            shoot(player, projectile, data, level);
            preparing.remove(player);
            iterator.remove();
        }
    }

    private void shoot(Player player, InfernoProjectile projectile, ChargeData data, int level) {
        if (projectile == null) return;

        // Calculate scaled values based on charge
        final double speed = getSpeed(level);

        // Fire the projectile
        projectiles.computeIfAbsent(player, k -> new ArrayList<>()).add(projectile);
        projectile.redirect(player.getEyeLocation().getDirection().multiply(speed));
        projectile.markLaunched();

        // Feedback
        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>" + getName() + " " + level + "</alt>.");
        new SoundEffect(Sound.ENTITY_BLAZE_SHOOT, 2F, 0.8F).play(player.getLocation());
    }

    @UpdateEvent
    public void updateProjectiles() {
        final Iterator<Player> iterator = projectiles.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final List<InfernoProjectile> projectiles = this.projectiles.get(player);

            if (player == null || !player.isValid() || !player.isOnline() || projectiles == null || projectiles.isEmpty()) {
                iterator.remove();

                if (projectiles != null) {
                    projectiles.forEach(InfernoProjectile::remove);
                    projectiles.clear();
                }

                continue;
            }

            final Iterator<InfernoProjectile> projectileIterator = projectiles.iterator();
            while (projectileIterator.hasNext()) {
                final InfernoProjectile projectile = projectileIterator.next();
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
    public void loadSkillConfig() {
        baseImpactDamage = getConfig("baseImpactDamage", 6.0, Double.class);
        impactDamageIncreasePerLevel = getConfig("impactDamageIncreasePerLevel", 0.5, Double.class);
        baseSpreadDamage = getConfig("baseSpreadDamage", 3.0, Double.class);
        spreadDamageIncreasePerLevel = getConfig("spreadDamageIncreasePerLevel", 0.25, Double.class);
        baseFireTicks = getConfig("baseFireTicks", 50.0, Double.class);
        fireTicksIncreasePerLevel = getConfig("fireTicksIncreasePerLevel", 10.0, Double.class);
        baseMaxRadius = getConfig("baseMaxRadius", 6.0, Double.class);
        maxRadiusIncreasePerLevel = getConfig("maxRadiusIncreasePerLevel", 0.5, Double.class);
        baseExpansionRate = getConfig("baseExpansionRate", 0.35, Double.class);
        expansionRateIncreasePerLevel = getConfig("expansionRateIncreasePerLevel", 0.05, Double.class);
        baseSpeed = getConfig("baseSpeed", 15.0, Double.class);
        speedIncreasePerLevel = getConfig("speedIncreasePerLevel", 2.5, Double.class);
        projectileAliveTime = getConfig("projectileAliveTime", 5000.0, Double.class);
        projectileHitboxSize = getConfig("projectileHitboxSize", 0.5, Double.class);
        baseActivationEnergy = getConfig("baseActivationEnergy", 20.0, Double.class);
        activationEnergyDecreasePerLevel = getConfig("activationEnergyDecreasePerLevel", 2.0, Double.class);
    }

}

