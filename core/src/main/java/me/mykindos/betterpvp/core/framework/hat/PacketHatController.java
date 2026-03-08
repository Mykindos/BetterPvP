package me.mykindos.betterpvp.core.framework.hat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Singleton
@PluginAdapter("PacketEvents")
public class PacketHatController {

    private final Multimap<Integer, HatProvider> providers = ArrayListMultimap.create();
    @Setter
    @Getter
    private Function<Player, ItemStack> providerFunction = player -> provideHighestPriority(player).orElse(null);
    private final HatProtocol protocol;
    private final ItemFactory itemFactory;

    @Inject
    public PacketHatController(HatProtocol protocol, ItemFactory itemFactory) {
        PacketEvents.getAPI().getEventManager().registerListener(new RemapperIn(this, protocol), PacketListenerPriority.HIGH);
        PacketEvents.getAPI().getEventManager().registerListener(new RemapperOut(this), PacketListenerPriority.HIGH);
        this.itemFactory = itemFactory;
        this.protocol = protocol;
    }

    public void addProvider(int index, HatProvider provider) {
        providers.put(index, provider);
    }

    public void removeProvider(HatProvider provider) {
        providers.values().remove(provider);
    }

    public Optional<ItemStack> provideHighestPriority(Player player) {
        final List<Integer> keys = providers.keySet().stream().sorted(Comparator.naturalOrder()).toList();
        for (int key : keys) {
            for (HatProvider provider : providers.get(key)) {
                final ItemStack itemStack = provider.apply(player);
                if (itemStack != null) {
                    return Optional.of(itemStack);
                }
            }
        }
        return Optional.empty();
    }

    public void broadcast(Player wearer, boolean others) {
        protocol.broadcast(wearer, others);
    }

    Optional<ItemStack> getHatItem(Player player) {
        ItemStack itemStack = providerFunction.apply(player);
        if (itemStack != null) {
            final ItemStack helmet = player.getInventory().getHelmet();
            final Component displayName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
                    ? itemStack.getItemMeta().displayName()
                    : itemStack.getData(DataComponentTypes.ITEM_NAME);

            if (helmet != null && !helmet.getType().isAir()) {
                final ItemStack view = itemFactory.fromItemStack(helmet).orElseThrow().getView().get();
                Integer model = itemStack.hasItemMeta() && itemStack.getItemMeta().hasCustomModelData()
                        ? itemStack.getItemMeta().getCustomModelData()
                        : null;
                itemStack = UtilItem.convertType(view, itemStack.getType(), model);
            } else {
                final ItemMeta meta = itemStack.getItemMeta();
                meta.displayName(Component.text("No helmet")
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false));
                itemStack.setItemMeta(meta);
            }

            // Add lore hat indicator
            final ItemMeta newMeta = itemStack.getItemMeta();
            final List<Component> lore = Objects.requireNonNullElse(newMeta.lore(), new ArrayList<>());
            lore.add(Component.empty());
            lore.add(Component.text("(", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Objects.requireNonNull(displayName).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(") ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            newMeta.lore(lore);
            itemStack.setItemMeta(newMeta);

            // Set hat metadata so we can identify it later
            final PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
            pdc.set(CoreNamespaceKeys.HATS_IS_HAT, PersistentDataType.BOOLEAN, true);
            pdc.set(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID, player.getUniqueId());
        }
        return Optional.ofNullable(itemStack);
    }

    Optional<ItemStack> fromHatItem(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return Optional.empty(); // No item
        }

        final ItemMeta meta = itemStack.getItemMeta();
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.getOrDefault(CoreNamespaceKeys.HATS_IS_HAT, PersistentDataType.BOOLEAN, false)) {
            return Optional.of(itemStack); // Not a hat
        }

        if (!pdc.has(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID)) {
            return Optional.of(itemStack); // No original owner
        }

        final UUID originalOwner = pdc.get(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID);
        final Player player = Bukkit.getPlayer(Objects.requireNonNull(originalOwner));
        if (player == null || !player.isOnline()) {
            return Optional.empty(); // Failsafe
        }

        return Optional.ofNullable(player.getInventory().getHelmet());
    }
}
