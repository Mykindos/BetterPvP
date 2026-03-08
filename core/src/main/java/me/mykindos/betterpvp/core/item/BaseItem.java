package me.mykindos.betterpvp.core.item;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityRenderer;
import me.mykindos.betterpvp.core.item.renderer.ItemLoreRenderer;
import me.mykindos.betterpvp.core.item.renderer.ItemNameRenderer;
import me.mykindos.betterpvp.core.item.renderer.ItemStackRenderer;
import me.mykindos.betterpvp.core.item.renderer.LoreComponentRenderer;
import me.mykindos.betterpvp.core.item.renderer.NameRarityRenderer;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An implementation of {@link Item} that uses the composite pattern for extensibility.
 * Supports addition/removal of components (runes, abilities, stats, etc.)
 */
public class BaseItem implements Item {

    @Getter
    protected final @NotNull ItemStack model;
    @Getter
    private final @NotNull ItemGroup itemGroup;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private @NotNull Function<@NotNull ItemInstance, @NotNull ItemRarity> instanceRarityProvider;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private @NotNull ItemLoreRenderer loreRenderer;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private @NotNull ItemNameRenderer itemNameRenderer;
    private final List<ItemStackRenderer> itemStackRenderers = new ArrayList<>();
    private final Multimap<Class<?>, @NotNull ItemComponent> serializableComponents = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Multimap<Class<?>, @NotNull ItemComponent> components = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Collection<@NotNull ItemComponent> componentsView = Collections.unmodifiableCollection(components.values());

    public BaseItem(String name, ItemStack model, ItemGroup group, ItemRarity rarity) {
        this(model, group, itemInstance -> rarity, new LoreComponentRenderer(), new NameRarityRenderer(name));
    }

    public BaseItem(Component name, ItemStack model, ItemGroup group, ItemRarity rarity) {
        this(model, group, itemInstance -> rarity, new LoreComponentRenderer(), new NameRarityRenderer(name));
    }

    public BaseItem(@NotNull ItemStack model,
                    @NotNull ItemGroup group,
                    @NotNull Function<@NotNull ItemInstance, @NotNull ItemRarity> rarityProvider,
                    @NotNull ItemLoreRenderer loreRenderer,
                    @NotNull ItemNameRenderer itemNameRenderer) {
        this.loreRenderer = loreRenderer;
        this.itemNameRenderer = itemNameRenderer;
        Preconditions.checkNotNull(model, "model cannot be null");
        Preconditions.checkNotNull(group, "itemGroup cannot be null");
        Preconditions.checkNotNull(rarityProvider, "rarityProvider cannot be null");
        this.model = model;
        this.itemGroup = group;
        this.instanceRarityProvider = rarityProvider;
        addItemStackRenderer(new DurabilityRenderer());
    }

    protected void addItemStackRenderer(@NotNull ItemStackRenderer renderer) {
        Preconditions.checkNotNull(renderer, "renderer cannot be null");
        itemStackRenderers.add(renderer);
    }

    public List<ItemStackRenderer> getItemStackRenderers() {
        return Collections.unmodifiableList(itemStackRenderers);
    }

    public boolean addSerializableComponent(@NotNull ItemComponent component) {
        if (addBaseComponent(component)) {
            serializableComponents.put(component.getClass(), component);
            return true;
        }
        return false;
    }

    public boolean removeSerializableComponent(@NotNull ItemComponent component) {
        Preconditions.checkNotNull(component, "component cannot be null");
        if (removeBaseComponent(component)) {
            serializableComponents.remove(component.getClass(), component);
            return true;
        }
        return false;
    }

    public boolean addBaseComponent(@NotNull ItemComponent component) {
        Preconditions.checkNotNull(component, "component cannot be null");
        Preconditions.checkArgument(component.isCompatibleWith(this), "component is not compatible with this item");
        return components.put(component.getClass(), component);
    }
    
    public boolean removeBaseComponent(@NotNull ItemComponent component) {
        Preconditions.checkNotNull(component, "component cannot be null");
        return components.remove(component.getClass(), component);
    }

    @Override
    public <T extends ItemComponent> Optional<T> getComponent(@NotNull Class<T> componentClass) {
        return components.get(componentClass).stream()
                .findFirst()
                .map(componentClass::cast);
    }

    @Override
    public @NotNull Collection<@NotNull ItemComponent> getComponents() {
        return componentsView;
    }

    /**
     * Determines which components from this BaseItem should be serialized into ItemInstances.
     * 
     * <p>By default, all components are serialized. Subclasses can override this to control
     * which components are serialized. For example, ability components might not need to be
     * serialized if they are the same for all instances of an item.</p>
     * 
     * @param component The component to check
     * @return true if the component should be serialized, false otherwise
     */
    public boolean shouldSerializeComponent(@NotNull ItemComponent component) {
        return contains(serializableComponents, component) || !contains(components, component);
    }
    
    /**
     * Gets the components that should be serialized when creating an ItemInstance.
     * This is determined by the {@link #shouldSerializeComponent(ItemComponent)} method.
     * 
     * @return A set of components that should be serialized
     */
    public @NotNull Set<ItemComponent> getSerializableComponents() {
        return components.values().stream()
                .filter(this::shouldSerializeComponent)
                .collect(Collectors.toSet());
    }

    // We have to do this because Multimap implementation ignores equals() and hashCode() of the values for lookups.
    // It instead uses the identity of the objects, which means we can't rely on the equals() method of ItemComponent.
    // This is a workaround to check for the component presence the same way a regular Set would.
    private boolean contains(Multimap<Class<?>, ItemComponent> map, ItemComponent component) {
        return map.asMap().values().stream()
                .flatMap(Collection::stream)
                .anyMatch(c -> c.equals(component));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BaseItem baseItem = (BaseItem) o;
        return model.isSimilar(baseItem.model)
                && itemGroup == baseItem.itemGroup
                && serializableComponents.equals(baseItem.serializableComponents)
                && components.equals(baseItem.components);
    }

    @Override
    public int hashCode() {
        ItemStack model = this.model.clone();
        model.setAmount(1); // Ensure consistent hashCode regardless of item amount
        final byte[] bytes = model.serializeAsBytes();
        int result = Arrays.hashCode(bytes); // Serialize the item to bytes for consistency

        // Use enum ordinal for absolute consistency (though hashCode should work too)
        result = 31 * result + itemGroup.ordinal();

        result = 31 * result + serializableComponents.asMap().values().stream()
                .flatMap(Collection::stream)
                .mapToInt(ItemComponent::hashCode)
                .sum();
        result = 31 * result + components.asMap().values().stream()
                .flatMap(Collection::stream)
                .mapToInt(ItemComponent::hashCode)
                .sum();
        return result;
    }
}