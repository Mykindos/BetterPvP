package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data;

import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.RayProjectile;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;

import java.util.Collection;

public class DaggerProjectile extends RayProjectile {

    private final ItemDisplay display;
    private final double damage;
    private final Skill skill;

    public DaggerProjectile(Player caster, double hitboxSize, Location location, long aliveTime, ItemStack sword, double damage, Skill skill) {
        super(caster, hitboxSize, location, aliveTime);
        this.damage = damage;
        this.skill = skill;
        this.display = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(sword);
            spawned.setGlowing(false);

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set(0.5, 0.5, 1.0);
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(90));
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(45));
            transformation.getLeftRotation().rotateLocalZ(0f);
            spawned.setTransformation(transformation);
            spawned.setPersistent(false);

            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
        });
    }

    @Override
    protected void onTick() {
        final Collection<Player> receivers = location.getNearbyPlayers(60);
        for (Location point : interpolateLine()) {
            Particle.SMALL_GUST.builder()
                    .count(1)
                    .extra(0)
                    .offset(0.1, 0.1, 0.1)
                    .location(point)
                    .receivers(receivers)
                    .spawn();
        }

        display.teleport(location.clone().setDirection(display.getLocation().getDirection()));
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        // hit entities can only be living entities (non-armorstands) by default
        final Entity hit = result.getHitEntity();
        setMarkForRemoval(true);
        caster.getWorld().playSound(display.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0F, 2.0F);
        if (hit == null) {
            return;
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 2.0F);
        UtilDamage.doCustomDamage(new CustomDamageEvent(((LivingEntity) hit),
                caster,
                null,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                damage,
                true,
                skill.getName()));

        UtilMessage.simpleMessage(hit, skill.getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), skill.getName());
        UtilMessage.simpleMessage(caster, skill.getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s</alt>.", hit.getName(), skill.getName());
    }

    public void remove() {
        display.remove();
        Particle.GUST.builder()
                .count(0)
                .extra(0)
                .offset(0.0, 0.0, 0.0)
                .location(location)
                .receivers(30)
                .spawn();
    }
}