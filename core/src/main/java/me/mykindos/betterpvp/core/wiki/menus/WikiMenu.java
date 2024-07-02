package me.mykindos.betterpvp.core.wiki.menus;

import lombok.NonNull;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.settings.menus.SettingCategory;
import me.mykindos.betterpvp.core.settings.menus.event.SettingsFetchEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.wiki.menus.event.WikiFetchEvent;
import me.mykindos.betterpvp.core.wiki.types.WikiCategory;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.AbstractGui;
import xyz.xenondevs.invui.item.Click;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.Iterator;
import java.util.function.Consumer;

public class WikiMenu extends ViewCollectionMenu implements Windowed {

    public WikiMenu(@NonNull Player player, @NonNull Client client) {
        for (WikiCategory wikiCategory : WikiCategory.values()) {
            if (wikiCategory.getParent() == null) {

            }
        }
        super();
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Settings");
    }
}
