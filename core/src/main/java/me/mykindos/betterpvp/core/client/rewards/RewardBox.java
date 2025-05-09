package me.mykindos.betterpvp.core.client.rewards;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Represents a virtual inventory in which clients can claim rewards
 */
@Getter
@Setter
public class RewardBox {


    private @NotNull ArrayList<ItemStack> contents;
    private boolean isLocked;

    public RewardBox() {
        this.contents = new ArrayList<>();
    }

    public void read(String data) {
        try {
            contents.clear();
            if (data != null && !data.isEmpty()) {
                contents = UtilItem.deserializeItemStackList(data);
            }
        } catch (Exception e) {
            // Log the error but don't expose it to the client
            // This prevents potential deserialization attacks
            throw new IllegalArgumentException("Invalid reward data format");
        }
    }

    public String serialize() {
        try {
            return UtilItem.serializeItemStackList(contents);
        } catch (Exception e) {
            // Log the error but don't expose it to the client
            throw new IllegalStateException("Failed to serialize reward data");
        }
    }

}
