package me.mykindos.betterpvp.clans.clans.core.mailbox;

import me.mykindos.betterpvp.clans.clans.core.menu.CoreMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
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
            .displayName(Component.text("Mailbox", TextColor.color(84, 115, 255), TextDecoration.BOLD))
            .frameLore(true)
            .lore(Component.text("The mailbox stores items that have", NamedTextColor.GRAY))
            .lore(Component.text("been delivered to your clan.", NamedTextColor.GRAY))
            .build();

    private final ClanMailbox mailbox;
    private final Windowed previous;

    public ClanMailboxButton(ClanMailbox mailbox, Windowed previous) {
        this.mailbox = mailbox;
        this.previous = previous;
        this.setFlashing(!mailbox.getContents().isEmpty());
    }

    @Override
    public ItemProvider getItemProvider(CoreMenu gui) {
        return ItemView.builder()
                .material(Material.SMOKER)
                .displayName(Component.text("Mailbox", TextColor.color(84, 115, 255), TextDecoration.BOLD))
                .frameLore(true)
                .lore(Component.text("The mailbox stores items that have", NamedTextColor.GRAY))
                .lore(Component.text("been delivered to your clan.", NamedTextColor.GRAY))
                .glow(this.isFlash())
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (mailbox.isLocked()) {
            UtilMessage.message(player, "Clans", "<red>The mailbox is currently in use by: <dark_red>%s</dark_red>.", mailbox.getLockedBy());
            return;
        }

        mailbox.show(player, previous);
        new SoundEffect(Sound.BLOCK_CHEST_OPEN, 0.8F, 0.7F).play(player.getLocation());
    }

    @Override
    public boolean isFlashing() {
        return !mailbox.getContents().isEmpty();
    }
}
