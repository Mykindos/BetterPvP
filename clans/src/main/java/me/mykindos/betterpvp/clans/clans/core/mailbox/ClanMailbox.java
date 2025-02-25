package me.mykindos.betterpvp.clans.clans.core.mailbox;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.repository.ClanRepository;
import me.mykindos.betterpvp.core.items.ItemHandler;
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
        contents = UtilItem.deserializeItemStackList(data);
    }

    @SneakyThrows
    public String serialize() {
        return UtilItem.serializeItemStackList(contents);
    }

    public void show(Player player, ItemHandler itemHandler, Windowed previous) throws IllegalStateException {
        Preconditions.checkState(!isLocked(), "Clan mailbox is locked");
        lockedBy = player.getName();
        new GuiClanMailbox(this, itemHandler, previous).show(player).addCloseHandler(() -> {
            lockedBy = null;
            JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ClanRepository.class).updateClanMailbox(clan);
        });
    }

    public void show(Player player, ItemHandler itemHandler) throws IllegalStateException {
        show(player, itemHandler, null);
    }
}
