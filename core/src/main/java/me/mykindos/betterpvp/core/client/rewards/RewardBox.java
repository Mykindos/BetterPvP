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

    public RewardBox() {
        this.contents = new ArrayList<>();
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

}
