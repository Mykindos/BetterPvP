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

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SpeedBoostAbility extends ItemAbility {

    @EqualsAndHashCode.Include
    private double duration;
    @EqualsAndHashCode.Include
    private double cooldown;
    @EqualsAndHashCode.Include
    private int level;
    private final EffectManager effectManager;
    private final CooldownManager cooldownManager;

    @Inject
    private SpeedBoostAbility(EffectManager effectManager, CooldownManager cooldownManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class),
                        "speed_boost"),
                "Speed Boost",
                "Gain a small speed effect for a short duration.",
                TriggerType.RIGHT_CLICK);
        this.effectManager = effectManager;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());

        if (!cooldownManager.use(player, getName(), cooldown, true, true, false)) {
            return false;
        }

        effectManager.addEffect(player, EffectTypes.SPEED, level, (long) (duration * 1000));
        UtilMessage.message(player, "Item",
                Component.text("You used ", NamedTextColor.GRAY)
                        .append(Component.text(getName(), NamedTextColor.YELLOW))
                        .append(Component.text(".", NamedTextColor.GRAY)));
        UtilSound.playSound(player, Sound.ENTITY_PLAYER_BURP, 1f, 1f, false);
        return true;
    }
} 