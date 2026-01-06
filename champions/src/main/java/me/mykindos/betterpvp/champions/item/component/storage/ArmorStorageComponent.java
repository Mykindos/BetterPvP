package me.mykindos.betterpvp.champions.item.component.storage;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.component.armor.RoleArmorComponent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ArmorStorageComponent extends AbstractItemComponent implements LoreComponent {

    // Helmet, Chestplate, Leggings, Boots
    private final ItemStack[] items;
    private final Role role;
    private final boolean exclusive;

    public ArmorStorageComponent(ItemStack[] items, Role role, boolean exclusive) {
        super("armor_storage");
        this.role = role;
        this.exclusive = exclusive;
        Preconditions.checkArgument(items.length == 4, "ArmorStorageComponent must have exactly 4 items");
        this.items = items;
    }

    public ArmorStorageComponent(Role role, boolean exclusive) {
        this(new ItemStack[4], role, exclusive);
    }

    public ItemStack[] getItems() {
        final ItemStack[] copy = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            copy[i] = items[i] == null || items[i].isEmpty() ? null : items[i].clone();
        }
        return copy;
    }

    public ItemStack getItem(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> getItems()[0];
            case CHEST -> getItems()[1];
            case LEGS -> getItems()[2];
            case FEET -> getItems()[3];
            default -> throw new IllegalArgumentException("Invalid slot: " + slot);
        };
    }

    public boolean canStore(ItemInstance itemInstance) {
        Optional<RoleArmorComponent> opt = itemInstance.getComponent(RoleArmorComponent.class);
        return opt.map(roleArmorComponent -> roleArmorComponent.getRoles().contains(role)).orElse(!exclusive);
    }

    public void setItem(EquipmentSlot slot, ItemStack item) {
        switch (slot) {
            case HEAD -> items[0] = item;
            case CHEST -> items[1] = item;
            case LEGS -> items[2] = item;
            case FEET -> items[3] = item;
            default -> throw new IllegalArgumentException("Invalid slot: " + slot);
        }
    }

    @Override
    public ArmorStorageComponent copy() {
        final ItemStack[] copy = getItems();
        return new ArmorStorageComponent(copy, role, exclusive);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        return List.of(
                Component.text("Contains:", NamedTextColor.GRAY, TextDecoration.UNDERLINED),
                    getArmorPiece(0, "helmet"),
                    getArmorPiece(1, "chestplate"),
                    getArmorPiece(2, "leggings"),
                    getArmorPiece(3, "boots")
        );
    }

    private Component getArmorPiece(int index, String placeholder) {
        final ItemStack stack = items[index];
        if (stack == null || stack.isEmpty()) {
            return Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text("✘", NamedTextColor.RED))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text("No " + placeholder, NamedTextColor.GRAY));
        }

        final ItemFactory itemFactory = JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(ItemFactory.class);
        return Component.text("[", NamedTextColor.GRAY)
                .append(Component.text("✔", NamedTextColor.GREEN))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(itemFactory.fromItemStack(stack).orElseThrow().getView().getName().applyFallbackStyle(NamedTextColor.WHITE));
    }

    @Override
    public int getRenderPriority() {
        return 100;
    }
}
