package me.mykindos.betterpvp.clans.world.ship.crew.menu.button;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.profiles.PlayerProfiles;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.site.crew.Crew;
import me.mykindos.betterpvp.core.world.site.crew.CrewService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Somebody asking to come aboard, shown in the lower band. Left accepts, right turns them down.
 */
@RequiredArgsConstructor
public class CrewRequestButton extends AbstractItem {

    private final CrewService crewService;
    private final ClientManager clientManager;
    private final Crew crew;
    private final Player requester;
    private final Runnable onChange;

    @Override
    public ItemProvider getItemProvider() {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(PlayerProfiles.CACHE.get(requester.getUniqueId(), key -> requester.getPlayerProfile()));
        head.setItemMeta(meta);

        return ItemView.of(head).toBuilder()
                .displayName(Component.text(requester.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(Component.empty())
                .lore(Translations.component("clans.crew.button.wants-to-sail").color(NamedTextColor.GRAY))
                .action(ClickActions.LEFT, Translations.component("clans.crew.button.take-aboard"))
                .action(ClickActions.RIGHT, Translations.component("clans.crew.button.turn-away"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!crew.getCaptain().equals(player.getUniqueId())) {
            return;
        }

        if (clickType.isRightClick()) {
            crewService.decline(crew, requester.getUniqueId());
            new SoundEffect(Sound.UI_BUTTON_CLICK, 0.8f, 0.6f).play(player);
            onChange.run();
            return;
        }

        // A request can outlive the space it was queued for, so a full hull refuses here rather than at the door.
        if (!crewService.accept(crew, requester.getUniqueId())) {
            UtilMessage.message(player, "clans.prefix.crew", "clans.crew.no-room", Component.text(requester.getName()));
            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 1.0f).play(player);
            return;
        }

        new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1.4f, 0.6f).play(player);
        welcome(player);
        onChange.run();
    }

    /** Chat, title and a cue, so being taken aboard registers even with the menu closed. */
    private void welcome(@NotNull Player captain) {
        UtilMessage.message(requester, "clans.prefix.crew", "clans.crew.taken-aboard", Component.text(captain.getName()));
        new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 0.8f).play(requester);
        clientManager.search().online(requester).getGamer().getTitleQueue().add(5, new TitleComponent(0.2, 1.6, 0.4, false,
                gmr -> Translations.component("clans.crew.title.aboard").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                gmr -> Translations.component("clans.crew.title.sailing-with", Component.text(captain.getName()))
                        .color(NamedTextColor.GRAY)));
    }
}
