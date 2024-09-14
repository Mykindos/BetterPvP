package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.combat.armour.ArmourManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.FlashingButton;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class ClassSelectionButton extends FlashingButton<ClassSelectionMenu> {

    private final BuildManager buildManager;
    private final Role role;
    private final RoleBuild roleBuild;
    private final ChampionsSkillManager skillManager;
    private final ArmourManager armourManager;
    private final Windowed parent;

    /**
     *
     * @param buildManager
     * @param skillManager
     * @param role
     * @param armourManager
     * @param roleBuild The optional rolebuild to prompt the player to create. Null if empty
     * @param parent
     */
    public ClassSelectionButton(BuildManager buildManager, ChampionsSkillManager skillManager, Role role, ArmourManager armourManager, @Nullable RoleBuild roleBuild, Windowed parent) {
        super();
        this.buildManager = buildManager;
        this.role = role;
        this.skillManager = skillManager;
        this.armourManager = armourManager;
        this.roleBuild = roleBuild;
        this.parent = parent;
        if (roleBuild != null) {
            if (roleBuild.getRole() == role) {
                this.setFlashing(true);
                this.setFlashPeriod(1000L);
            }
        }
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
        return ItemView.builder().material(role.getChestplate())
                .displayName(this.isFlashing() ? flashComponent : standardComponent)
                .lore(List.of(UtilMessage.deserialize("Class Damage Reduction: <yellow>" + this.armourManager.getReductionForArmourSet(role.getChestplate().name().replace("_CHESTPLATE", "")) + "%"),
                        UtilMessage.deserialize("Effective Health: <red>" + (int) Math.floor(20 / (1 - this.armourManager.getReductionForArmourSet(role.getChestplate().name().replace("_CHESTPLATE", "")) / 100))),
                        Component.text(""),
                        UtilMessage.deserialize("Click to manage your builds.")))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .glow(this.isFlash())
                .build();
    }
}
