package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

@Singleton
@BPvPListener
public class RunedPickaxe extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private double velocityStrength;
    private final EnergyHandler energyHandler;
    private final ClientManager clientManager;
    private  Set<UUID> cooldownPlayers;

    @Inject
    public RunedPickaxe(Champions champions, EnergyHandler energyHandler, ClientManager clientManager) {
        super(champions, "runed_pickaxe");
        this.energyHandler = energyHandler;
        this.clientManager = clientManager;
        this.cooldownPlayers = new HashSet<>();
    }

    //TODO add better lore lol / make the utilmessage more "clear" to users
    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("What an interesting design this", NamedTextColor.WHITE));
        lore.add(Component.text("pickaxe seems to have!", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<yellow>Minimum Energy <white>must be <green>50%"));
        lore.add(UtilMessage.deserialize("<white>to use instant <green>Mine"));
        return lore;
    }


    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerLeftClick(PlayerInteractEvent event) {

        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        if (!enabled) {
            return;
        }

        if (player.getInventory().getItemInMainHand().getType() != Material.FIREWORK_STAR) {
            return;
        }

        if (!canUse(player)) {
            return;
        }

        Block block = event.getClickedBlock();


        if (!energyHandler.use(player, "Runed Pickaxe", energyPerTick, true)) {
            cooldownPlayers.add(player.getUniqueId());
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0F, 1.0F);
            return;
        }


        if (block != null ) {
            if (block.getType() == Material.BEDROCK) {return;}

            BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
            Bukkit.getServer().getPluginManager().callEvent(blockBreakEvent);

            if (blockBreakEvent.isCancelled()) {
                return;
            }
            block.breakNaturally();
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_LAVA_POP, 1.0F, 1.0F);
            //TODO balance energy decrements (current value is just random)
            energyHandler.degenerateEnergy(player, 0.0075);

        }
    }



    @Override
    public boolean canUse(Player player) {
        if (energyHandler.getEnergy(player) >= 0.5){
            cooldownPlayers.remove(player.getUniqueId());
            return true;
        } else return !(energyHandler.getEnergy(player) < 0.5) || !cooldownPlayers.contains(player.getUniqueId());
    }


    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

}
