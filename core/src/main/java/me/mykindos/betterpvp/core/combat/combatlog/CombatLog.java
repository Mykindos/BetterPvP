package me.mykindos.betterpvp.core.combat.combatlog;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerClickCombatLogEvent;
import me.mykindos.betterpvp.core.combat.nms.CombatSheep;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.WorldHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@CustomLog
@Getter
public class CombatLog {

    private final UUID owner;
    private final long expiryTime;
    private final LivingEntity combatLogSheep;
    private final String playerName;

    public CombatLog(Player player, long expiryTime) {
        this.owner = player.getUniqueId();
        this.expiryTime = expiryTime;
        this.playerName = player.getName();

        CombatSheep combatSheep = new CombatSheep(player.getLocation());
        combatLogSheep = (LivingEntity) combatSheep.spawn();

        setupSheep(player);
    }

    private void setupSheep(Player player) {
        combatLogSheep.customName(Component.text("Right Click Me! ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .append(Component.text(player.getName(), NamedTextColor.GRAY)));
        combatLogSheep.setCustomNameVisible(true);
        combatLogSheep.setRemoveWhenFarAway(false);

    }

    public void onClicked(Player player, WorldHandler worldHandler) {

        if (Bukkit.getPlayer(owner) != null) {
            return; // Safety check, shouldn't ever be true but just in case
        }

        combatLogSheep.remove();
        CraftInventoryPlayer inventory = UtilInventory.getOfflineInventory(playerName, owner);

        for (ItemStack stack : inventory.getContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            combatLogSheep.getLocation().getWorld().dropItemNaturally(combatLogSheep.getLocation(), stack);
        }

        UtilServer.callEvent(new PlayerClickCombatLogEvent(player, this));

        UtilMessage.broadcast("Log", "<yellow>%s</yellow> caused <yellow>%s</yellow> to drop their inventory for combat logging.", player.getName(), playerName);

        inventory.clear();
        UtilInventory.saveOfflineInventory(owner, inventory);
        UtilPlayer.setOfflinePosition(owner, worldHandler.getSpawnLocation());

    }

    public boolean hasExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
}
