package me.mykindos.betterpvp.core.combat.combatlog;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerClickCombatLogEvent;
import me.mykindos.betterpvp.core.combat.nms.CombatSheep;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CustomLog
@Getter
public class CombatLog {

    private final UUID owner;
    private final List<ItemStack> items;
    private final long expiryTime;
    private final LivingEntity combatLogSheep;
    private final String playerName;

    public CombatLog(Player player, long expiryTime) {
        this.owner = player.getUniqueId();
        this.expiryTime = expiryTime;
        this.playerName = player.getName();
        items = new ArrayList<>();
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;

            items.add(itemStack);
        }

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

    public void onClicked(Player player) {

        if (Bukkit.getPlayer(owner) != null) {
            return; // Safety check, shouldn't ever be true but just in case
        }

        combatLogSheep.remove();
        for (ItemStack stack : items) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            combatLogSheep.getLocation().getWorld().dropItemNaturally(combatLogSheep.getLocation(), stack);
        }

        UtilServer.callEvent(new PlayerClickCombatLogEvent(player, this));

        UtilMessage.broadcast("Log", "<yellow>%s</yellow> dropped their inventory for combat logging.", playerName);
        File currentPlayerData = new File("world/playerdata", owner + ".dat");
        if (currentPlayerData.exists()) {
            if (!currentPlayerData.delete()) {
                log.error("Failed to delete dat file for player {}", owner).submit();
            }
        }

        File oldPlayerData = new File("world/playerdata", owner + ".dat_old");
        if (oldPlayerData.exists()) {
            if (!oldPlayerData.delete()) {
                log.error("Failed to delete dat_old file for player {}", owner).submit();
            }
        }

    }

    public boolean hasExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
}
