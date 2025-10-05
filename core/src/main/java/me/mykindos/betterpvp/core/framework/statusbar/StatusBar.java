package me.mykindos.betterpvp.core.framework.statusbar;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.utilities.model.display.ActionBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class StatusBar extends ActionBar {

    private final EntityHealthService healthService;
    private final EnergyHandler energyHandler;

    public StatusBar(EntityHealthService healthService, EnergyHandler energyHandler) {
        this.healthService = healthService;
        this.energyHandler = energyHandler;
    }

    @Override
    public void show(Gamer gamer) {
        // Cleanup the action bar
        cleanUp();

        // The component to show
        Component component;
        synchronized (lock) {
            component = hasComponentsQueued() ?  nextComponent(gamer) : EMPTY;
        }
        if (component == null) {
            component = EMPTY;
        }

        // Append and prepend the health and energy components
        final Component health = getHealthComponent(gamer.getPlayer());
        final Component energy = getEnergyComponent(gamer.getPlayer());
        component = health.append(Component.text(" ".repeat(3)))
                .append(component)
                .append(Component.text(" ".repeat(3)))
                .append(energy);

        // Send the action bar to the player
        final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
        if (player != null) {
            player.sendActionBar(component);
        }
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
}
