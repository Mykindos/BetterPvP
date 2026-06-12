package me.mykindos.betterpvp.clans.clans.core.mailbox;

import me.mykindos.betterpvp.clans.clans.core.menu.CoreMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.FlashingButton;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class ClanMailboxButton extends FlashingButton<CoreMenu> {

    private static final ItemView MAILBOX_ITEM = ItemView.builder()
            .material(Material.CHEST)
            .displayName(Translations.component("clans.menu.core.button.mailbox.name").color(TextColor.color(84, 115, 255)).decorate(TextDecoration.BOLD))
            .frameLore(true)
            .lore(Translations.component("clans.menu.core.button.mailbox.lore.description.1").color(NamedTextColor.GRAY))
            .lore(Translations.component("clans.menu.core.button.mailbox.lore.description.2").color(NamedTextColor.GRAY))
            .build();

    private final ClanMailbox mailbox;
    private final ItemFactory itemFactory;
    private final Windowed previous;

    public ClanMailboxButton(ClanMailbox mailbox, ItemFactory itemFactory, Windowed previous) {
        this.mailbox = mailbox;
        this.previous = previous;
        this.itemFactory = itemFactory;
        this.setFlashing(!mailbox.getContents().isEmpty());
    }

    @Override
    public ItemProvider getItemProvider(CoreMenu gui) {
        return ItemView.builder()
                .material(Material.SMOKER)
                .displayName(Translations.component("clans.menu.core.button.mailbox.name").color(TextColor.color(84, 115, 255)).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.menu.core.button.mailbox.lore.description.1").color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.menu.core.button.mailbox.lore.description.2").color(NamedTextColor.GRAY))
                .glow(this.isFlash())
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (mailbox.isLocked()) {
            UtilMessage.message(player, "clans.prefix", "clans.core.mailbox.locked", Component.text(mailbox.getLockedBy(), NamedTextColor.DARK_RED));
            return;
        }

        mailbox.show(player, itemFactory, previous);
        new SoundEffect(Sound.BLOCK_CHEST_OPEN, 0.8F, 0.7F).play(player.getLocation());
    }

    @Override
    public boolean isFlashing() {
        return !mailbox.getContents().isEmpty();
    }
}
