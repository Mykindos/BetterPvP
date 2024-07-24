package me.mykindos.betterpvp.core.framework.customtypes;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Used for Rupture / Grasp to ensure entity is invisible when spawned
 */
public class CustomArmourStand extends ArmorStand {
    public CustomArmourStand(Level world) {
        super(EntityType.ARMOR_STAND, world);
        setInvisible(true);
    }

    public CustomArmourStand(World world) {
        this(((CraftWorld) world).getHandle());
    }

    public CraftEntity spawn(Location loc) {
        this.absMoveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        this.level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return getBukkitEntity();
    }

    @Override
    public Fallsounds getFallSounds() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damagesource) {
        return null;
    }

    @Override
    public SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean canTakeItem(ItemStack itemStack){
        return false;
    }

    @Override
    protected SoundEvent getDrinkingSound(ItemStack itemstack) {
        return null;
    }
}
