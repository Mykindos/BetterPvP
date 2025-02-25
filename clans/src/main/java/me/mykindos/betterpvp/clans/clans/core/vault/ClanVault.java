package me.mykindos.betterpvp.clans.clans.core.vault;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.clans.clans.leveling.perk.model.ClanVaultSlot;
import me.mykindos.betterpvp.clans.clans.repository.ClanRepository;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents a virtual inventory in which a clan can place items.
 */
@Setter
@Getter
public final class ClanVault {

    private final Clans clans;
    private final Clan clan;
    private @NotNull Map<Integer, @NotNull ItemStack> contents;
    private String lockedBy;

    public ClanVault(Clan clan) {
        this.clans = JavaPlugin.getPlugin(Clans.class);
        this.clan = clan;
        this.contents = new Int2ObjectOpenHashMap<>();
    }

    public boolean hasPermission(Player player) {
        return clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN);
    }

    public boolean isLocked() {
        return lockedBy != null;
    }

    public int getSize() {
        final int baseSize = clans.getConfig().getOrSaveInt("clans.clan.vault.base-size", 9);
        return baseSize + ClanPerkManager.getInstance().getPerks(clan).stream()
                .filter(ClanVaultSlot.class::isInstance)
                .map(ClanVaultSlot.class::cast)
                .mapToInt(ClanVaultSlot::getSlots)
                .sum();
    }

    @SneakyThrows
    public void read(String data) {
        contents.clear();
        contents = UtilItem.deserializeItemStackMap(data);
    }

    @SneakyThrows
    public String serialize() {
        return UtilItem.serializeItemStackMap(contents);
    }

    public void show(Player player, Windowed previous) throws IllegalStateException {
        Preconditions.checkState(!isLocked(), "Clan vault is locked");
        lockedBy = player.getName();
        new GuiClanVault(player, this, previous).show(player).addCloseHandler(() -> {
            lockedBy = null;
            clans.getInjector().getInstance(ClanRepository.class).updateClanVault(clan);
        });
    }

    public void show(Player player) throws IllegalStateException {
        show(player, null);
    }
}
