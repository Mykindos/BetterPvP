package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HyperRushAbility extends ItemAbility {

    private double cooldown;
    private int speedAmplifier;
    private int durationTicks;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final CooldownManager cooldownManager;
    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;

    @Inject
    public HyperRushAbility(Champions champions, CooldownManager cooldownManager, EffectManager effectManager) {
        super(new NamespacedKey(champions, "hyper_rush"),
                "Hyper Rush",
                "Gain a burst of speed at a high level for a short duration.",
                TriggerTypes.RIGHT_CLICK);
        this.champions = champions;
        this.cooldownManager = cooldownManager;
        this.effectManager = effectManager;
        
        // Default values, will be overridden by config
        this.cooldown = 16.0;
        this.speedAmplifier = 3;
        this.durationTicks = 160; // 8 seconds (160 ticks)
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        // Check cooldown
        if (!cooldownManager.use(player, getName(), cooldown, true)) {
            return false;
        }
        
        // Apply speed effect
        effectManager.addEffect(player, EffectTypes.SPEED, speedAmplifier, (long) ((durationTicks / 20.0) * 1000));
        
        // Notify player and play sound
        UtilMessage.simpleMessage(player, "Hyper Axe", "You used <green>Hyper Rush<gray>.");
        UtilSound.playSound(player.getWorld(), player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
        return true;
    }
} 