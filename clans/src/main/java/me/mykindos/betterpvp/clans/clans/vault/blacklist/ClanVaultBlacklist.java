package me.mykindos.betterpvp.clans.clans.vault.blacklist;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.clans.clans.vault.ClanVault;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class ClanVaultBlacklist {

    // By model specific data
    private final Multimap<Material, BlacklistEntry<Integer>> byModel = ArrayListMultimap.create();

    // By material
    private final List<BlacklistEntry<Material>> byMaterial = new ArrayList<>();

    // By superclass
    private final List<BlacklistEntry<Class<?>>> bySuperclass = new ArrayList<>();

    // By specific class
    private final List<BlacklistEntry<Class<?>>> byClass = new ArrayList<>();

    private final Clans clans;
    private final ItemHandler itemHandler;

    @Inject
    private ClanVaultBlacklist(@NotNull Clans clans, @NotNull ItemHandler itemHandler) {
        this.clans = clans;
        this.itemHandler = itemHandler;
        reload();
    }

    public boolean isBlacklisted(@NotNull ClanVault vault, @NotNull ItemStack itemStack) {
        Preconditions.checkNotNull(itemStack, "itemStack");
        // Check by type
        final Material material = itemStack.getType();
        if (byMaterial.stream().anyMatch(entry -> entry.getAllowedCount(vault, material))) {
            return true;
        }

        // Check by model
        if (byModel.containsKey(material)) {
            final ItemMeta meta = itemStack.getItemMeta();
            if (meta != null && meta.hasCustomModelData()) {
                final int model = meta.getCustomModelData();
                if (byModel.get(material).stream().anyMatch(entry -> entry.getAllowedCount(vault, model))) {
                    return true;
                }
            }
        }

        final BPvPItem item = itemHandler.getItem(itemStack);
        if (item == null) {
            return false;
        }

        // Check by class
        final Class<?> clazz = item.getClass();
        if (byClass.stream().anyMatch(entry -> entry.getAllowedCount(vault, clazz))) {
            return true;
        }

        // Check by superclass
        return bySuperclass.stream().anyMatch(entry -> entry.getAllowedCount(vault, clazz));
    }

    public void reload() {
        byModel.clear();
        byMaterial.clear();
        bySuperclass.clear();
        byClass.clear();

        // Load by type
        final ExtendedYamlConfiguration config = clans.getConfig("vault-blacklist");
        for (String entry : config.getStringList("type-blacklist")) {
            final String[] split = entry.split(":");
            if (split.length < 1 || split.length > 3) {
                log.error("Invalid entry in vault-blacklist.type-blacklist: {}", entry);
                continue;
            }

            final Material material = Material.matchMaterial(split[0]);
            if (material == null) {
                log.error("Invalid material in vault-blacklist.type-blacklist: {}", split[0]);
                continue;
            }

            BlacklistEntry<?> blacklistEntry;
            if (split.length == 1 || split[1].isEmpty()) {
                TypeBlacklist typeEntry = new TypeBlacklist(material, null);
                byMaterial.add(typeEntry);
                blacklistEntry = typeEntry;
            } else {
                try {
                    final int model = Integer.parseInt(split[1]);
                    final ModelBlacklist modelBlacklist = new ModelBlacklist(model, null);
                    byModel.put(material, modelBlacklist);
                    blacklistEntry = modelBlacklist;
                } catch (NumberFormatException e) {
                    log.error("Invalid model in vault-blacklist.type-blacklist: {}", split[1]);
                    continue;
                }
            }

            if (split.length == 3) {
                final Optional<ClanPerk> perkOpt = ClanPerkManager.getInstance().getObject(split[2]);
                if (perkOpt.isPresent()) {
//                    blacklistEntry.setRestriction(perkOpt.get());
                } else {
                    log.error("Invalid perk in vault-blacklist.type-blacklist: {}", split[2]);
                }
            }
        }

        // Load by class
        for (String entry : config.getStringList("class-blacklist")) {
            final String[] split = entry.split(":");
            if (split.length < 1 || split.length > 3) {
                log.error("Invalid entry in vault-blacklist.class-blacklist: {}", entry);
                continue;
            }

            BlacklistEntry<?> blacklistEntry;
            try {
                final Class<?> clazz = Class.forName(split[0]);
                if (split.length == 1 || split[1].isEmpty()) {
                    ClassBlacklist classBlacklist = new ClassBlacklist(clazz, null);
                    byClass.add(classBlacklist);
                    blacklistEntry = classBlacklist;
                } else if (split[1].equalsIgnoreCase("all")) {
                    SubclassBlacklist subclassBlacklist = new SubclassBlacklist(clazz, null);
                    bySuperclass.add(subclassBlacklist);
                    blacklistEntry = subclassBlacklist;
                } else {
                    log.error("Invalid class strategy in vault-blacklist.class-blacklist: {}", split[1]);
                    continue;
                }
            } catch (ClassNotFoundException e) {
                log.error("Invalid class in vault-blacklist.class-blacklist: {}", split[0]);
                continue;
            }

            if (split.length == 3) {
                final Optional<ClanPerk> perkOpt = ClanPerkManager.getInstance().getObject(split[2]);
                if (perkOpt.isPresent()) {
                    blacklistEntry.setRestriction(perkOpt.get());
                } else {
                    log.error("Invalid perk in vault-blacklist.class-blacklist: {}", split[2]);
                }
            }
        }
    }

}
