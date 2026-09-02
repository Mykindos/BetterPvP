package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildIcon;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.window.AnvilWindow;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.button.FlashingButton;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * A single build slot in the {@link BuildMenu}. Left click selects the build, right click opens it in the skill
 * editor, and shift right click renames it.
 */
public class BuildButton extends FlashingButton<BuildMenu> {

    public static final int MIN_NAME_LENGTH = 3;
    public static final int MAX_NAME_LENGTH = 15;

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9 ]+");

    private final GamerBuilds builds;
    private final Role role;
    private final int build;
    private final BuildIcon icon;
    private final BuildManager buildManager;
    private final ChampionsSkillManager skillManager;
    private final @Nullable RoleBuild promptBuild;

    /**
     * @param builds       the builds of the player looking at the menu
     * @param role         the role the build belongs to
     * @param build        the one-indexed build id
     * @param promptBuild  the optional build the player is being prompted to create. Null if empty
     * @param buildManager the BuildManager, used to persist renames
     * @param skillManager the champions SkillManager
     */
    public BuildButton(GamerBuilds builds, Role role, int build, @Nullable RoleBuild promptBuild,
                       BuildManager buildManager, ChampionsSkillManager skillManager) {
        this.builds = builds;
        this.role = role;
        this.build = build;
        this.icon = BuildIcon.of(build);
        this.buildManager = buildManager;
        this.skillManager = skillManager;
        this.promptBuild = promptBuild;

        if (promptBuild != null && promptBuild.getRole() == role && promptBuild.getId() == build) {
            setFlashing(true);
        }
    }

    @Override
    public ItemProvider getItemProvider(BuildMenu gui) {
        final Optional<RoleBuild> roleBuildOptional = builds.getBuild(role, build);
        final boolean selected = builds.getActiveBuilds().get(role.getName()).getId() == build;

        final ItemView.ItemViewBuilder builder = icon.itemBuilder()
                .displayName(nameOf(roleBuildOptional.orElse(null)).color(icon.getColor()).decorate(TextDecoration.BOLD))
                .lore(loreOf(roleBuildOptional.orElse(null)))
                .action(ClickActions.LEFT, Translations.component("champions.menu.build.button.action.select"))
                .action(ClickActions.RIGHT, Translations.component("champions.menu.build.button.action.edit"))
                .action(ClickActions.RIGHT_SHIFT, Translations.component("champions.menu.build.button.action.rename"))
                .glow(selected || isFlash());

        if (selected) {
            builder.material(Material.EMERALD_BLOCK);
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (ClickActions.RIGHT_SHIFT.accepts(clickType)) {
            openRenameWindow(player);
            return;
        }

        if (ClickActions.RIGHT.accepts(clickType)) {
            new SkillMenu(player, builds, role, build, buildManager, skillManager, getGui(), promptBuild).show(player);
            SoundEffect.HIGH_PITCH_PLING.play(player);
            return;
        }

        if (!ClickActions.LEFT.accepts(clickType)) {
            return;
        }

        builds.getBuild(role, build).ifPresent(selected -> {
            RoleBuild activeBuild = builds.getActiveBuilds().get(role.getName());
            activeBuild.setActive(false);

            selected.setActive(true);
            builds.getActiveBuilds().put(role.getName(), selected);

            UtilServer.callEvent(new ApplyBuildEvent(player, builds, activeBuild, selected));
            getGui().updateControlItems();
            SoundEffect.HIGH_PITCH_PLING.play(player);
        });
    }

    private Component nameOf(@Nullable RoleBuild roleBuild) {
        if (roleBuild == null || roleBuild.getName() == null) {
            return Translations.component("champions.menu.build.button.name", Component.text(build));
        }
        return Component.text(roleBuild.getName());
    }

    private List<Component> loreOf(@Nullable RoleBuild roleBuild) {
        final List<Component> lore = new ArrayList<>();
        lore.add(slotLine("sword", roleBuild == null ? null : roleBuild.getBuildSkill(SkillType.SWORD)));
        lore.add(slotLine("axe", roleBuild == null ? null : roleBuild.getBuildSkill(SkillType.AXE)));
        lore.add(slotLine("bow", roleBuild == null ? null : roleBuild.getBuildSkill(SkillType.BOW)));
        lore.add(slotLine("passive-a", roleBuild == null ? null : roleBuild.getBuildSkill(SkillType.PASSIVE_A)));
        lore.add(slotLine("passive-b", roleBuild == null ? null : roleBuild.getBuildSkill(SkillType.PASSIVE_B)));
        lore.add(slotLine("global", roleBuild == null ? null : roleBuild.getBuildSkill(SkillType.GLOBAL)));
        return lore;
    }

    private Component slotLine(String slot, @Nullable BuildSkill skill) {
        return Component.text()
                .append(Translations.component("champions.menu.build.slot." + slot).color(NamedTextColor.YELLOW)).appendSpace()
                .append(Component.text(skill != null ? skill.getString() : "", NamedTextColor.WHITE))
                .build();
    }

    /**
     * Opens an anvil the player types the new name into. The name is only committed by the save button, so a
     * half-typed or rejected name never reaches the build.
     */
    private void openRenameWindow(Player player) {
        final AtomicReference<String> input = new AtomicReference<>();
        final SimpleItem save = new SimpleItem(ItemView.builder()
                .material(Material.GREEN_CONCRETE)
                .displayName(Translations.component("champions.menu.build.rename.save").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .build(), click -> {
            final String name = input.get() == null ? "" : input.get().trim();
            if (!isValidName(name)) {
                SoundEffect.WRONG_ACTION.play(player);
                UtilMessage.message(player, "core.prefix.champions", "champions.menu.build.rename.invalid",
                        Component.text(MIN_NAME_LENGTH, NamedTextColor.GREEN),
                        Component.text(MAX_NAME_LENGTH, NamedTextColor.GREEN));
                return;
            }

            builds.getBuild(role, build).ifPresent(roleBuild -> {
                roleBuild.setName(name);
                buildManager.getBuildRepository().update(roleBuild);
                UtilMessage.message(player, "core.prefix.champions", "champions.menu.build.rename.success",
                        Component.text(name, icon.getColor()));
            });

            SoundEffect.HIGH_PITCH_PLING.play(player);
            player.closeInventory();
        });

        final ItemView label = ItemView.builder()
                .material(Material.PAPER)
                .displayName(nameOf(builds.getBuild(role, build).orElse(null)).color(icon.getColor()))
                .lore(Translations.component("champions.menu.build.rename.hint", Component.text(MIN_NAME_LENGTH), Component.text(MAX_NAME_LENGTH))
                        .color(NamedTextColor.GRAY))
                .build();

        AnvilWindow.single()
                .setGui(Gui.of(new Structure("x#p")
                        .addIngredient('x', label)
                        .addIngredient('p', save)))
                .setTitle(Translations.component("champions.menu.build.rename.title"))
                .addRenameHandler(input::set)
                .addCloseHandler(() -> {
                    getGui().updateControlItems();
                    getGui().showAfterClose(player);
                })
                .setViewer(player)
                .open(player);
    }

    private boolean isValidName(String name) {
        return name.length() >= MIN_NAME_LENGTH
                && name.length() <= MAX_NAME_LENGTH
                && VALID_NAME.matcher(name).matches();
    }
}
