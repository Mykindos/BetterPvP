package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.Random;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class EffectRouletteAbility extends ItemAbility {

    @EqualsAndHashCode.Include
    private double duration;
    @EqualsAndHashCode.Include
    private double cooldown;
    private final EffectManager effectManager;
    private final CooldownManager cooldownManager;
    private static final Random random = new Random();

    @Inject
    private EffectRouletteAbility(EffectManager effectManager, CooldownManager cooldownManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class), "effect_roulette"), "Effect Roulette",
                "Grants a random effect for a short duration. The effect can be a positive or negative one.", TriggerType.RIGHT_CLICK);
        this.effectManager = effectManager;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());

        if (!cooldownManager.use(player, getName(), cooldown, true, true, false)) {
            return false;
        }

        // Cooldown is handled by the item system or externally if needed
        List<EffectType> effectTypesList = EffectTypes.getEffectTypes();
        List<EffectType> validEffectTypes = effectTypesList.stream()
                .filter(effect -> !effect.isSpecial())
                .toList();
        EffectType randomEffect = validEffectTypes.get(random.nextInt(validEffectTypes.size()));
        int randomLevel = random.nextInt(4) + 1;
        effectManager.addEffect(player, randomEffect, randomLevel, (long) (duration * 1000));
        UtilMessage.message(player, "Item",
                Component.text("You used ", NamedTextColor.GRAY)
                        .append(Component.text(getName(), NamedTextColor.YELLOW))
                        .append(Component.text(".", NamedTextColor.GRAY)));
        UtilMessage.message(player, "Effect",
                Component.text("You have been granted ", NamedTextColor.GREEN)
                        .append(Component.text(randomEffect.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" Level " + randomLevel, NamedTextColor.GREEN))
                        .append(Component.text(" for " + duration + " seconds.", NamedTextColor.GREEN)));
        UtilSound.playSound(player, Sound.ENTITY_PLAYER_BURP, 1f, 1f, false);
        return true;
    }
} 