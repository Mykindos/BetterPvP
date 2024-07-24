package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class WolfsPounce extends ChannelSkill implements InteractSkill, CooldownSkill, OffensiveSkill, MovementSkill, DamageSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final WeakHashMap<Player, Pounce> pounces = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this,
            charging,
            gamer -> true);

    private double baseCharge;
    private double chargeIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseSlowDuration;
    private double slowDurationIncreasePerLevel;
    private int slowStrength;

    @Inject
    public WolfsPounce(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wolfs Pounce";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Hold right click with a Sword to channel",
                "",
                "Charges <val>" + getValueString(this::getChargePerSecond, level, 1, "%", 0) + "</val> per second",
                "",
                "Release right click to pounce forward",
                "in the direction you are looking",
                "",
                "Colliding with another player mid-air",
                "will deal up to " + getValueString(this::getDamage, level) + " damage and apply",
                "<effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect> for " + getValueString(this::getSlowDuration, level) + " seconds",
                "",
                "Taking damage cancels charge",
                "",
                "Cooldown: <val>" + getValueString(this::getCooldown, level)
        };
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + ((level -1) * slowDurationIncreasePerLevel);
    }

    private double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    private double getChargePerSecond(int level) {
        return baseCharge + (chargeIncreasePerLevel * (level - 1)); // Increment of 10% per level
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d) * cooldownDecreasePerLevel;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !charging.containsKey(gamer.getPlayer()) && isHolding(gamer.getPlayer());
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
    public void activate(Player player, int level) {
        final ChargeData chargeData = new ChargeData((float) getChargePerSecond(level) / 100);
        charging.put(player, chargeData);
    }

    private void pounce(Player player, ChargeData chargeData, int level) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s %d<gray>.", getName(), level);

        // Velocity
        final double charge = chargeData.getCharge();
        final double strength = 0.4 + (1.4 * charge);
        VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), strength, false, 0.0, 0.2, 0.4 + (0.9 * charge), true);
        UtilVelocity.velocity(player, null, velocityData);

        // Pounce log
        pounces.put(player, new Pounce(chargeData, level, player.getLocation(), -1));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 1f, 0.8f + (1.2f * (float) charge));

        // Cooldown
        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                true,
                true,
                isCancellable(),
                this::shouldDisplayActionBar);
    }

    private void collide(Player damager, LivingEntity damagee, Pounce pounce) {
        final int level = pounce.getLevel();
        double damage = getDamage(level) * pounce.getData().getCharge();

        // Effects & Damage
        UtilDamage.doCustomDamage(new CustomDamageEvent(damagee, damager, null, EntityDamageEvent.DamageCause.CUSTOM, damage, true, getName()));
        championsManager.getEffects().addEffect(damagee, damager, EffectTypes.SLOWNESS, slowStrength, (long) (getSlowDuration(level) * 1000));

        // Cues
        UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s %s</alt>.", damagee.getName(), getName(), level);
        UtilMessage.simpleMessage(damagee, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s %s</alt>.", damager.getName(), getName(), level);
        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.5f, 0.5f);
    }

    @EventHandler
    public void onDamageReceived(CustomDamageEvent event) {
        if (event.isCancelled() || !(event.getDamagee() instanceof Player player)) {
            return;
        }

        if (hasSkill(player) && charging.containsKey(player)) {
            charging.get(player).setCharge(0);
            // Cues
            UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> was interrupted.", getName());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 0.6f, 1.2f);
        }
    }

    @UpdateEvent
    public void checkCollide() {
        // Collision check
        final Iterator<Player> iterator = pounces.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // Mark as grounded if they're on the ground
            // If they're grounded for more than 250ms, remove them
            final Pounce pounce = pounces.get(player);
            if (pounce.getGroundTime() == -1 && (UtilBlock.isGrounded(player) || UtilBlock.isInLiquid(player))) {
                pounce.setGroundTime(System.currentTimeMillis());
            } else if (pounce.getGroundTime() != -1 && UtilTime.elapsed(pounce.getGroundTime(), 250)) {
                iterator.remove();
                continue;
            }

            // Passive particles while flying
            final Location playerLoc = player.getLocation();
            Particle.CRIT.builder()
                    .count(3)
                    .extra(0)
                    .offset(0.6, 0.6, 0.6)
                    .location(playerLoc)
                    .receivers(60)
                    .spawn();

            // See if they collided with anyone
            final Location midBody = playerLoc.clone().add(0, player.getHeight() / 2, 0);
            final List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, midBody, 1.5);
            final Optional<LivingEntity> hit = enemies.stream().findFirst().or(() -> UtilEntity.interpolateCollision(
                    pounce.getLastLocation(),
                    playerLoc,
                    0.6f,
                    ent -> UtilEntity.IS_ENEMY.test(player, ent)
            ).map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast));

            // If they didn't collide with anyone, continue
            if (hit.isEmpty()) {
                pounce.setLastLocation(playerLoc);
                continue;
            }

            // Collide
            iterator.remove();
            collide(player, hit.get(), pounce);
        }
    }

    @UpdateEvent
    public void updateCharge() {
        // Charge check
        Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            ChargeData charge = charging.get(player);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // Remove if they no longer have the skill
            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            // Check if they still are blocking and charge
            if (isHolding(player) && gamer.isHoldingRightClick()) {
                // Check if the player is grounded or the block directly beneath them is solid
                if (!UtilBlock.isGrounded(player, 2)){
                    if (charge.canSendMessage()) {
                        UtilMessage.simpleMessage(player, getClassType().getName(), "You cannot use <alt>" + getName() + "</alt> in the air.");
                        charge.messageSent();
                    }
                    continue;
                }

                charge.tick();
                charge.tickSound(player);
                continue;
            }

            iterator.remove();
            pounce(player, charge, level);
        }
    }

    @Override
    public void loadSkillConfig() {
        baseCharge = getConfig("baseCharge", 40.0, Double.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 10.0, Double.class);
        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        baseSlowDuration = getConfig("baseSlowDuration", 3.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);

        slowStrength = getConfig("slowStrength", 2, Integer.class);
    }

    @Data
    @AllArgsConstructor
    private static class Pounce {
        private final ChargeData data;
        private final int level;
        private Location lastLocation;
        private long groundTime;
    }}
