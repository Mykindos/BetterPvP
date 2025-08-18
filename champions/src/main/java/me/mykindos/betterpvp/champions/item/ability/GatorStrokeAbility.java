package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class GatorStrokeAbility extends ItemAbility {

    private double velocityStrength;
    private double energyPerTick;
    private double skimmingEnergyMultiplier;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final EnergyHandler energyHandler;

    @Inject
    private GatorStrokeAbility(Champions champions, EnergyHandler energyHandler) {
        super(new NamespacedKey(champions, "gator_stroke"),
                "Gator Stroke",
                "Propels the user at high speed. This ability only works in water.",
                TriggerTypes.HOLD_RIGHT_CLICK);
        this.champions = champions;
        this.energyHandler = energyHandler;
        
        // Default values, will be overridden by config
        this.velocityStrength = 0.7;
        this.energyPerTick = 1.0;
        this.skimmingEnergyMultiplier = 3.0;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        if (!UtilBlock.isInWater(player)) {
            return false;
        }
        
        double energyToUse = energyPerTick;
        if (!UtilBlock.isWater(player.getEyeLocation().getBlock().getRelative(BlockFace.DOWN))) {
            energyToUse *= skimmingEnergyMultiplier;
        }
        
        if (!energyHandler.use(player, getName(), energyToUse, true)) {
            return false;
        }
        
        VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), velocityStrength, false, 0, 0.11, 1.0, true);
        UtilVelocity.velocity(player, null, velocityData);
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.LAPIS_BLOCK);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, 0.8F, 1.5F);
        return true;
    }
} 