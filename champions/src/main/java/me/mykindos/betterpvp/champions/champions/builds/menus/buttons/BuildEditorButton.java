package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens the build editor for whichever class the selector currently describes. It is invisible and repeated
 * across four slots so it reads as one large button rather than four items.
 */
public class BuildEditorButton extends ControlItem<ClassSelectionMenu> {

    private final BuildManager buildManager;
    private final ChampionsSkillManager skillManager;
    private final RoleBuild promptBuild;

    public BuildEditorButton(BuildManager buildManager, ChampionsSkillManager skillManager, @Nullable RoleBuild promptBuild) {
        this.buildManager = buildManager;
        this.skillManager = skillManager;
        this.promptBuild = promptBuild;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!ClickActions.LEFT.accepts(clickType)) {
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        final GamerBuilds builds = buildManager.getObject(player.getUniqueId()).orElseThrow();
        new BuildMenu(builds, getGui().getSelected(), buildManager, skillManager, promptBuild, getGui()).show(player);
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    @Override
    public ItemProvider getItemProvider(ClassSelectionMenu gui) {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .displayName(Translations.component("champions.menu.build-editor.name")
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .action(ClickActions.LEFT, Translations.component("champions.menu.build-editor.open"))
                .hideAdditionalTooltip(true)
                .build();
    }
}
