package me.mykindos.betterpvp.core.settings.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class EnumSettingsButton extends AbstractItem implements CooldownButton {

    @Getter
    protected final String setting;
    @Getter
    private final Description description;
    private final PropertyContainer container;
    private final Enum<?> def;
    private final List<Enum<?>> values;

    public EnumSettingsButton(PropertyContainer container, Enum<?> setting, Enum<?> def, Description description) {
        this.container = container;
        this.description = description;
        this.setting = setting.name();
        this.def = def;
        this.values = (List<Enum<?>>) Arrays.stream(def.getDeclaringClass().getEnumConstants()).toList();
    }

    private Enum<?> getCurrentValue() {
        String valueName = (String) this.container.getProperty(setting).orElse(def.name());
        return Enum.valueOf(def.getDeclaringClass(), valueName);
    }

    private Enum<?> getNextValue(Enum<?> currentValue) {
        int index = values.indexOf(currentValue);
        if (index + 1 >= values.size()) {
            index = -1;
        }
        return values.get(index + 1);
    }

    @Override
    public ItemProvider getItemProvider() {
        final Enum<?> nextValue = getNextValue(getCurrentValue());
        final Component action = Component.text("Set: ", NamedTextColor.WHITE).append(Component.text(nextValue.name(), NamedTextColor.YELLOW));

        ItemProvider icon = description.getIcon();
        return ItemView.builder().with(icon.get())
                .action(ClickActions.ALL, action)
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final Enum<?> nextValue = getNextValue(getCurrentValue());
        this.container.saveProperty(setting, nextValue.name());

        notifyWindows();
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    @Override
    public double getCooldown() {
        return 0.2;
    }
}
