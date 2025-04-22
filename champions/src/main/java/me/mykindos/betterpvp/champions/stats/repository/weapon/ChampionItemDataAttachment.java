package me.mykindos.betterpvp.champions.stats.repository.weapon;

import com.google.common.collect.ConcurrentHashMultiset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.champions.stats.ChampionsKill;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsCombatData;
import me.mykindos.betterpvp.core.combat.stats.model.ICombatDataAttachment;
import me.mykindos.betterpvp.core.combat.weapon.types.IRune;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@CustomLog
public class ChampionItemDataAttachment implements ICombatDataAttachment<ChampionsCombatData, ChampionsKill> {

    private final ItemHandler itemHandler;
    private final Set<String> allowedIdentifiers;

    @Getter(AccessLevel.NONE)
    private final ConcurrentHashMultiset<ChampionsKillItemData> pendingWeaponData = ConcurrentHashMultiset.create();

    @Override
    public void prepareUpdates(@NotNull ChampionsCombatData data, @NotNull Database database) {
        log.info("preparing item update").submit();
        final List<Statement> itemStatements = new ArrayList<>();
        final List<Statement> runeStatements = new ArrayList<>();
        final String itemStmt = "INSERT INTO champions_kills_items (WeaponId, KillId, Player, Weapon) VALUES (?, ?, ?, ?);";
        final String runeStmt = "INSERT INTO champions_kills_items_rune (WeaponId, Rune, Tier, Data) VALUES (?, ?, ?, ?)";

        for (final ChampionsKillItemData itemData : pendingWeaponData) {
            final UUID killId = itemData.getKill().getId();
            for (Map.Entry<UUID, Set<BPvPItem.SerializedItem>> playerEntry : itemData.getPlayerItems().entrySet()) {
                final UUID player = playerEntry.getKey();
                for (BPvPItem.SerializedItem serializedItem : playerEntry.getValue()) {
                    itemStatements.add(new Statement(itemStmt,
                            new UuidStatementValue(serializedItem.getId()),
                            new UuidStatementValue(killId),
                            new UuidStatementValue(player),
                            new StringStatementValue(serializedItem.getIdentifier())
                    ));
                    for (Map.Entry<NamespacedKey, IRune.RuneData> runeEntry : serializedItem.getRunes().entrySet()) {
                        IRune.RuneData runeData = runeEntry.getValue();
                        //todo move this into RuneData
                        List<String> runeValues = runeData.getData().entrySet().stream()
                                .map((entry) -> entry.getKey() + ": " + entry.getValue())
                                .toList();
                        String runeInfo = String.join(" | ", runeValues);
                        runeStatements.add(new Statement(runeStmt,
                                new UuidStatementValue(serializedItem.getId()),
                                new StringStatementValue(runeEntry.getKey().toString()),
                                new IntegerStatementValue(runeData.getTier()),
                                new StringStatementValue(runeInfo)
                                ));
                    }
                }
            }
        }

        database.executeBatch(itemStatements, false);
        database.executeBatch(runeStatements, false);
        pendingWeaponData.clear();

    }

    @Override
    public void onKill(@NotNull ChampionsCombatData data, ChampionsKill kill) {
        List<Player> players = new ArrayList<>();
        ChampionsKillItemData itemData = new ChampionsKillItemData(kill);

        players.add(Bukkit.getPlayer(kill.getKiller()));
        players.add(Bukkit.getPlayer(kill.getVictim()));
        players.addAll(kill.getContributions().stream()
                        .filter(contribution -> Bukkit.getPlayer(contribution.getContributor()) != null)
                        .map(contribution -> Objects.requireNonNull(Bukkit.getPlayer(contribution.getContributor())
                        )).toList()
        );
        players = players.stream().filter(Objects::nonNull).toList();
        for (Player player : players) {
            log.info(player.getName()).submit();
            Set<BPvPItem.SerializedItem> items = new HashSet<>();
            PlayerInventory inventory = player.getInventory();
            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null || itemStack.getType() == Material.AIR) continue;
                BPvPItem item = itemHandler.getItem(itemStack);
                if (item == null) continue;
                if (!allowedIdentifiers.contains(item.getIdentifier())) continue;
                BPvPItem.SerializedItem serializedItem = new BPvPItem.SerializedItem(itemStack);

                items.add(serializedItem);
                log.info(serializedItem.toString()).submit();

            }
            //todo iterate through armor and only add if runed
            itemData.getPlayerItems().put(player.getUniqueId(), items);
        }

        pendingWeaponData.add(itemData);
    }
}
