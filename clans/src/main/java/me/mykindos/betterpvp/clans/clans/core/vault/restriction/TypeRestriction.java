package me.mykindos.betterpvp.clans.clans.core.vault.restriction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Getter
@EqualsAndHashCode(of = { "type", "customModelData" }, callSuper = false)
@ToString(of = { "type", "customModelData" })
public final class TypeRestriction extends VaultRestriction {

    private final @NotNull Material type;
    private final @Nullable Integer customModelData;

    public TypeRestriction(int allowedCount, @NotNull Material type, @Nullable Integer customModelData) {
        super(allowedCount);
        this.type = type;
        this.customModelData = customModelData;
    }

    public TypeRestriction(@NotNull Map<@NotNull ClanPerk, @NotNull Integer> allowedPerks, @NotNull Material type, @Nullable Integer customModelData) {
        super(allowedPerks);
        this.type = type;
        this.customModelData = customModelData;
    }

    @Override
    public boolean matches(@NotNull ItemStack itemStack) {
        if (customModelData != null) {
            final ItemMeta meta = itemStack.getItemMeta();
            if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) {
                return false; // Custom model data does not match
            }
        }

        return itemStack.getType() == type;
    }

}
