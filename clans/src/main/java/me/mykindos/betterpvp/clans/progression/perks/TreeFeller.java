package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.EnchantedLumberfall;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.TreeFellerSkill;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerUsesTreeFellerEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

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
    private final ItemHandler itemHandler;
    private final BlockTagManager blockTagManager;

    @Inject
    public TreeFeller(ClanManager clanManager, ItemHandler itemHandler, BlockTagManager blockTagManager) {
        this.clanManager = clanManager;
        this.itemHandler = itemHandler;
        this.blockTagManager = blockTagManager;
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

        Player player = event.getPlayer();
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        if (!UtilItem.isAxe(itemInMainHand) &&
            !itemHandler.getItem("champions:hyper_axe").matches(itemInMainHand)) return;

        Optional<ProgressionSkill> progressionSkillOptional = progressionSkillManager.getSkill("Tree Feller");
        if (progressionSkillOptional.isEmpty()) return;

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

            // If EnchantedLumberfall triggered, then this location will be where the special item gets dropped
            Location locationToActivatePerk = fellTree(
                    player, playerClan, event.getChoppedLogBlock(), event,
                    null
            );

            // Reset the player's felled blocks
            treeFellerSkill.blocksFelledByPlayer.put(player.getUniqueId(), 0);

            UtilServer.callEvent(new PlayerUsesTreeFellerEvent(
                    player, locationToActivatePerk, event.getChoppedLogBlock().getLocation(),
                    event.getLogType()
            ));

            treeFellerSkill.whenPlayerUsesSkill(player, skillLevel);
        });
    }


    /**
     * Removes the logs of the tree
     * <br>
     * Removes the leaves of the tree if the player has <b>No More Leaves</b>
     *
     * @param player     the player who activated Tree Feller
     * @param playerClan the Clan of the player who activated Tree Feller
     * @param block      the current log or leaf block
     * @param event      the PlayerChopLogEvent instance
     * @return the set of all leaf locations for the felled tree
     */
    public Location fellTree(Player player, Clan playerClan, Block block,
                             PlayerChopLogEvent event,
                             @Nullable Location locationToActivatePerk) {

        UUID playerUUID = player.getUniqueId();
        int blocksFelled = treeFellerSkill.blocksFelledByPlayer.getOrDefault(playerUUID, 0);
        if (blocksFelled >= treeFellerSkill.getMaxBlocksThatCanBeFelled()) return locationToActivatePerk;

        if (blockTagManager.isPlayerPlaced(block)) return null;

        UtilBlock.breakBlockNaturally(block, player, woodcuttingHandler.getEffectManager());

        treeFellerSkill.blocksFelledByPlayer.put(playerUUID, blocksFelled + 1);
        event.setAmountChopped(event.getAmountChopped() + 1);

        Location newLocToActivatePerk = locationToActivatePerk;


        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 1; y++) {
                    Block targetBlock = block.getRelative(x, y, z);

                    Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(targetBlock.getLocation());
                    if (targetBlockLocationClanOptional.isPresent()) {
                        if (!targetBlockLocationClanOptional.get().equals(playerClan)) continue;
                    }

                    /*
                    We only want to get the first leaves block encountered. Any other leaves dont matter
                    Also, we need to make sure the player did not place the block. If a player placed
                    a block next to a leaf, we need to check that and prevent giving a special item for that
                     */
                    if (targetBlock.getType().name().contains("LEAVES")) {
                        if (enchantedLumberfall.doesPlayerHaveSkill(player) && !blockTagManager.isPlayerPlaced(block) && locationToActivatePerk == null) {
                            newLocToActivatePerk = targetBlock.getLocation();
                        }
                    }

                    if (targetBlock.getType().name().contains("_LOG")) {

                        Location returnedLocation = fellTree(
                                player, playerClan, targetBlock, event,
                                newLocToActivatePerk
                        );

                        if (newLocToActivatePerk == null && returnedLocation != null) {
                            newLocToActivatePerk = returnedLocation;
                        }
                    }
                }
            }
        }

        return newLocToActivatePerk;
    }
}
