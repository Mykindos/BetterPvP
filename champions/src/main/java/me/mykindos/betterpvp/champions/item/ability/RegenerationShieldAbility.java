package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class RegenerationShieldAbility extends ItemAbility {

    private double energyPerTick;
    private int regenerationAmplifier;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final EnergyHandler energyHandler;
    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;

    @Inject
    public RegenerationShieldAbility(Champions champions, EnergyHandler energyHandler, EffectManager effectManager) {
        super(new NamespacedKey(champions, "regeneration_shield"),
                "Shield",
                "Gain an amplified regeneration effect while using this ability.",
                TriggerTypes.HOLD_RIGHT_CLICK);
        this.champions = champions;
        this.energyHandler = energyHandler;
        this.effectManager = effectManager;
        
        // Default values, will be overridden by config
        this.energyPerTick = 1.5;
        this.regenerationAmplifier = 5;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        // Apply regeneration effect with condition to remove when no longer holding item
        applyRegeneration(player, itemStack.getType());
        
        // Check energy
        if (!energyHandler.use(player, getName(), energyPerTick, true)) {
            return false;
        }
        
        // Play particles and sound
        new SoundEffect(Sound.BLOCK_LAVA_POP, 1f, 2f).play(player.getLocation());
        new ParticleBuilder(Particle.HEART)
                .location(player.getEyeLocation().add(0, 0.25, 0))
                .offset(0.5, 0.5, 0.5)
                .extra(0.2f)
                .receivers(60)
                .spawn();
        return true;
    }
    
    /**
     * Apply regeneration effect to player
     */
    private void applyRegeneration(Player player, Material itemMaterial) {
        effectManager.addEffect(player, player, EffectTypes.REGENERATION, getName(), regenerationAmplifier, 80L, true, false,
                (livingEntity) -> {
                    if (livingEntity instanceof Player p) {
                        return p.getInventory().getItemInMainHand().getType() != itemMaterial;
                    }
                    return false;
                });
    }
} 