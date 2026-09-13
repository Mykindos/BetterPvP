package me.mykindos.betterpvp.clans.world.ship.crew.menu.button;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.framework.profiles.PlayerProfiles;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
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
 * Somebody already aboard, shown in the top band. Clicking puts them off the ship.
 * <p>
 * The captain's own head is shown but is not a button — a captain leaves by walking off, and offering "remove" on
 * yourself would read as a way to disband that silently strands everyone else.
 */
@RequiredArgsConstructor
public class CrewMemberButton extends AbstractItem {

    private final CrewService crewService;
    private final Crew crew;
    private final Player member;
    private final Runnable onChange;

    private boolean isCaptain() {
        return crew.getCaptain().equals(member.getUniqueId());
    }

    @Override
    public ItemProvider getItemProvider() {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(PlayerProfiles.CACHE.get(member.getUniqueId(), key -> member.getPlayerProfile()));
        head.setItemMeta(meta);

        final ItemView.ItemViewBuilder builder = ItemView.of(head).toBuilder()
                .displayName(Component.text(member.getName(), NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(Component.empty());

        if (isCaptain()) {
            builder.lore(Translations.component("clans.crew.button.captain").color(NamedTextColor.GOLD));
        } else {
            builder.action(ClickActions.LEFT, Translations.component("clans.crew.button.put-ashore"));
        }
        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (isCaptain() || !crew.getCaptain().equals(player.getUniqueId())) {
            return;
        }

        if (crewService.removeMember(crew, member.getUniqueId())) {
            new SoundEffect(Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.1f, 0.7f).play(player);
            UtilMessage.message(member, "clans.prefix.crew", "clans.crew.put-ashore", Component.text(player.getName()));
            new SoundEffect(Sound.BLOCK_WOODEN_DOOR_CLOSE, 0.9f, 0.7f).play(member);
            onChange.run();
        }
    }
}
