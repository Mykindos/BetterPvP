package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
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
public class CleanseAbility extends ItemAbility {

    @EqualsAndHashCode.Include
    private double duration;
    @EqualsAndHashCode.Include
    private double cooldown;
    private final EffectManager effectManager;

    public CleanseAbility(EffectManager effectManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class), "cleanse"), "Cleanse",
                "Cleanses negative effects and grants immunity for a short duration.", TriggerType.RIGHT_CLICK);
        this.effectManager = effectManager;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        UtilMessage.message(player, "Item",
                Component.text("You used ", NamedTextColor.GRAY)
                        .append(Component.text(getName(), NamedTextColor.YELLOW))
                        .append(Component.text(".", NamedTextColor.GRAY)));
        UtilSound.playSound(player, Sound.ENTITY_GENERIC_DRINK, 1f, 1f, false);
        UtilSound.playSound(player.getWorld(), player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 1.2f);
        this.effectManager.addEffect(player, EffectTypes.IMMUNE, (long) (duration * 1000L));
        player.setFireTicks(0);
        UtilServer.callEvent(new EffectClearEvent(player));
        return true;
    }
} 