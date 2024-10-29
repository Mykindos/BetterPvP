package me.mykindos.betterpvp.clans.fields;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.clans.clans.listeners.ClanListener;
import me.mykindos.betterpvp.clans.fields.event.FieldsInteractableUseEvent;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientAdministrateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.events.ClanAddExperienceEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import net.kyori.adventure.text.Component;
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
    public FieldsListener(ClanManager clanManager, ClientManager clientManager, Fields fields) {
        super(clanManager, clientManager);
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
        Component message = UtilMessage.deserialize("<yellow>%s</yellow> saved <green>%s</green> interactables changes(s) to the database.", event.getPlayer().getName(), toSave.size());
        clientManager.sendMessageToRank("Fields", message, Rank.HELPER);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdminBreak(BlockBreakEvent event) {
        processBlockEvent(event.getPlayer(), event, event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdminPlace(BlockPlaceEvent event) {
        processBlockEvent(event.getPlayer(), event, event.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
            UtilServer.callEvent(new ClanAddExperienceEvent(event.getPlayer(), 0.1));
        }
    }

    private void processBlockEvent(Player player, BlockEvent event, Block block) {
        if (!isFields(event.getBlock())) {
            return; // ignore if it's not fields
        }

        Client client = clientManager.search().online(player);
        if (!client.isAdministrating()) {
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
            Component message = UtilMessage.deserialize("<yellow>%s</yellow> is now editing interactables in Fields", player.getName());
            clientManager.sendMessageToRank("Fields", message, Rank.HELPER);
            return new HashMap<>();
        }).put(block, blockType);
    }

    @UpdateEvent(delay = 5_000)
    public void respawnOres() {
        final double modifier = fields.getSpeedBuff();
        fields.getBlocks().entries().forEach(entry -> {
            final FieldsBlock interactable = entry.getValue();
            final FieldsInteractable type = entry.getKey();
            if (interactable.isActive()) {
                return; // The block is already the interactable, ignore
            }

            if (!UtilTime.elapsed(interactable.getLastUsed(), (long) (type.getRespawnDelay() * 1_000 / modifier))) {
                return;
            }

            final Block block = interactable.getLocation().getBlock();
            block.setType(type.getType().getMaterial());
            block.setBlockData(interactable.getBlockData() == null ? type.getType() : interactable.getBlockData());
            interactable.setActive(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCatchFish(PlayerCaughtFishEvent event) {
        if (!(event.getLoot() instanceof Fish fish)) return;
        if (!isFields(event.getHook().getLocation().getBlock())) {

            fish.setWeight((int) (fish.getWeight() * 0.50));
            if (UtilMath.randomInt(10) < 2) {
                UtilMessage.simpleMessage(event.getPlayer(), "Fishing", "Fish caught outside of Fields are half their normal size.");
            }
        } else {
            UtilServer.callEvent(new ClanAddExperienceEvent(event.getPlayer(), 0.1));
        }
    }

}
