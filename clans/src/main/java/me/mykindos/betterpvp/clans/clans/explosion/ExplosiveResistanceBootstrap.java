package me.mykindos.betterpvp.clans.clans.explosion;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.item.ItemLoader;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Material;

import java.util.List;

@Singleton
public class ExplosiveResistanceBootstrap {

    private final ItemLoader itemLoader;

    @Inject
    private ExplosiveResistanceBootstrap(Clans clans) {
        this.itemLoader = new ItemLoader(clans);
    }

    public void register() {
        for (ExplosiveResistantBlocks tree : ExplosiveResistantBlocks.values()) {
            // Hardest -> Weakest
            final List<Material> tiers = tree.getTiers();
            Preconditions.checkState(tiers.size() > 1, "Explosive resistance tree must have at least 2 tiers");
            final Material first = tiers.getFirst();
            final TranslatableComponent name = Component.translatable(first.translationKey());

            final int treeSize = tiers.size();
            for (int i = 0; i < treeSize; i++) {
                int explosiveResistance = treeSize - (i + 1);
                final Material material = tiers.get(i);
                final VanillaItem item = new VanillaItem(name, material, ItemRarity.COMMON);
                item.addBaseComponent(new ExplosiveResistanceComponent(explosiveResistance));
                itemLoader.registerFallbackItem(material.getKey(), item, material, true);
            }
        }
    }

}
