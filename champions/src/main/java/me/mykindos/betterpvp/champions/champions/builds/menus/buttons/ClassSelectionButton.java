package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.roles.RoleEffect;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.FlashingButton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class ClassSelectionButton extends FlashingButton<ClassSelectionMenu> {

    private final BuildManager buildManager;
    private final Role role;
    private final double reduction;
    private final RoleBuild roleBuild;
    private final ChampionsSkillManager skillManager;
    private final Windowed parent;
    private final boolean shouldShowPassives;

    public ClassSelectionButton(BuildManager buildManager, ChampionsSkillManager skillManager, Role role,
                                @Nullable RoleBuild roleBuild, Windowed parent, boolean shouldShowPassives) {
        super();
        this.buildManager = buildManager;
        this.role = role;
        this.skillManager = skillManager;
        this.roleBuild = roleBuild;
        this.parent = parent;
        this.shouldShowPassives = shouldShowPassives;
        if (roleBuild != null) {
            if (roleBuild.getRole() == role) {
                this.setFlashing(true);
                this.setFlashPeriod(1000L);
            }
        }

        final EntityHealthService entityHealthService = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(EntityHealthService.class);
        this.reduction = entityHealthService.getHealth(new ItemStack[]{
                ItemStack.of(role.getHelmet()),
                ItemStack.of(role.getChestplate()),
                ItemStack.of(role.getLeggings()),
                ItemStack.of(role.getBoots())
        });
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final GamerBuilds builds = buildManager.getObject(player.getUniqueId()).orElseThrow();
        new BuildMenu(builds, role, buildManager, skillManager, roleBuild, parent).show(player);
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    @Override
    public ItemProvider getItemProvider(ClassSelectionMenu gui) {
        final Component standardComponent = Component.text(role.getName(), role.getColor(), TextDecoration.BOLD);
        final Component flashComponent = Component.empty().append(Component.text("Click Me!", NamedTextColor.GREEN)).appendSpace().append(standardComponent);

        List<Component> roleLore = new ArrayList<>(List.of(
                Component.text("While wearing base armor:", NamedTextColor.GRAY),
                Component.empty()
                        .append(Component.text("+", TextColor.color(255, 0, 0)))
                        .append(Component.text(UtilFormat.formatNumber(reduction), TextColor.color(255, 0, 0)))
                        .appendSpace()
                        .append(Component.text("❤", TextColor.color(255, 0, 0)))
                        .appendSpace()
                        .append(Component.text("Health", TextColor.color(255, 0, 0))),
                Component.empty()
        ));

        if (shouldShowPassives) {

            // Use a default because not every role has a passive
            ArrayList<RoleEffect> roleEffects = RoleManager.rolePassiveDescs.getOrDefault(role, null);
            if (roleEffects == null) {
                roleLore.add(Component.text("No Effects", NamedTextColor.WHITE, TextDecoration.BOLD));
                roleLore.add(Component.text(""));
            } else {
                roleLore.add(Component.text("Effects:", NamedTextColor.WHITE, TextDecoration.BOLD));
                for (RoleEffect roleEffect : roleEffects) {
                    roleLore.add(Component.text("- ").append(roleEffect.getDescription()));
                }
                roleLore.add(Component.text(""));
            }

        }

        roleLore.add(UtilMessage.deserialize("Click to manage your builds."));

        return ItemView.builder().material(role.getChestplate())
                .displayName(this.isFlashing() ? flashComponent : standardComponent)
                .lore(roleLore)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .glow(this.isFlash())
                .build();
    }
}
