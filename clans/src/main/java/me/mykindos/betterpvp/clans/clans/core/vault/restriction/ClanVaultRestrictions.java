package me.mykindos.betterpvp.clans.clans.core.vault.restriction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.core.vault.ClanVault;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

@CustomLog
@Singleton
public class ClanVaultRestrictions {

    private final List<VaultRestriction> restrictions = new ArrayList<>();
    private final Clans clans;
    private final RestrictionParser parser;

    @Inject
    private ClanVaultRestrictions(Clans clans, @NotNull RestrictionParser parser) {
        this.clans = clans;
        this.parser = parser;
        reload();
    }

    /**
     * @param vault The clan vault to check
     * @param itemStack The item stack to check
     * @return Empty optional if no restriction is present for this item or the restriction for this item
     *         to be allowed in the given vault.
     */
    public Optional<VaultRestriction> getAvailable(@NotNull ClanVault vault, @NotNull ItemStack itemStack) {
        final List<VaultRestriction> applied = this.restrictions.stream().filter(restriction -> restriction.matches(itemStack)).toList();
        VaultRestriction max = null;
        int maxCount = 0;
        for (VaultRestriction restriction : applied) {
            final OptionalInt current = restriction.getRemainingCount(vault);
            if (current.isEmpty()) {
                return Optional.of(restriction); // Infinite
            }

            if (max == null || current.getAsInt() > maxCount) {
                max = restriction;
                maxCount = current.getAsInt();
            }
        }

        return Optional.ofNullable(max);
    }

    public void reload() {
        restrictions.clear();

        final ExtendedYamlConfiguration config = clans.getConfig("vault-restrictions");
        if (!config.isConfigurationSection("restrictions")) {
            log.error("'restrictions' not found").submit();
            return;
        }

        final ConfigurationSection section = config.getConfigurationSection("restrictions");
        if (section == null) {
            log.error("'restrictions' not found").submit();
            return;
        }

        final Set<String> keys = section.getKeys(false);
        for (String restriction : keys) {
            if (!section.isConfigurationSection(restriction)) {
                log.warn("Invalid restriction: {}", restriction).submit();
                continue;
            }

            final ConfigurationSection restrictionSection = section.getConfigurationSection(restriction);
            if (restrictionSection == null) {
                log.warn("Invalid restriction section: {}", restriction).submit();
                return;
            }


            try {
                addRestriction(this.parser.parse(restrictionSection));
            } catch (Exception e) {
                log.warn("Invalid restriction: " + restriction, e).submit();
            }
        }

        log.info("Loaded {} restrictions", restrictions.size()).submit();
    }

    private void addRestriction(@NotNull VaultRestriction restriction) {
        if (this.restrictions.contains(restriction)) {
            throw new IllegalArgumentException("Cannot add duplicate restriction for type: " + restriction);
        }

        this.restrictions.add(restriction);
    }

}
