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

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

            // If noMoreLeaves triggered, then this location will be where the special item gets dropped
            Location locationToDropItem = fellTree(
                    player, playerClan, event.getChoppedLogBlock(), event,
                    null
            );

            // Reset the player's felled blocks
            treeFellerSkill.blocksFelledByPlayer.put(player.getUniqueId(), 0);

            if (enchantedLumberfall.doesPlayerHaveSkill(player) && locationToDropItem != null) {
                enchantedLumberfall.whenSkillTriggers(player, locationToDropItem);
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
     * @return the set of all leaf locations for the felled tree
     */
    public Location fellTree(Player player, Clan playerClan, Block block,
                                           PlayerChopLogEvent event,
                                           @Nullable Location locationToDropItem) {
        if (woodcuttingHandler.didPlayerPlaceBlock(block)) return null;

        UUID playerUUID = player.getUniqueId();
        int blocksFelled = treeFellerSkill.blocksFelledByPlayer.getOrDefault(playerUUID, 0);

        if (blocksFelled >= treeFellerSkill.getMaxBlocksThatCanBeFelled()) return locationToDropItem;

        block.breakNaturally();
        treeFellerSkill.blocksFelledByPlayer.put(playerUUID, blocksFelled + 1);
        event.setAmountChopped(event.getAmountChopped() + 1);

        Location newLocationToDropItem = locationToDropItem;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block targetBlock = block.getRelative(x, 1, z);

                Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(targetBlock.getLocation());
                if (targetBlockLocationClanOptional.isPresent()) {
                    if (!targetBlockLocationClanOptional.get().equals(playerClan)) continue;
                }

                /*
                We only want to get the first leaves block encountered. Any other leaves dont matter
                Also, we need to make sure the player did not place the block. If the block is forest flourisher, then
                initialBlock and the first if-statement of this method should handle that but if a player placed
                a block next to a leaf, we need to check that and prevent giving a special item for that
                 */
                if (targetBlock.getType().name().contains("LEAVES")
                        && enchantedLumberfall.doesPlayerHaveSkill(player)
                        && !woodcuttingHandler.didPlayerPlaceBlock(block)
                ){

                    if (locationToDropItem == null) {
                        newLocationToDropItem = targetBlock.getLocation();
                    }
                }

                if (targetBlock.getType().name().contains("_LOG")) {

                    Location returnedLocation = fellTree(
                            player, playerClan, targetBlock, event,
                            newLocationToDropItem
                    );

                    if (newLocationToDropItem == null && returnedLocation != null) {
                        newLocationToDropItem = returnedLocation;
                    }
                }
            }
        }

        return newLocationToDropItem;
    }
}
