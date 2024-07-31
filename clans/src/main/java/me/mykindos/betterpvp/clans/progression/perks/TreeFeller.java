package me.mykindos.betterpvp.clans.progression.perks;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemDropEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.NoMoreLeaves;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.TreeFellerSkill;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingLoot;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@CustomLog
@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class TreeFeller implements Listener {

    private final ClanManager clanManager;
    private final ProfessionProfileManager professionProfileManager;
    private final ProgressionSkillManager progressionSkillManager;
    private final WoodcuttingHandler woodcuttingHandler;
    private final TreeFellerSkill treeFellerSkill;
    private final NoMoreLeaves noMoreLeaves;

    @Inject
    public TreeFeller(ClanManager clanManager) {
        this.clanManager = clanManager;
        final Progression progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.professionProfileManager = progression.getInjector().getInstance(ProfessionProfileManager.class);
        this.progressionSkillManager = progression.getInjector().getInstance(ProgressionSkillManager.class);
        this.woodcuttingHandler = progression.getInjector().getInstance(WoodcuttingHandler.class);
        this.treeFellerSkill = progression.getInjector().getInstance(TreeFellerSkill.class);
        this.noMoreLeaves = progression.getInjector().getInstance(NoMoreLeaves.class);

    }

    /**
     * Handles Tree Feller ability logic like cooldowns and checking that the player actually has the ability
     */
    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        if (event.isCancelled()) return;

        Optional<ProgressionSkill> progressionSkillOptional = progressionSkillManager.getSkill("Tree Feller");
        if(progressionSkillOptional.isEmpty()) return;

        Player player = event.getPlayer();
        if (!player.getInventory().getItemInMainHand().getType().name().contains("_AXE")) return;

        ProgressionSkill skill = progressionSkillOptional.get();

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {

            var profession = profile.getProfessionDataMap().get("Woodcutting");
            if (profession == null) return;

            int skillLevel = profession.getBuild().getSkillLevel(skill);
            if (skillLevel <= 0) return;

            Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

            if (treeFellerSkill.getCooldownManager().hasCooldown(player, treeFellerSkill.getName())) {
                treeFellerSkill.whenPlayerCantUseSkill(player);
                return;
            }

            event.setCancelled(true);

            UUID felledTreeUUID = UUID.randomUUID();
            noMoreLeaves.felledTreeLeavesMap.put(felledTreeUUID, 0);
            ImmutableSet<Location> initialLocations = ImmutableSet.of();
            ImmutableSet<Location> felledLeavesLocations = fellTree(
                    player, playerClan, event.getChoppedLogBlock(), felledTreeUUID,
                    event, true, initialLocations
            );
            noMoreLeaves.felledTreeLeavesMap.remove(felledTreeUUID);

            List<Location> felledLeavesLocationsAsList = felledLeavesLocations.asList();

            if (noMoreLeaves.doesPlayerHaveSkill(player) && !felledLeavesLocationsAsList.isEmpty()) {

                final Random RANDOM = new Random();
                int randomIndex = RANDOM.nextInt(felledLeavesLocationsAsList.size());
                Location randomLocation = felledLeavesLocationsAsList.get(randomIndex);

                WoodcuttingLoot woodcuttingLoot = woodcuttingHandler.getLootTypes().getRandomLoot(
                        noMoreLeaves.specialDropChance(skillLevel)
                );

                final int count = RANDOM.ints(woodcuttingLoot.getMinAmount(), woodcuttingLoot.getMaxAmount() + 1)
                        .findFirst()
                        .orElse(woodcuttingLoot.getMinAmount());

                final ItemStack itemStack = new ItemStack(woodcuttingLoot.getMaterial(), count);
                itemStack.editMeta(meta -> meta.setCustomModelData(woodcuttingLoot.getCustomModelData()));

                final Item item = event.getChoppedLogBlock().getWorld().dropItem(randomLocation, itemStack);
                item.setItemStack(itemStack);

                UtilServer.callEvent(new SpecialItemDropEvent(item, "Woodcutting"));

                log.info("{} found {}x {}.", player.getName(), count, woodcuttingLoot.getMaterial().name().toLowerCase())
                        .addClientContext(player).addLocationContext(item.getLocation()).submit();
            }

            treeFellerSkill.whenPlayerUsesSkill(player, skillLevel);
        });
    }

    /**
     * Removes the logs of the tree
     * <br>
     * Removes the leaves of the tree if the player has <b>No More Leaves</b>
     *
     * @param player the player who activated Tree Feller
     * @param playerClan the Clan of the player who activated Tree Feller
     * @param block the current log or leaf block
     * @param felledTreeUUID UUID for the tree being felled
     * @param event the PlayerChopLogEvent instance
     * @param initialBlock the initial log block that was chopped
     * @param leafLocations immutable set of all leaf locations for the felled tree
     * @return the set of all leaf locations for the felled tree
     */
    public ImmutableSet<Location> fellTree(Player player, Clan playerClan, Block block,
                                           UUID felledTreeUUID, PlayerChopLogEvent event,
                                           boolean initialBlock, ImmutableSet<Location> leafLocations) {

        // Initial block may not be necessary anymore; was used for old forest flourisher logic
        if (!initialBlock && woodcuttingHandler.didPlayerPlaceBlock(block)) return leafLocations;
        if (event.getAmountChopped() >= treeFellerSkill.getMaxFellableLogs()) return leafLocations;

        // I don't like to reassign arguments so that's why this variable initialization is here
        ImmutableSet<Location> leafLocationsWithAddedLocation = leafLocations;

        if (UtilBlock.isLeaves(block.getType())) {
            if (noMoreLeaves.doesPlayerHaveSkill(player)) {
                // If the player has No More Leaves, then this is where the extra logic for that happens

                int leavesCount = noMoreLeaves.felledTreeLeavesMap.get(felledTreeUUID);
                if (leavesCount >= noMoreLeaves.maxLeavesCount(noMoreLeaves.getPlayerSkillLevel(player))) {
                    return leafLocations;
                }

                leafLocationsWithAddedLocation = new ImmutableSet.Builder<Location>()
                        .addAll(leafLocations)
                        .add(block.getLocation())
                        .build();

                noMoreLeaves.felledTreeLeavesMap.put(felledTreeUUID, leavesCount + 1);
            }
        } else {
            event.setAmountChopped(event.getAmountChopped() + 1);
        }

        final Set<Location> leafLocationsAsMutableSet = new HashSet<>(leafLocationsWithAddedLocation);

        // Utilized to remove both leaves and logs
        block.breakNaturally();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block targetBlock = block.getRelative(x, 1, z);

                boolean shouldRemoveLeaves = (
                        noMoreLeaves.doesPlayerHaveSkill(player) && UtilBlock.isLeaves(targetBlock.getType())
                );

                if (UtilBlock.isLog(targetBlock.getType()) || shouldRemoveLeaves) {

                    Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(targetBlock.getLocation());
                    if (targetBlockLocationClanOptional.isPresent()) {
                        if (!targetBlockLocationClanOptional.get().equals(playerClan)) continue;
                    }

                    ImmutableSet<Location> felledLocations = fellTree(player, playerClan, targetBlock, felledTreeUUID, event, false, leafLocationsWithAddedLocation);
                    leafLocationsAsMutableSet.addAll(felledLocations);
                }
            }
        }

        return ImmutableSet.copyOf(leafLocationsAsMutableSet);
    }
}
