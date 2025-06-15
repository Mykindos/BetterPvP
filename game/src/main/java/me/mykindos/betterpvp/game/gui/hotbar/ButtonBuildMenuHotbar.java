package me.mykindos.betterpvp.game.gui.hotbar;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.game.framework.manager.InventoryProvider;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayout;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;

/**
 * Additional button for {@link BuildMenu} that allows the player to open the {@link GuiHotBarEditor}
 */
@RequiredArgsConstructor
public class ButtonBuildMenuHotbar extends ControlItem<BuildMenu> {

    private final InventoryProvider inventoryProvider;
    private final HotBarLayoutManager layoutManager;
    private final ItemFactory itemFactory;
    private final Role role;
    private final GamerBuilds builds;
    private final int buildId;

    @Override
    public ItemProvider getItemProvider(BuildMenu gui) {
        return ItemView.builder()
                .material(Material.CHEST)
                .displayName(Component.text("Edit Hotbar", NamedTextColor.GOLD))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final Optional<RoleBuild> buildOpt = builds.getBuild(role, buildId);
        if (buildOpt.isEmpty()) {
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        final HotBarLayout layout = Objects.requireNonNull(layoutManager.getLayout(player, buildOpt.get()));

        final WeakReference<Player> playerRef = new WeakReference<>(player);
        new HotBarEditor(role, layout, layoutManager, itemHandler, getGui(), p -> {
            Player cachedPlayer = playerRef.get();
            if (cachedPlayer != null) {
                inventoryProvider.refreshInventory(cachedPlayer);
            }
        }).show(player);

    }
}
