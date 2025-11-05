package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class EnergyBoost extends ItemAbility {

    @EqualsAndHashCode.Include
    private double energy;
    @EqualsAndHashCode.Include
    private double cooldown;
    private final EnergyService energyService;
    private final CooldownManager cooldownManager;
    private final SoundEffect soundEffect;

    public EnergyBoost(EnergyService energyService, CooldownManager cooldownManager, SoundEffect soundEffect) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class), "energy_boost"), "Energy Boost",
                "Instantly grants a flat energy boost when used.", TriggerTypes.RIGHT_CLICK);
        this.energyService = energyService;
        this.cooldownManager = cooldownManager;
        this.soundEffect = soundEffect;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        if (!cooldownManager.use(player, getName(), cooldown, true, true)) {
            return false;
        }

        energyService.regenerateEnergy(player, energy, EnergyEvent.Cause.USE);
        final TextComponent name = Component.text(getName()).color(NamedTextColor.YELLOW);
        UtilMessage.message(player, "Item", Component.text("You used ", NamedTextColor.GRAY)
                .append(name)
                .append(Component.text(".", NamedTextColor.GRAY)));
        soundEffect.play(player);
        return true;
    }
}
