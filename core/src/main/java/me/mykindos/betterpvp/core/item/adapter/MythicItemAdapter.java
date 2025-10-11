package me.mykindos.betterpvp.core.item.adapter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.events.MythicMobItemGenerateEvent;
import io.lumine.mythic.core.utils.jnbt.Tag;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

@CustomLog
@PluginAdapter("MythicMobs")
@BPvPListener
@Singleton
public class MythicItemAdapter implements Listener {

    private final ItemFactory itemFactory;

    @Inject
    private MythicItemAdapter(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onItem(MythicMobItemGenerateEvent event) {
        final Map<String, Tag> itemNBT = event.getItem().getItemNBT();
        if (!itemNBT.containsKey("bpvpid")) {
            return;
        }

        final Tag itemTag = itemNBT.get("bpvpid");
        final String itemKey = ((PlaceholderString) itemTag.getValue()).get();
        final BaseItem item = itemFactory.getItemRegistry().getItem(itemKey);
        if (item == null) {
            log.error("Could not find item {} to convert through MythicItemAdapter", itemKey).submit();
            return;
        }

        final ItemInstance instance = itemFactory.create(item);
        event.setItemStack(instance.createItemStack());
    }

}
