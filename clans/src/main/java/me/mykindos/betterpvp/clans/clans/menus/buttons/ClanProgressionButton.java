package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.clans.menus.PerkMenu;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ClanProgressionButton extends Button {
    private final Clan clan;

    public ClanProgressionButton(int slot, Clan clan) {
        super(slot, new ItemStack(Material.CAMPFIRE));
        this.clan = clan;
        this.name = Component.text("Clan Level", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC,false);

        final long currentLevel = clan.getLevel();
        final long currentBaseExperience = Clan.getExperienceForLevel(currentLevel);
        final long experienceNeeded = Clan.getExperienceForLevel(currentLevel + 1) - currentBaseExperience;
        final long experienceHave = clan.getExperience() - currentBaseExperience;
        final float progress = experienceHave / (float) experienceNeeded;
        final TextComponent progressBar = ProgressBar.withLength(progress, 20)
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

        this.lore = List.of(
                UtilMessage.DIVIDER,
                Component.empty(),
                progressBarFinal,
                Component.empty(),
                UtilMessage.deserialize("<gray>Level: <white>%s", currentLevel).decoration(TextDecoration.ITALIC, false),
                UtilMessage.deserialize("<gray>Progress: <white>%,d/%,d XP", experienceHave, experienceNeeded).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                UtilMessage.DIVIDER,
                Component.empty(),
                UtilMessage.deserialize("<white><bold>Click to</bold> <yellow>View Perks").decoration(TextDecoration.ITALIC, false)
        );

        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(itemStack, name, lore)).clone();
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (clickType.isLeftClick()) {
            MenuManager.openMenu(player, new PerkMenu(player, clan, new ClanMenu(player, clan, clan)));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 2f);
        }
    }
}
