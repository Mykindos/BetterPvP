package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.ReturningLinkProjectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;

import java.util.Collection;

public class BattlebindProjectile extends ReturningLinkProjectile {

    private final double damage;
    private final Skill skill;
    private final ItemStack swordItem;

    public BattlebindProjectile(Player caster, double hitboxSize, Location location, long aliveTime, long pullTime, double pullSpeed, ItemStack item, double damage, Skill skill) {
        super(caster, hitboxSize, location, aliveTime, pullTime, pullSpeed);
        this.damage = damage;
        this.skill = skill;
        this.swordItem = item;
    }

    @Override
    protected void onTick() {
        ((ItemDisplay) lead).setItemStack(swordItem);

        final Collection<Player> receivers = location.getNearbyPlayers(60);
        final BlockData data = Material.CHAIN.createBlockData();
        for (Location point : interpolateLine()) {
            Particle.BLOCK.builder()
                    .count(3)
                    .extra(0)
                    .data(data)
                    .offset(0.5, 0.5, 0.5)
                    .location(point)
                    .receivers(receivers)
                    .spawn();
        }
        super.onTick();
    }

    @Override
    protected Display item() {
        return location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(swordItem);
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
    protected Display createLink(Location spawnLocation, double height) {
        return spawnLocation.getWorld().spawn(spawnLocation, BlockDisplay.class, spawned -> {
            spawned.setBlock(Material.CHAIN.createBlockData());
            spawned.setGlowing(false);

            Transformation transformation = spawned.getTransformation();
            transformation.getTranslation().set(-0.5, 0, -0.5);
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(90));
            transformation.getLeftRotation().rotateLocalZ(0f);
            transformation.getScale().set(1, height, 1);
            spawned.setTransformation(transformation);

            spawned.setPersistent(false);
            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
        });
    }

    @Override
    protected SoundEffect pullSound() {
        return new SoundEffect(Sound.BLOCK_CHAIN_BREAK, 0f, 1f);
    }

    @Override
    protected SoundEffect pushSound() {
        return new SoundEffect(Sound.BLOCK_CHAIN_PLACE, 1f, 1f);
    }

    @Override
    public SoundEffect impactSound() {
        return new SoundEffect(Sound.BLOCK_GLASS_BREAK, 0.5f, 1f);
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        super.onImpact(location, result);
        if (hit == null) {
            return;
        }

        final DamageEvent event = new DamageEvent(hit,
                caster,
                null,
                new SkillDamageCause(skill),
                damage,
                skill.getName());
        event.setForceDamageDelay(0);
        event.setDamageDelay(0);
        UtilDamage.doDamage(event);

        UtilMessage.simpleMessage(hit, skill.getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), skill.getName());
        UtilMessage.simpleMessage(caster, skill.getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s</alt>.", hit.getName(), skill.getName());
    }
}
