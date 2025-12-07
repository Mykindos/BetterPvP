package me.mykindos.betterpvp.progression.profession.woodcutting.item;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;

@Singleton
@ItemKey("progression:compacted_log")
public class CompactedLog extends BaseItem {
    public CompactedLog() {
        super("Compacted Log", ItemView.builder().material(Material.OAK_WOOD).glow(true).build().get(), ItemGroup.BLOCK, ItemRarity.COMMON);
    }
}
