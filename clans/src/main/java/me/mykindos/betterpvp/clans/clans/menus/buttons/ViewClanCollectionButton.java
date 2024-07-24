package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;

public abstract class ViewClanCollectionButton extends AbstractItem {

    protected final String collectionName;
    protected final Windowed parent;
    protected final ItemView base;

    protected ViewClanCollectionButton(ItemView base, String collectionName, Windowed parent) {
        this.base = base;
        this.collectionName = collectionName;
        this.parent = parent;
    }

    protected abstract Collection<Clan> getPool();

    @Override
    public ItemProvider getItemProvider() {
        Collection<Clan> pool = getPool();
        return base.toBuilder()
                .frameLore(true)
                .displayName(Component.text(collectionName, NamedTextColor.YELLOW))
                .lore(UtilMessage.deserialize("<gray><white>%,d</white>/%,d Online", pool.stream().filter(Clan::isOnline).count(), pool.size()))
                .action(ClickActions.ALL, Component.text("View " + collectionName))
                .build();
    }
}
