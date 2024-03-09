package me.mykindos.betterpvp.clans.clans.vault.restriction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public final class RestrictionParser {

    @Inject
    private RestrictionParser() {
    }

    public VaultRestriction parse(ConfigurationSection section) {
        final String type = section.getString("type");
        if (type == null) {
            throw new IllegalArgumentException("Type is not defined");
        }

        final Material material = Material.matchMaterial(type);
        if (material != null) {
            Integer model = section.getObject("model", Integer.class, null);
            if (!section.contains("allowed")) {
                return new TypeRestriction(0, material, model);
            }

            if (section.isInt("allowed")) {
                return new TypeRestriction(section.getInt("allowed"), material, model);
            }

            final List<String> allowed = section.getStringList("allowed");
            return new TypeRestriction(parsePerks(allowed), material, model);
        } else {
            Class<?> clazz = parseClass(type);
            if (!section.contains("allowed")) {
                return new BPvPItemRestriction(0, clazz);
            }

            if (section.isInt("allowed")) {
                return new BPvPItemRestriction(section.getInt("allowed"), clazz);
            }

            final List<String> allowed = section.getStringList("allowed");
            return new BPvPItemRestriction(parsePerks(allowed), clazz);
        }
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
