package me.mykindos.betterpvp.core.settings.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class SettingsButton extends AbstractItem implements CooldownButton {

    @Getter
    protected final String setting;
    @Getter
    private final Description description;
    private final PropertyContainer container;

    public SettingsButton(Gamer gamer, Enum<?> setting, Description description) {
        this.container = gamer;
        this.description = description;
        this.setting = setting.name();
    }

    public SettingsButton(Client client, Enum<?> setting, Description description) {
        this.container = client;
        this.description = description;
        this.setting = setting.name();
    }

    @Override
    public ItemProvider getItemProvider() {
        final boolean current = (boolean) this.container.getProperty(setting).orElse(false);
        final String action = current ? "Disable" : "Enable";

        ItemProvider icon = description.getIcon();
        return ItemView.builder().with(icon.get())
                    .action(ClickActions.ALL, Component.text(action))
                    .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final boolean current = (boolean) this.container.getProperty(setting).orElse(false);
        this.container.saveProperty(setting, !current);

        notifyWindows();
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    @Override
    public double getCooldown() {
        return 0.2;
    }
}
