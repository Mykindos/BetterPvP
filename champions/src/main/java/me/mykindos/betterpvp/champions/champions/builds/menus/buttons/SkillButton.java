package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.menu.button.FlashingButton;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class SkillButton extends FlashingButton<SkillMenu> {

    private final Skill skill;
    private final RoleBuild roleBuild;
    private final RoleBuild promptBuild;

    /**
     * @param skill
     * @param roleBuild
     * @param promptBuild The optional rolebuild to prompt the player to create. Null if empty
     */
    public SkillButton(Skill skill, RoleBuild roleBuild, @Nullable RoleBuild promptBuild) {
        this.skill = skill;
        this.roleBuild = roleBuild;
        this.promptBuild = promptBuild;
    }

    @Override
    public ItemProvider getItemProvider(SkillMenu gui) {
        final ItemView.ItemViewBuilder builder = ItemView.builder();
        if (this.skill.getTags() != null) {
            builder.prelore(this.skill.getTags());
        }

        builder.lore(Arrays.stream(this.skill.parseDescription()).toList());
        boolean active = roleBuild.getActiveSkills().stream().anyMatch(s -> s != null && s.equals(this.skill));
        if (active) {
            Material flashMaterial = this.isFlash() ? Material.WRITTEN_BOOK : Material.BOOK;
            builder.material(this.isFlashing() ? flashMaterial : Material.WRITTEN_BOOK);

            Component standardComponent = Component.text(this.skill.getName(), NamedTextColor.GREEN, TextDecoration.BOLD);
            Component flashingComponent = Component.text("Click Me!", NamedTextColor.RED)
                    .appendSpace()
                    .append(standardComponent);
            builder.displayName(isFlashing() ? flashingComponent : standardComponent);
        } else {
            builder.material(Material.BOOK);
            Component standardComponent = Component.text(this.skill.getName(), NamedTextColor.RED);
            Component flashingComponent = Component.text("Click Me!", NamedTextColor.GREEN)
                    .appendSpace()
                    .append(standardComponent).appendSpace();
            builder.displayName(isFlashing() ? flashingComponent : standardComponent);
        }

        return builder.flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP).frameLore(true).build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType == ClickType.DOUBLE_CLICK) return;

        Skill latest = roleBuild.getSkill(skill.getType());

        final boolean isDifferent = !skill.equals(latest);
        if (isDifferent && ClickActions.LEFT.accepts(clickType)) {
            // Return if skill isn't enabled
            if (!skill.isEnabled()) {
                UtilMessage.simpleMessage(player, "Skills", "This skill is not enabled.");
                UtilSound.playSound(player, Sound.ENTITY_ITEM_BREAK, 1f, 1f, false);
                return;
            }

            // Deselect the current skill
            if (latest != null) {
                roleBuild.setSkill(latest.getType(), null);
                UtilServer.callEvent(new SkillDequipEvent(player, latest, roleBuild));
            }

            // Set the new skill
            roleBuild.setSkill(skill.getType(), skill);
            UtilServer.callEvent(new SkillEquipEvent(player, skill, roleBuild));

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            getGui().updateControlItems();
        } else if (ClickActions.RIGHT.accepts(clickType)) {
            // Cancel if we're trying to remove a skill we don't have selected
            if (isDifferent) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
                return;
            }

            // Remove the skill
            roleBuild.setSkill(skill.getType(), null);
            UtilServer.callEvent(new SkillDequipEvent(player, latest, roleBuild));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            getGui().updateControlItems();
        } else {
            SoundEffect.WRONG_ACTION.play(player);
        }
    }

}