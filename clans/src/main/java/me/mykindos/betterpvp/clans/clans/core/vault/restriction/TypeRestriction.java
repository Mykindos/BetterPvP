package me.mykindos.betterpvp.clans.clans.core.vault.restriction;

import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

@Getter
@EqualsAndHashCode(of = { "type", "model" }, callSuper = false)
@ToString(of = { "type", "model" })
public final class TypeRestriction extends VaultRestriction {

    private final @NotNull Material type;
    private final @Nullable Key model;

    public TypeRestriction(int allowedCount, @NotNull Material type, @Nullable Key model) {
        super(allowedCount);
        this.type = type;
        this.model = model;
    }

    public TypeRestriction(@NotNull Map<@NotNull ClanPerk, @NotNull Integer> allowedPerks, @NotNull Material type, @Nullable Key model) {
        super(allowedPerks);
        this.type = type;
        this.model = model;
    }

    @Override
    public boolean matches(@NotNull ItemStack itemStack) {
        if (model != null && !Objects.equals(itemStack.getData(DataComponentTypes.ITEM_MODEL), model)) {
            return false;
        }

        return itemStack.getType() == type;
    }

}
