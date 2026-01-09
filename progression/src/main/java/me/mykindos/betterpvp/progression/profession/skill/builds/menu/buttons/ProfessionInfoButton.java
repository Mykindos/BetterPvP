package me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionAttributeNode;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.ProfessionMenu;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
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

import java.util.HashMap;
import java.util.Map;

public class ProfessionInfoButton extends ControlItem<ProfessionMenu> {

    private final String profession;
    private final ProfessionData professionData;

    public ProfessionInfoButton(String profession, ProfessionData professionData) {
        this.profession = profession;
        this.professionData = professionData;
    }

    @Override
    public ItemProvider getItemProvider(ProfessionMenu professionMenu) {
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

        int totalSkillLevels = professionData.getBuild().getSkills().values().stream().mapToInt(Integer::intValue).sum();
        // Calculate attribute totals
        Map<ProfessionAttribute, Double> attributeTotals = new HashMap<>();

        professionData.getBuild().getSkills().forEach((node, level) -> {
            if (node instanceof ProfessionAttributeNode attributeNode && level > 0) {
                attributeNode.getAttributes().forEach((attribute, config) -> {
                    double value = config.getBaseValue() + (Math.max(level - 1, 0) * config.getPerLevel());
                    attributeTotals.merge(attribute, value, Double::sum);
                });
            }
        });
        ItemView.ItemViewBuilder builder = ItemView.builder().material(Material.BARRIER)
                .customModelData(1)
                .displayName(Component.text(profession + " Level", NamedTextColor.BLUE))
                .itemModel(Resources.ItemModel.INVISIBLE)
                .lore(progressBarFinal)
                .lore(Component.empty())
                .lore(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(currentLevel, NamedTextColor.YELLOW)))
                .lore(Component.text("Progress: ", NamedTextColor.GRAY).append(Component.text(String.format("%,.1f / %,.1f XP", experienceHave, experienceNeeded), NamedTextColor.YELLOW)))
                .lore(Component.text("Total Experience: ", NamedTextColor.GRAY).append(Component.text(String.format("%,.1f XP", professionData.getExperience()), NamedTextColor.YELLOW)))
                .lore(Component.empty())
                .lore(UtilMessage.deserialize("<green>Points available: <yellow>%d", (currentLevel - totalSkillLevels)));

        // Add attribute totals to lore if any exist
        if (!attributeTotals.isEmpty()) {
            builder.lore(Component.empty())
                    .lore(Component.text("Attribute Totals:", NamedTextColor.GOLD));

            attributeTotals.forEach((attribute, total) -> {
                String formattedValue = UtilFormat.formatNumber(total, 1);
                builder.lore(Component.text(" +" + formattedValue + attribute.getOperation() + " " + attribute.getName(), NamedTextColor.GREEN));
            });
        }

        return builder.frameLore(true).build();

    }


    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

    }
}
