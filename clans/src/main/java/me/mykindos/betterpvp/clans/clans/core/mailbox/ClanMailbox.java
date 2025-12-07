package me.mykindos.betterpvp.clans.clans.core.mailbox;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.repository.ClanRepository;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Represents a virtual inventory in which a clan can place items.
 */
@Setter
@Getter
@CustomLog
public final class ClanMailbox {

    private final Clan clan;
    private @NotNull ArrayList<ItemStack> contents;
    private String lockedBy;

    public ClanMailbox(Clan clan) {
        this.clan = clan;
        this.contents = new ArrayList<>();
    }

    public boolean isLocked() {
        return lockedBy != null;
    }

    @SneakyThrows
    public void read(String data) {
        contents.clear();
        contents.addAll(UtilItem.deserializeItemStackList(data));
    }

    @SneakyThrows
    public String serialize() {
        return UtilItem.serializeItemStackList(contents);
    }

    public void show(Player player, ItemFactory itemFactory, Windowed previous) throws IllegalStateException {
        Preconditions.checkState(!isLocked(), "Clan mailbox is locked");
        lockedBy = player.getName();
        new GuiClanMailbox(this, itemFactory, previous).show(player).addCloseHandler(() -> {
            ClanRepository instance = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ClanRepository.class);
            instance.updateClanMailbox(clan).whenComplete((unused, throwable) -> {
                if(throwable != null) {
                    log.error("Failed to update clan mailbox", throwable).submit();
                    return;
                }

                lockedBy = null;
            });
        });
    }

    public void show(Player player, ItemFactory itemFactory) throws IllegalStateException {
        show(player, itemFactory, null);
    }
}
