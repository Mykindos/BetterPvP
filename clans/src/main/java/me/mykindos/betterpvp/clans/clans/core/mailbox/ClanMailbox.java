package me.mykindos.betterpvp.clans.clans.core.mailbox;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.repository.ClanRepository;
import me.mykindos.betterpvp.core.menu.Windowed;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Represents a virtual inventory in which a clan can place items.
 */
@Setter
@Getter
public final class ClanMailbox {

    private final Clan clan;
    private final @NotNull ArrayList<ItemStack> contents;
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
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack[] items = new ItemStack[dataInput.readInt()];

        // Read the serialized inventory
        for (int i = 0; i < items.length; i++) {
            contents.add((ItemStack) dataInput.readObject());
        }

        dataInput.close();

    }

    @SneakyThrows
    public String serialize() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        // Write the size of the inventory
        dataOutput.writeInt(contents.size());

        // Save every element in the list
        for (ItemStack item : contents) {
            dataOutput.writeObject(item);
        }

        // Serialize that array
        dataOutput.close();
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

    public void show(Player player, Windowed previous) throws IllegalStateException {
        Preconditions.checkState(!isLocked(), "Clan mailbox is locked");
        lockedBy = player.getName();
        new GuiClanMailbox(this, previous).show(player).addCloseHandler(() -> {
            lockedBy = null;
            JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ClanRepository.class).updateClanMailbox(clan);
        });
    }

    public void show(Player player) throws IllegalStateException {
        show(player, null);
    }
}
