package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class UnderwaterBreathingAbility extends ItemAbility {

    @Inject
    private UnderwaterBreathingAbility(Champions champions) {
        super(new NamespacedKey(champions, "underwater_breathing"),
                "Underwater Breathing",
                "Grants instant underwater breathing when holding this item in water.",
                TriggerTypes.HOLD);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        if (!UtilBlock.isInWater(player)) {
            return false;
        }

        player.setRemainingAir(player.getMaximumAir());
        return true;
    }
} 