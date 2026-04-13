package me.mykindos.betterpvp.core.menu.button.filter;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryBuilderFactory;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
@AllArgsConstructor
public class NameSearchButton extends AbstractItem {

    private final Supplier<String> getter;
    private final Consumer<String> setter;

    @Override
    public ItemProvider getItemProvider() {
        final String name = getter.get();
        if (name == null) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/icon/regular/magnifying_glass_icon"))
                    .displayName(Component.text("Search", NamedTextColor.GRAY))
                    .action(ClickActions.LEFT, Component.text("Search"))
                    .build();
        }

        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/magnifying_glass_icon"))
                .displayName(Component.text("Search: ", NamedTextColor.GRAY).
                        append(Component.text(name, NamedTextColor.GOLD)))
                .action(ClickActions.LEFT, Component.text("Change Search"))
                .action(ClickActions.RIGHT, Component.text("Clear Search"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isLeftClick()) {
            final Dialog dialog = Dialog.create(this::createDialog);
            player.showDialog(dialog);
        } else {
            setter.accept(null);
            notifyWindows();
        }
    }

    private void createDialog(RegistryBuilderFactory<@NotNull Dialog, ? extends DialogRegistryEntry.Builder> factory) {
        final DialogRegistryEntry.Builder builder = factory.empty();
        final TextDialogInput input = DialogInput.text("search", Component.text("Search an item by name")).maxLength(20).build();
        builder.base(DialogBase.builder(Component.text("Search"))
                .inputs(List.of(input))
                .build());

        builder.type(DialogType.confirmation(
                ActionButton.builder(Component.text("Search")).action(DialogAction.customClick((response, audience) -> {
                    final String text = response.getText("search");
                    String value = (text == null || text.isBlank()) ? null : text.toLowerCase().replace(" ", "_");
                    setter.accept(value);
                    notifyWindows();
                }, ClickCallback.Options.builder().build())).build(),
                ActionButton.builder(Component.text("Cancel")).build()
        ));
    }
}