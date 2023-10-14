package me.mykindos.betterpvp.clans.fields;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.clans.clans.listeners.ClanListener;
import me.mykindos.betterpvp.clans.fields.event.FieldsInteractableUseEvent;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.core.client.events.ClientAdministrateEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.exceptions.NoSuchGamerException;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@BPvPListener
public class FieldsListener extends ClanListener {

    private final WeakHashMap<Player, Map<Block, FieldsInteractable>> profiles = new WeakHashMap<>();

    private Fields fields;

    @Inject
    public FieldsListener(ClanManager clanManager, GamerManager gamerManager, Fields fields) {
        super(clanManager, gamerManager);
        this.fields = fields;
    }

    private boolean isFields(Block block) {
        return clanManager.getClanByLocation(block.getLocation())
                .map(c -> c.getName().equalsIgnoreCase("Fields"))
                .orElse(false);
    }

    @EventHandler
    public void onAdministrate(ClientAdministrateEvent event) {
        if (event.isAdministrating()) {
            return;
        }

        // If they stop administrating, save their profiles
        final Map<Block, FieldsInteractable> toSave = profiles.remove(event.getPlayer());
        if (toSave == null || toSave.isEmpty()) {
            return; // If they didn't edit any interactables, ignore
        }

        fields.addBlocks(toSave);
        UtilMessage.message(event.getPlayer(), "Fields", "Saved <alt2>%s</alt2> interactables change(s) to the database.", toSave.size());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdminBreak(BlockBreakEvent event) {
         processBlockEvent(event.getPlayer(), event, event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdminPlace(BlockPlaceEvent event) {
        processBlockEvent(event.getPlayer(), event, event.getBlockPlaced());
    }

    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOreMine(TerritoryInteractEvent event) {
        if (event.getResult() != TerritoryInteractEvent.Result.DENY) {
            return; // If they're allowed to edit the claim, this means we should not interfere with that
        }

        if (!event.getTerritoryOwner().getName().equalsIgnoreCase("Fields")) {
            return; // If they're not in the Fields zone, ignore
        }

        // Attempt to find the block they're mining
        final Optional<Pair<FieldsInteractable, FieldsBlock>> interactOpt = fields.getBlock(event.getBlock());
        if (interactOpt.isEmpty()) {
            return; // If they're not interacting with a block, ignore
        }

        // If they're interacting with a block
        final FieldsInteractable type = interactOpt.get().getLeft();
        final FieldsBlock block = interactOpt.get().getRight();
        if (!block.isActive()) {
            return; // If the block isn't active yet, ignore
        }

        final boolean allow = type.processInteraction(event, block);

        if (allow) {
            event.setInform(false); // Block the message that they cant break
            block.setLastUsed(System.currentTimeMillis());
            block.setActive(false);
            UtilServer.callEvent(new FieldsInteractableUseEvent(fields, type, block, event.getPlayer()));
            event.getBlock().setType(type.getReplacement().getMaterial()); // Then replace the block
            event.getBlock().setBlockData(type.getReplacement());
        }
    }

    private void processBlockEvent(Player player, BlockEvent event, Block block) {
        if (!isFields(event.getBlock())) {
            return; // ignore if it's not fields
        }

        Gamer gamer = gamerManager.getObject(player.getUniqueId().toString()).orElseThrow(() -> new NoSuchGamerException(player.getName()));
        if (!gamer.getClient().isAdministrating()) {
            return; // if they're not admin, skip
        }

        // If they're admin
        final FieldsInteractable blockType;
        if (event instanceof BlockPlaceEvent) {
            Optional<FieldsInteractable> typeOpt = fields.getTypeFromBlock(block);
            if (typeOpt.isEmpty()) {
                return; // Cancel if what they placed isn't an interactable
            }

            blockType = typeOpt.get();
        } else {
            final Optional<Pair<FieldsInteractable, FieldsBlock>> interactable = fields.getBlock(event.getBlock());
            if (interactable.isEmpty() && profiles.values().stream().noneMatch(map -> map.containsKey(block))) {
                // They didn't break a registered interactable
                // If they didn't break an interactable, and they're not editing one in an edit profile, ignore
                return;
            }
            blockType = null; // Set the interactable type to null because they broke it
        }

        // If they're admin, and they placed an interactable, then save it
        profiles.computeIfAbsent(player, p -> {
            UtilMessage.message(player, "Fields", "<red>You are now editing interactables in Fields. <u>Your changes will be saved when you stop administrating.</u></red>");
            return new HashMap<>();
        }).put(block, blockType);
    }

    @UpdateEvent(delay = 100)
    public void respawnOres() {
        final double modifier = fields.getSpeedBuff();
        fields.getBlocks().entries()
                .forEach(entry -> {
                    final FieldsBlock interactable = entry.getValue();
                    final FieldsInteractable type = entry.getKey();
                    if (!UtilTime.elapsed(interactable.getLastUsed(), (long) (type.getRespawnDelay() * 1_000 / modifier))) {
                        return;
                    }

                    if (interactable.isActive()) {
                        return; // The block is already the interactable, ignore
                    }

                    final Block block = interactable.getLocation().getBlock();
                    block.setType(type.getType().getMaterial());
                    block.setBlockData(type.getType());
                    interactable.setActive(true);
                });
    }

}
