package me.mykindos.betterpvp.core.settings.menus;

import lombok.NonNull;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Click;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.settings.menus.event.SettingsFetchEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

public class SettingsMenu extends AbstractGui implements Windowed {

    public SettingsMenu(@NonNull Player player, @NonNull Client client) {
        super(9, 1);
        final SettingsFetchEvent event = new SettingsFetchEvent(player, client);
        UtilServer.callEvent(event);
        final Iterator<SettingCategory> categories = event.getCategories().iterator();
        int index = 0;
        while (categories.hasNext()) {
            final SettingCategory category = categories.next();
            final Description description = category.getDescription();
            setItem(index, new SimpleItem(description.getIcon(), click -> {
                final Consumer<Click> func = description.getClickFunction();
                if (func != null) {
                    func.accept(click);
                }
                category.show(click.getPlayer());
                SoundEffect.HIGH_PITCH_PLING.play(click.getPlayer());
            }));
            index++;
        }

        setBackground(Menu.BACKGROUND_ITEM);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Settings");
    }
}
