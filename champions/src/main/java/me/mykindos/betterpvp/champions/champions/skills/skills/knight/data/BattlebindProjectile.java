package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword.Battlebind;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.LinkProjectile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

public class BattlebindProjectile extends LinkProjectile {

    private final Battlebind skill;
    private final int level;
    private final ItemStack swordItem;

    public BattlebindProjectile(Player caster, double hitboxSize, Location location, long aliveTime, ItemStack item, Battlebind skill, int level) {
        super(caster, hitboxSize, location, aliveTime);
        this.skill = skill;
        this.level = level;
        this.swordItem = item;
    }

    @Override
    protected void onTick() {
        ((ItemDisplay) lead).setItemStack(swordItem);

        final Collection<Player> receivers = location.getNearbyPlayers(60);
        final BlockData data = Material.IRON_CHAIN.createBlockData();
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
            spawned.setBlock(Material.IRON_CHAIN.createBlockData());
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
    protected SoundEffect pushSound() {
        return new SoundEffect(Sound.BLOCK_CHAIN_PLACE, 1f, 1f);
    }

    @Override
    protected SoundEffect impactSound() {
        return new SoundEffect(Sound.BLOCK_CHAIN_BREAK, 0.8f, 1f);
    }

    /**
     * The chain snaps on contact: the whole link trail and the thrown sword vanish, and whoever was
     * struck wears what is left of it.
     */
    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        super.onImpact(location, result);
        remove();
        setMarkForRemoval(true);

        Particle.BLOCK.builder()
                .count(20)
                .extra(0)
                .data(Material.IRON_CHAIN.createBlockData())
                .offset(0.3, 0.3, 0.3)
                .location(location)
                .allPlayers()
                .spawn();

        if (hit == null) {
            return;
        }

        final DamageEvent event = new DamageEvent(hit,
                caster,
                null,
                new SkillDamageCause(skill),
                skill.getDamage(level),
                skill.getName());
        event.setDamageDelay(0);
        UtilDamage.doDamage(event);

        skill.bind(caster, hit, level);

        UtilMessage.message(hit, skill.getClassType().getDisplayName(), "champions.skill.hit-by", this.skill.championsManager.getDisplayNameService().getProvider().getDisplayNameAsComponent(caster, hit), skill.getDisplayName().color(NamedTextColor.GREEN));
        UtilMessage.message(caster, skill.getClassType().getDisplayName(), "champions.skill.hit-target", this.skill.championsManager.getDisplayNameService().getProvider().getDisplayNameAsComponent(hit, caster), skill.getDisplayName().color(NamedTextColor.GREEN));
    }
}
