package me.mykindos.betterpvp.progression.profession.fishing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.loot.session.LootSessionController;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.fishing.data.CaughtFish;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profession.fishing.leaderboards.BiggestFishLeaderboard;
import me.mykindos.betterpvp.progression.profession.fishing.leaderboards.FishingCountLeaderboard;
import me.mykindos.betterpvp.progression.profession.fishing.leaderboards.FishingWeightLeaderboard;
import me.mykindos.betterpvp.progression.profession.fishing.repository.FishingRepository;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
@CustomLog
@Getter
public class FishingHandler extends ProfessionHandler implements Reloadable {

    @me.mykindos.betterpvp.core.config.Config(path = "fishing.xpPerPound", defaultValue = "0.10")
    @Inject
    private double xpPerPound;

    private final FishingRepository fishingRepository;
    private final LeaderboardManager leaderboardManager;
    private final ItemFactory itemFactory;
    private final LootTableRegistry lootTableRegistry;
    private final LootSessionController sessionController;

    private LootTable lootTable;

    @Inject
    protected FishingHandler(Progression progression, ItemFactory itemFactory, ClientManager clientManager,
                             ProfessionProfileManager professionProfileManager, Provider<ProfessionNodeManager> nodeManager,
                             FishingRepository fishingRepository, LeaderboardManager leaderboardManager,
                             LootTableRegistry lootTableRegistry, LootSessionController sessionController) {
        super(progression, clientManager, professionProfileManager, nodeManager, "Fishing");
        this.fishingRepository = fishingRepository;
        this.leaderboardManager = leaderboardManager;
        this.itemFactory = itemFactory;
        this.lootTableRegistry = lootTableRegistry;
        this.sessionController = sessionController;
    }

    @Override
    public void reload() {
        this.lootTable = lootTableRegistry.loadLootTable("fishing");
    }

    public void addFish(Player player, Fish fish) {
        ProfessionData professionData = getProfessionData(player.getUniqueId());
        if (professionData == null) return;

        double xp = (fish.getWeight() * xpPerPound);
        if (xp > 0) {
            professionData.grantExperience(xp, player);
        }

        log.info("{} caught a {} pound {} for {} experience", player.getName(), fish.getWeight(), fish.getTypeName(), xp)
                .addClientContext(player).addLocationContext(player.getLocation())
                .addContext("Experience", xp + "").addContext("Fish Weight", fish.getWeight() + "").submit();

        fishingRepository.saveFish(clientManager.search().online(player), fish);

        long fishCaught = (long) professionData.getProperties().getOrDefault("TOTAL_FISH_CAUGHT", 0L);
        professionData.getProperties().put("TOTAL_FISH_CAUGHT", fishCaught + 1);

        long weightCaught = (long) professionData.getProperties().getOrDefault("TOTAL_WEIGHT_CAUGHT", 0L);
        professionData.getProperties().put("TOTAL_WEIGHT_CAUGHT", weightCaught + fish.getWeight());

        leaderboardManager.getObject("Total Weight Caught").ifPresent(leaderboard -> {
            FishingWeightLeaderboard fishingWeightLeaderboard = (FishingWeightLeaderboard) leaderboard;
            fishingWeightLeaderboard.add(player.getUniqueId(), (long) fish.getWeight()).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to add weight to leaderboard for player " + player.getName(), throwable).submit();
                    return;
                }
                fishingWeightLeaderboard.attemptAnnounce(player, result);
            });
        });

        leaderboardManager.getObject("Total Fish Caught").ifPresent(leaderboard -> {
            FishingCountLeaderboard fishingCountLeaderboard = (FishingCountLeaderboard) leaderboard;
            fishingCountLeaderboard.add(player.getUniqueId(), 1L).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to add fish count to leaderboard for player " + player.getName(), throwable).submit();
                    return;
                }
                fishingCountLeaderboard.attemptAnnounce(player, result);
            });
        });

        leaderboardManager.getObject("Biggest Fish Caught").ifPresent(leaderboard -> {
            BiggestFishLeaderboard biggestFishLeaderboard = (BiggestFishLeaderboard) leaderboard;
            biggestFishLeaderboard.add(fish.getUuid(), new CaughtFish(player.getUniqueId(), fish.getTypeName(), fish.getWeight())).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to add biggest fish to leaderboard for player " + player.getName(), throwable).submit();
                    return;
                }
                biggestFishLeaderboard.attemptAnnounce(player, result);
            });
        });
    }

    public @NotNull LootBundle getRandomLoot(Player player, Location location) {
        final LootSession session = sessionController.resolve(player, lootTable, () -> LootSession.newSession(lootTable, player));
        final LootContext context = new LootContext(session, location, "Fishing");
        return this.lootTable.generateLoot(context);
    }

    @Override
    public String getName() {
        return "Fishing";
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
    }
}