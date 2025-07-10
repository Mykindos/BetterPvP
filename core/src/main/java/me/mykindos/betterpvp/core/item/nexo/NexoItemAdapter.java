package me.mykindos.betterpvp.core.item.nexo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.NexoPlugin;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

@Singleton
@PluginAdapter("Nexo")
@BPvPListener
public class NexoItemAdapter implements Listener {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;

    @Inject
    private NexoItemAdapter(ItemFactory itemFactory, ItemRegistry itemRegistry) {
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        itemFactory.registerDefaultBuilder(this::populateNexoItem);
    }

    private BaseItem getBaseNexoItem(String id) {
        return itemRegistry.getItems().values().stream()
                .filter(item -> item instanceof NexoItem nexoItem && nexoItem.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent event) {
        final ItemStack itemStack = event.getItem().getItemStack();
        final String nexoItem = NexoItems.idFromItem(itemStack);

        if (nexoItem == null) {
            return;
        }

        if (itemFactory.isCustomItem(itemStack)) {
            return;
        }

        final BaseItem baseItem = getBaseNexoItem(nexoItem);
        if (baseItem == null) {
            return;
        }

        final ItemInstance instance = itemFactory.create(baseItem);
        event.getItem().setItemStack(instance.createItemStack());
    }

    private void populateNexoItem(ItemInstance instance) {
        if (!(instance.getBaseItem() instanceof NexoItem nexoItem)) {
            return;
        }

        final ItemStack model = instance.getModel();
        model.editMeta(meta -> {
            final String id = nexoItem.getId();
            final ItemBuilder builder = Objects.requireNonNull(NexoItems.itemFromId(id), "Nexo item not found for id: " + id);
            final NamespacedKey itemModel = builder.getItemModel() == null
                    ? builder.getType().getKey()
                    : builder.getItemModel();
            meta.setItemModel(itemModel);
            meta.setCustomModelData(Objects.requireNonNull(builder.getNexoMeta()).getCustomModelData());

            final PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(new NamespacedKey("nexo", "id"), PersistentDataType.STRING, id);

            if (nexoItem.isFurniture()) {
                pdc.set(new NamespacedKey("nexo", "furniture"), PersistentDataType.BYTE, (byte) 1);
            }
        });
    }

}
