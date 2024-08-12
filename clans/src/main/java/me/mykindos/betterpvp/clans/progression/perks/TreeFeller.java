package me.mykindos.betterpvp.clans.progression.perks;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.EnchantedLumberfall;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.TreeFellerSkill;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class TreeFeller implements Listener {

    private final ClanManager clanManager;
    private final ProfessionProfileManager professionProfileManager;
    private final ProgressionSkillManager progressionSkillManager;
    private final WoodcuttingHandler woodcuttingHandler;
    private final TreeFellerSkill treeFellerSkill;
    private final EnchantedLumberfall enchantedLumberfall;

    @Inject
    public TreeFeller(ClanManager clanManager) {
        this.clanManager = clanManager;
        final Progression progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.professionProfileManager = progression.getInjector().getInstance(ProfessionProfileManager.class);
        this.progressionSkillManager = progression.getInjector().getInstance(ProgressionSkillManager.class);
        this.woodcuttingHandler = progression.getInjector().getInstance(WoodcuttingHandler.class);
        this.treeFellerSkill = progression.getInjector().getInstance(TreeFellerSkill.class);
        this.enchantedLumberfall = progression.getInjector().getInstance(EnchantedLumberfall.class);
    }

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

            ImmutableSet<Location> initialLeafLocations = ImmutableSet.of();

            // Arbitrarily large number
            final int LOWEST_LEAF_Y_LEVEL = 5000;

            ImmutableSet<Location> lowestLeafLocations = fellTree(
                    player, playerClan, event.getChoppedLogBlock(), event, true,
                    initialLeafLocations, LOWEST_LEAF_Y_LEVEL
            );

            player.sendMessage("is it empty? " + lowestLeafLocations.isEmpty());
            if (enchantedLumberfall.doesPlayerHaveSkill(player) && !lowestLeafLocations.isEmpty()) {
                enchantedLumberfall.whenSkillTriggers(player, lowestLeafLocations);
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
     * @param event the PlayerChopLogEvent instance
     * @param initialBlock the initial log block that was chopped
     * @param leafLocations immutable set of all leaf locations for the felled tree
     * @return the set of all leaf locations for the felled tree
     */
    public ImmutableSet<Location> fellTree(Player player, Clan playerClan, Block block,
                                           PlayerChopLogEvent event, boolean initialBlock,
                                           ImmutableSet<Location> leafLocations, int lowestLeafYLevel) {
        if (!initialBlock && woodcuttingHandler.didPlayerPlaceBlock(block)) return leafLocations;

        block.breakNaturally();
        event.setAmountChopped(event.getAmountChopped() + 1);

        final Set<Location> leafLocationsAsMutableSet = new HashSet<>(leafLocations);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block targetBlock = block.getRelative(x, 1, z);

                Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(targetBlock.getLocation());
                if (targetBlockLocationClanOptional.isPresent()) {
                    if (!targetBlockLocationClanOptional.get().equals(playerClan)) continue;
                }

                int currentLowestYLevel = lowestLeafYLevel;

                if (targetBlock.getType().name().contains("LEAVES") && enchantedLumberfall.doesPlayerHaveSkill(player)) {
                    if (targetBlock.getY() <= lowestLeafYLevel) {
                        leafLocationsAsMutableSet.add(targetBlock.getLocation());
                        currentLowestYLevel = targetBlock.getY();
                    }
                }

                if (targetBlock.getType().name().contains("_LOG")) {

                    ImmutableSet<Location> currentlyFelledLocations = ImmutableSet.copyOf(leafLocationsAsMutableSet);
                    ImmutableSet<Location> felledLocations = fellTree(
                            player, playerClan, targetBlock, event, false,
                            currentlyFelledLocations, currentLowestYLevel
                    );
                    leafLocationsAsMutableSet.addAll(felledLocations);
                }
            }
        }

        return ImmutableSet.copyOf(leafLocationsAsMutableSet);
    }
}
