package me.mykindos.betterpvp.core.framework.statusbar;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.function.Function;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@Singleton
public class StatusBar extends DisplayComponent {

    private final EntityHealthService healthService;
    private final EnergyHandler energyHandler;

    @Inject
    private StatusBar(EntityHealthService healthService, EnergyHandler energyHandler) {
        super(null);
        this.healthService = healthService;
        this.energyHandler = energyHandler;
    }

    @Override
    protected Function<Gamer, Component> getProvider() {
        return this::getComponent;
    }

    private Component getHealthComponent(Player player) {
        final int health = (int) Math.ceil(healthService.getHealth(player));
        final int maxHealth = (int) Math.ceil(healthService.getMaxHealth(player));
        return Component.empty()
                .append(Component.text(health + "/" + maxHealth, TextColor.color(255, 0, 0)))
                .appendSpace()
                .append(Component.text("<glyph:heart_icon>").font(NEXO));
    }

    private Component getEnergyComponent(Player player) {
        final int maxEnergy = (int) Math.ceil(energyHandler.getMax(player));
        final int energy = (int) Math.ceil(energyHandler.getEnergy(player) * maxEnergy);
        return Component.empty()
                .append(Component.text(energy + "/" + maxEnergy, TextColor.color(48, 114, 255)))
                .appendSpace()
                .append(Component.text("<glyph:mana_icon>").font(NEXO));
    }

    public Component getComponent(Gamer gamer) {
        final Player player = gamer.getPlayer();
        Preconditions.checkNotNull(player, "Gamer must be online");
        return Component.empty()
                .append(getHealthComponent(player))
                .append(Component.text(" ".repeat(3)))
                .append(getEnergyComponent(player))
                .append(Component.text(" ".repeat(25)))
                .append(Component.empty()); // For padding to work
    }
}
