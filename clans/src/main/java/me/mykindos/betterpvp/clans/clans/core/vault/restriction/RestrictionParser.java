package me.mykindos.betterpvp.clans.clans.core.vault.restriction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
public final class RestrictionParser {

    private final ItemRegistry itemRegistry;

    @Inject
    private RestrictionParser(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public VaultRestriction parse(ConfigurationSection section) {
        if (section.contains("type")) {
            final String typeStr = Objects.requireNonNull(section.getString("type"));
            final Material material = Material.matchMaterial(typeStr);
            if (material == null) {
                throw new IllegalArgumentException("Invalid material type: " + typeStr);
            }
            Integer model = section.getObject("model", Integer.class, null);
        
            return createTypeRestriction(section, material, model);
        }
    
        if (section.contains("rarity")) {
            final String rarityStr = Objects.requireNonNull(section.getString("rarity"));
            ItemRarity rarity = ItemRarity.valueOf(rarityStr.toUpperCase());
        
            return createRarityRestriction(section, rarity);
        }
    
        if (section.contains("item")) {
            final String itemKey = Objects.requireNonNull(section.getString("item"));
            final BaseItem item = itemRegistry.getItem(itemKey);
            return createItemRestriction(section, item);
        }
    
        throw new IllegalArgumentException("Invalid restriction configuration: " + section.getCurrentPath());
    }

    private TypeRestriction createTypeRestriction(ConfigurationSection section, Material material, Integer model) {
        if (!section.contains("allowed")) {
            return new TypeRestriction(0, material, model);
        }
        if (section.isInt("allowed")) {
            return new TypeRestriction(section.getInt("allowed"), material, model);
        }
        final List<String> allowed = section.getStringList("allowed");
        return new TypeRestriction(parsePerks(allowed), material, model);
    }

    private RarityRestriction createRarityRestriction(ConfigurationSection section, ItemRarity rarity) {
        if (!section.contains("allowed")) {
            return new RarityRestriction(0, rarity);
        }
        if (section.isInt("allowed")) {
            return new RarityRestriction(section.getInt("allowed"), rarity);
        }
        final List<String> allowed = section.getStringList("allowed");
        return new RarityRestriction(parsePerks(allowed), rarity);
    }

    private ItemRestriction createItemRestriction(ConfigurationSection section, BaseItem item) {
        if (!section.contains("allowed")) {
            return new ItemRestriction(0, item);
        }
        if (section.isInt("allowed")) {
            return new ItemRestriction(section.getInt("allowed"), item);
        }
        final List<String> allowed = section.getStringList("allowed");
        return new ItemRestriction(parsePerks(allowed), item);
    }

    private @NotNull Map<ClanPerk, @NotNull Integer> parsePerks(List<String> allowed) {
        final Map<ClanPerk, Integer> perks = new HashMap<>();
        for (String perk : allowed) {
            String[] split = perk.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Invalid perk: " + perk);
            }

            ClanPerk found = ClanPerkManager.getInstance().getObjects().get(split[0]);
            if (found == null) {
                throw new IllegalArgumentException("Invalid perk: " + split[0]);
            }

            if (perks.containsKey(found)) {
                throw new IllegalArgumentException("Duplicate perk: " + split[0]);
            }

            int level = Integer.parseInt(split[1]);
            perks.put(found, level);
        }
        return perks;
    }

    private Class<?> parseClass(String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

}