package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

@Singleton
@BPvPListener
public class ShieldSmash extends Skill implements InteractSkill, CooldownSkill, Listener, CrowdControlSkill {

    private double baseMultiplier;

    private double multiplierIncreasePerLevel;

    @Inject
    public ShieldSmash(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Shield Smash";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Smash your shield into an enemy,",
                "dealing " + getValueString(this::getKnockbackMultiplier, level, 100, "%", 0) + " knockback",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!hasSkill(player)) return;

        if (player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
            event.setKnockback(false);
        }
    }

    private double getKnockbackMultiplier(int level) {
        return baseMultiplier + ((level - 1) * multiplierIncreasePerLevel);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d) * cooldownDecreasePerLevel;
    }


    @Override
    public void activate(Player player, int level) {
        final Location bashLocation = player.getLocation().add(0, 0.8, 0);
        bashLocation.add(player.getLocation().getDirection().setY(0).normalize().multiply(1.5));

        // Visual Cues
        Collection<Player> receivers = player.getWorld().getNearbyPlayers(player.getLocation(), 60);
        Particle.CLOUD.builder().extra(0.05f).count(6).location(bashLocation).receivers(receivers).spawn();
        Particle.EXPLOSION.builder().extra(0).count(1).location(bashLocation).receivers(receivers).spawn();

        // Skill
        Vector direction = player.getLocation().getDirection();
        direction.setY(Math.max(0, direction.getY())); // Prevents downwards knockback

        final double strength = getKnockbackMultiplier(level);
        final List<KeyValue<LivingEntity, EntityProperty>> bashed = UtilEntity.getNearbyEntities(player, bashLocation, 2.5, EntityProperty.ALL);
        for (KeyValue<LivingEntity, EntityProperty> bashedEntry : bashed) {
            final LivingEntity ent = bashedEntry.getKey();

            // Add velocity and damage
            VelocityData velocityData = new VelocityData(direction, strength, false, 0, 0.3, 0.8 + 0.05 * level, true);
            UtilVelocity.velocity(ent, player, velocityData, VelocityType.KNOCKBACK_CUSTOM);
            UtilDamage.doCustomDamage(new CustomDamageEvent(ent, player, null, EntityDamageEvent.DamageCause.FALL, 0.0, false, getName()));

            // Cancel fall damage if they're friendly
            if (bashedEntry.getValue() == EntityProperty.FRIENDLY && ent instanceof Player friendly) {
                championsManager.getEffects().addEffect(friendly, EffectTypes.NO_FALL, 10_000);
            }

            // Inform them
            UtilMessage.simpleMessage(ent, "Skill", "<alt2>%s</alt2> hit you with <alt>%s %s</alt>.", player.getName(), getName(), level);
        }

        // Result indicator
        if (!bashed.isEmpty()) {
            player.getWorld().playSound(bashLocation, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.9f);
        } else {
            UtilMessage.simpleMessage(player, "Skill", "You missed <alt>%s %s</alt>.", getName(), level);
        }

    }

    @EventHandler
    public void onShieldCheck(RightClickEvent event) {
        Player player = event.getPlayer();
        if (hasSkill(player) && !this.championsManager.getCooldowns().hasCooldown(player, getName())) {
            if (isHolding(event.getPlayer())) {
                event.setUseShield(true);
                event.setShieldModelData(RightClickEvent.DEFAULT_SHIELD);
            }
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseMultiplier = getConfig("baseMultiplier", 1.6, Double.class);
        multiplierIncreasePerLevel = getConfig("multiplierIncreasePerLevel", 0.2, Double.class);
    }
}
