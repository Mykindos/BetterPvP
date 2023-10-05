package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.combat.events.PlayerCheckShieldEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

@Singleton
@BPvPListener
public class ShieldSmash extends Skill implements InteractSkill, CooldownSkill, Listener {

    private double baseMultiplier;

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
                "dealing <val>" + (int) (getKnockbackMultiplier(level) * 100) + "%</val> knockback",
                "",
                "Cooldown: <val>" + getCooldown(level)
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

        if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
            event.setKnockback(false);
        }
    }

    private double getKnockbackMultiplier(int level) {
        return baseMultiplier + ((level - 1) * 0.2);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d);
    }


    @Override
    public void activate(Player player, int level) {
        final Location bashLocation = player.getLocation().add(0, 0.8, 0);
        bashLocation.add(player.getLocation().getDirection().setY(0).normalize().multiply(1.5));

        if (player.getInventory().getItemInOffHand().getType() != Material.SHIELD) {
            var shield = new ItemStack(Material.SHIELD);
            player.getInventory().setItemInOffHand(shield);
        }

        // Visual Cues
        Collection<Player> receivers = player.getWorld().getNearbyPlayers(player.getLocation(), 60);
        Particle.CLOUD.builder().extra(0.05f).count(6).location(bashLocation).receivers(receivers).spawn();
        Particle.EXPLOSION_LARGE.builder().extra(0).count(1).location(bashLocation).receivers(receivers).spawn();

        // Skill
        Vector direction = player.getLocation().getDirection();
        direction.setY(Math.max(0, direction.getY())); // Prevents downwards knockback

        final double strength = getKnockbackMultiplier(level);
        final List<KeyValue<LivingEntity, EntityProperty>> bashed = UtilEntity.getNearbyEntities(player, bashLocation, 2.5, EntityProperty.ALL);
        for (KeyValue<LivingEntity, EntityProperty> bashedEntry : bashed) {
            final LivingEntity ent = bashedEntry.getKey();

            // Add velocity and damage
            UtilVelocity.velocity(ent, direction, strength, false, 0, 0.3, 0.8 + 0.05 * level, true, true);
            UtilDamage.doCustomDamage(new CustomDamageEvent(ent, player, null, EntityDamageEvent.DamageCause.FALL, 0.0, false, getName()));

            // Cancel fall damage if they're friendly
            if (bashedEntry.getValue() == EntityProperty.FRIENDLY && ent instanceof Player friendly) {
                championsManager.getEffects().addEffect(friendly, EffectType.NOFALL, 10_000);
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
    public void onShieldCheck(PlayerCheckShieldEvent event) {
        Player player = event.getPlayer();
        if (hasSkill(player)) {
            if(UtilItem.isAxe(event.getPlayer().getInventory().getItemInMainHand().getType())) {
                event.setShouldHaveShield(true);
                event.setCustomModelData(0);
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
    }
}
