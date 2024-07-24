package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class ProfessionMenu extends AbstractGui implements Windowed {

    protected final String profession;
    protected final ProfessionProfile professionProfile;
    protected final ProgressionSkillManager progressionSkillManager;

    protected final ProfessionData professionData;

    public ProfessionMenu(String profession, ProfessionProfile professionProfile, ProgressionSkillManager progressionSkillManager) {
        super(9, 6);
        this.profession = profession;
        this.professionProfile = professionProfile;
        this.progressionSkillManager = progressionSkillManager;

        professionData = professionProfile.getProfessionDataMap().computeIfAbsent(profession,
                key -> new ProfessionData(professionProfile.getGamerUUID(), profession));

        final int currentLevel = professionData.getLevelFromExperience(professionData.getExperience());
        final double currentBaseExperience = professionData.getExperienceForLevel(currentLevel);
        final double experienceNeeded = professionData.getExperienceForLevel(currentLevel + 1) - currentBaseExperience;

        final double experienceHave = professionData.getExperience() - currentBaseExperience;
        final double progress = experienceHave / experienceNeeded;
        final TextComponent progressBar = ProgressBar.withLength((float) progress, 20)
                .withCharacter(' ')
                .build()
                .decoration(TextDecoration.STRIKETHROUGH, true);

        final Component progressBarFinal = Component.text(currentLevel, NamedTextColor.YELLOW)
                .appendSpace()
                .append(progressBar)
                .appendSpace()
                .append(Component.text(currentLevel + 1, NamedTextColor.YELLOW))
                .appendSpace()
                .append(Component.text(String.format("(%,d%%)", (int) (progress * 100)), TextColor.color(222, 222, 222)));

        ItemView build = ItemView.builder().material(Material.BEACON)
                .displayName(Component.text(profession + " Level", NamedTextColor.BLUE))
                .lore(progressBarFinal)
                .lore(Component.empty())
                .lore(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(currentLevel, NamedTextColor.YELLOW)))
                .lore(Component.text("Progress: ", NamedTextColor.GRAY).append(Component.text(String.format("%,.1f / %,.1f XP", experienceHave, experienceNeeded), NamedTextColor.YELLOW)))
                .lore(Component.text("Total Experience: ", NamedTextColor.GRAY).append(Component.text(String.format("%,.1f XP", professionData.getExperience()), NamedTextColor.YELLOW)))
                .frameLore(true)
                .build();

        setItem(0, build.toSimpleItem());

        setItem(8, new ControlItem<ProfessionMenu>() {

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // ignored
            }

            @Override
            public ItemProvider getItemProvider(ProfessionMenu gui) {
                int totalSkillLevels = professionData.getBuild().getSkills().values().stream().mapToInt(Integer::intValue).sum();
                Component levels = UtilMessage.deserialize("<green>Points available: <yellow>%d", (currentLevel - totalSkillLevels));

                return ItemView.builder().material(Material.NETHER_STAR).displayName(levels).build();
            }
        });

    }


    @Override
    public @NotNull Component getTitle() {
        return Component.text(profession);
    }

}
