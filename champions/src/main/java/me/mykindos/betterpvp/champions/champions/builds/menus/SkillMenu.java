package me.mykindos.betterpvp.champions.champions.builds.menus;

import lombok.NonNull;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.SkillButton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class SkillMenu extends AbstractGui implements Windowed {

    /**
     * The tag resolver for skill descriptions.
     * It will parse all tags with the name 'val'
     */
    public static final TagResolver TAG_RESOLVER = TagResolver.resolver(
            TagResolver.resolver("val", Tag.styling(NamedTextColor.GREEN)),
            TagResolver.resolver("effect", Tag.styling(NamedTextColor.WHITE)),
            TagResolver.resolver("stat", Tag.styling(NamedTextColor.YELLOW))
    );

    private final RoleBuild roleBuild;
    private final BuildManager buildManager;

    public SkillMenu(GamerBuilds builds, Role role, int build, BuildManager buildManager, ChampionsSkillManager skillManager, Windowed previous) {
        super(9, 6);
        this.roleBuild = builds.getBuilds().stream().filter(b -> b.getRole() == role && b.getId() == build).findFirst().orElseThrow();
        this.buildManager = buildManager;

        // Indicator items
        setItem(53, new BackButton(previous));
        setItem(0, getSkillType(Material.IRON_SWORD, "Sword Skills"));
        setItem(9, getSkillType(Material.IRON_AXE, "Axe Skills"));
        setItem(18, getSkillType(Material.BOW, "Bow Skills"));
        setItem(27, getSkillType(Material.RED_DYE, "Class Passive A Skills"));
        setItem(36, getSkillType(Material.ORANGE_DYE, "Class Passive B Skills"));
        setItem(45, getSkillType(Material.YELLOW_DYE, "Global Passive Skills"));


            setItem(8, new ControlItem<SkillMenu>() {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                    // ignored
                }

                @Override
                public ItemProvider getItemProvider(SkillMenu gui) {
                    return ItemView.builder()
                            .material(Material.NETHER_STAR)
                            .amount(roleBuild.getPoints())
                            .displayName(Component.text(roleBuild.getPoints() + " Skill Points Remaining", NamedTextColor.GREEN, TextDecoration.BOLD))
                            .build();
                }
            });


        int slotNumber = 0;
        int swordSlotNumber = 1;
        int axeSlotNumber = 10;
        int bowSlotNumber = 19;
        int passiveASlotNumber = 28;
        int passiveBSlotNumber = 37;
        int globalSlotNumber = 46;

        for (Skill skill : skillManager.getSkillsForRole(role)) {
            if (skill == null) continue;
            if (skill.getType() == null) continue;
            if (!skill.isEnabled()) continue;
            if (skill.getType() == SkillType.SWORD) {
                slotNumber = swordSlotNumber;
                swordSlotNumber++;
            } else if (skill.getType() == SkillType.AXE) {
                slotNumber = axeSlotNumber;
                axeSlotNumber++;
            } else if (skill.getType() == SkillType.BOW) {
                slotNumber = bowSlotNumber;
                bowSlotNumber++;
            } else if (skill.getType() == SkillType.PASSIVE_A) {
                slotNumber = passiveASlotNumber;
                passiveASlotNumber++;
            } else if (skill.getType() == SkillType.PASSIVE_B) {
                slotNumber = passiveBSlotNumber;
                passiveBSlotNumber++;
            } else if (skill.getType() == SkillType.GLOBAL) {
                slotNumber = globalSlotNumber;
                globalSlotNumber++;
            }


            setItem(slotNumber, new SkillButton(skill, roleBuild));
        }

        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static SimpleItem getSkillType(Material material, String name) {
        return ItemView.builder()
                .material(material)
                .displayName(Component.text(name, NamedTextColor.GREEN, TextDecoration.BOLD))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build()
                .toSimpleItem();
    }

    @Override
    public Window show(@NonNull Player player) {
        final Window window = Windowed.super.show(player);
        window.addCloseHandler(() -> buildManager.getBuildRepository().update(roleBuild));
        return window;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Edit Build");
    }
}
