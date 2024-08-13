package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.util.Vector;

import java.util.*;

@Singleton
@BPvPListener
public class Overcharge extends Skill implements InteractSkill, Listener, OffensiveSkill {

    private final WeakHashMap<Player, OverchargeData> data = new WeakHashMap<>();
    private final WeakHashMap<Arrow, OverchargeArrowData> bonus = new WeakHashMap<>();
    private final List<Arrow> arrows = new ArrayList<>();
    private final Set<UUID> charging = new HashSet<>();
    private double baseExtraVelocity;
    private double extraVelocityIncreasePerLevel;
    private double baseDuration;
    private double durationDecreasePerLevel;
    private double baseMaxExtraVelocity;
    private double maxExtraVelocityIncreasePerLevel;
    private double baseKnockbackStrength;
    private double knockbackStrengthIncreasePerLevel;

    @Inject
    public Overcharge(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Overcharge";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hold right click with a Bow to use",
                "",
                "Draw back harder on your bow, giving",
                getValueString(this::getExtraVelocity, level) + "% extra velocity per " + getValueString(this::getDuration, level) + " seconds,",
                "increasing knockback strength by " + getValueString(this::getKnockbackStrength, level) + "%.",
                "",
                "Maximum Velocity: " + getValueString(this::getMaxExtraVelocity, level) + "%"
        };
    }

    public double getExtraVelocity(int level) {
        return baseExtraVelocity + ((level - 1) * extraVelocityIncreasePerLevel);
    }

    public double getMaxExtraVelocity(int level) {
        return baseMaxExtraVelocity + ((level - 1) * maxExtraVelocityIncreasePerLevel);
    }

    public double getKnockbackStrength(int level) {
        return baseKnockbackStrength + ((level - 1) * knockbackStrengthIncreasePerLevel);
    }

    private double getDuration(int level) {
        return baseDuration - ((level - 1) * durationDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        charging.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        charging.remove(player.getUniqueId());
        if (hasSkill(player)) {
            OverchargeData overchargeData = data.get(player);
            if (overchargeData != null) {
                double charge = overchargeData.getCharge();
                bonus.put(arrow, new OverchargeArrowData(charge, player.getLocation().getDirection()));
                data.remove(player);

                // Apply the velocity increase
                Vector velocity = arrow.getVelocity();
                Vector originalVelocity = velocity.clone();
                velocity = velocity.multiply(1 + charge / 100.0);
                arrow.setVelocity(velocity);

                // Debug message for arrow velocity application
                Bukkit.getLogger().info(String.format(
                        "Arrow shot by player %s with original velocity %s. Applied velocity: %s. Charge: %.2f%%. Location: %s",
                        player.getName(), originalVelocity, velocity, charge, arrow.getLocation()
                ));
            }
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!(event.getEntity() instanceof Player hitPlayer)) return;
        OverchargeArrowData overchargeArrowData = bonus.get(arrow);
        if (overchargeArrowData != null) {
            Vector originalDirection = overchargeArrowData.getDirection();
            double chargeMultiplier = 1 + overchargeArrowData.getCharge() / 100.0;
            Vector knockback = originalDirection.multiply(chargeMultiplier);

            // Log charge multiplier and velocities
            Bukkit.getLogger().info(String.format(
                    "Calculating knockback: Player %s hit by arrow shot by %s. Original direction: %s. Charge multiplier: %.2f. Calculated knockback velocity: %s.",
                    hitPlayer.getName(), shooter.getName(), originalDirection, chargeMultiplier, knockback
            ));

            hitPlayer.setVelocity(knockback);

            // Log after setting velocity
            Bukkit.getLogger().info(String.format(
                    "Applied knockback: Player %s hit by arrow shot by %s. Knockback velocity set to: %s. Charge: %.2f%%. Hit location: %s",
                    hitPlayer.getName(), shooter.getName(), knockback, overchargeArrowData.getCharge(), hitPlayer.getLocation()
            ));
        }
    }


        @UpdateEvent
    public void createRedDustParticles() {
        bonus.forEach((arrow, arrowData) -> {
            if (arrow.isValid() && !arrow.isDead() && !arrow.isOnGround() && bonus.get(arrow) != null) {

                double baseSize = 0.25;
                double count = arrowData.getCharge() / 10;

                double finalSize = baseSize * count;

                Particle.DustOptions redDust = new Particle.DustOptions(Color.fromRGB(255, 0, 0), (float) finalSize);
                new ParticleBuilder(Particle.REDSTONE)
                        .location(arrow.getLocation())
                        .count(1)
                        .offset(0.1, 0.1, 0.1)
                        .extra(0)
                        .data(redDust)
                        .receivers(60)
                        .spawn();
            }
        });

        bonus.keySet().removeIf(arrow -> !arrow.isValid() || arrow.isDead() || arrow.isOnGround());
    }

    @UpdateEvent
    public void updateOvercharge() {
        Iterator<Map.Entry<Player, OverchargeData>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {
            OverchargeData data = iterator.next().getValue();
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player != null) {
                int level = getLevel(player);
                if (!charging.contains(player.getUniqueId())) {
                    iterator.remove();
                    continue;
                }

                if (!isHolding(player)) {
                    iterator.remove();
                    continue;
                }

                Material mainhand = player.getInventory().getItemInMainHand().getType();
                if (mainhand == Material.BOW && player.getActiveItem().getType() == Material.AIR) {
                    iterator.remove();
                    continue;
                }

                if (mainhand == Material.CROSSBOW && player.getActiveItem().getType() == Material.AIR) {
                    CrossbowMeta meta = (CrossbowMeta) player.getInventory().getItemInMainHand().getItemMeta();
                    if (!meta.hasChargedProjectiles()) {
                        iterator.remove();
                    }
                    continue;
                }

                if (UtilBlock.isInLiquid(player)) {
                    iterator.remove();
                    continue;
                }

                if (UtilTime.elapsed(data.getLastCharge(), (long) (getDuration(level) * 1000))) {
                    if (data.getCharge() < data.getMaxCharge()) {
                        data.addCharge();
                        UtilMessage.simpleMessage(player, getClassType().getName(), "%s: <yellow>+%d%%<gray> Bonus Velocity", getName(), (int) data.getCharge());
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4F, 1.0F + 0.05F * (float) data.getCharge());
                    }
                }
            }
        }

        arrows.removeIf(arrow -> arrow.isOnGround() || !arrow.isValid() || arrow.isInsideVehicle());
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    @Override
    public void activate(Player player, int level) {
        if (!data.containsKey(player)) {
            data.put(player, new OverchargeData(player.getUniqueId(), getExtraVelocity(level), getMaxExtraVelocity(level)));
            charging.add(player.getUniqueId());
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean displayWhenUsed() {
        return false;
    }

    @Data
    private static class OverchargeData {
        private final UUID uuid;
        private final double increment;
        private final double maxCharge;

        private double charge;
        private long lastCharge;

        public OverchargeData(UUID uuid, double increment, double maxCharge) {
            this.uuid = uuid;
            this.charge = 0;
            this.lastCharge = System.currentTimeMillis();
            this.increment = increment;
            this.maxCharge = maxCharge;
        }

        public void addCharge() {
            if (getCharge() <= getMaxCharge()) {
                setCharge(getCharge() + getIncrement());
                lastCharge = System.currentTimeMillis();
            }
        }
    }

    @Data
    private static class OverchargeArrowData {
        private final double charge;
        private final Vector direction;
    }

    public void loadSkillConfig() {
        baseExtraVelocity = getConfig("baseExtraVelocity", 10.0, Double.class);
        extraVelocityIncreasePerLevel = getConfig("extraVelocityIncreasePerLevel", 0.0, Double.class);
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationDecreasePerLevel = getConfig("durationDecreasePerLevel", 0.5, Double.class);
        baseMaxExtraVelocity = getConfig("baseMaxExtraVelocity", 100.0, Double.class);
        maxExtraVelocityIncreasePerLevel = getConfig("maxExtraVelocityIncreasePerLevel", 0.0, Double.class);
        baseKnockbackStrength = getConfig("baseKnockbackStrength", 10.0, Double.class);
        knockbackStrengthIncreasePerLevel = getConfig("knockbackStrengthIncreasePerLevel", 0.0, Double.class);
    }
}
