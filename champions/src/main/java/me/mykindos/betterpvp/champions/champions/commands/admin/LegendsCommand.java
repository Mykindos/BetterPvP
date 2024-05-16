package me.mykindos.betterpvp.champions.champions.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.weapon.WeaponManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.UUID;

@WithReflection
@CustomLog
public class LegendsCommand extends Command {

    private final ClientManager clientManager;
    private final UUIDManager uuidManager;

    private final ItemHandler itemHandler;


    @Inject
    public LegendsCommand(ClientManager clientManager, UUIDManager uuidManager, ItemHandler itemHandler, WeaponManager weaponManager) {
        this.itemHandler = itemHandler;
        this.clientManager = clientManager;
        this.uuidManager = uuidManager;
    }

    @Singleton
    @Override
    public String getName() {
        return "legends";
    }

    @Override
    public String getDescription() {
        return "give the target all legends";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "Command", getUsage());
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            UtilMessage.message(player, "Command", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid player name.", args[0]));
            return;
        }


        for (BPvPItem bPvPItem : itemHandler.getLegends()) {
            ItemStack itemStack = itemHandler.updateNames(bPvPItem.getItemStack());
            itemHandler.updateNames(itemStack);
            ItemMeta itemMeta = itemStack.getItemMeta();
            UUIDItem uuidItem = null;

            if (itemMeta != null) {
                PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
                if (pdc.has(CoreNamespaceKeys.UUID_KEY)) {
                    uuidItem = uuidManager.getObject(UUID.fromString(Objects.requireNonNull(pdc.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING)))).orElse(null);
                }
            }
            if (uuidItem != null) {
                log.info("{} spawned and gave ({}) to {}", player.getName(), uuidItem.getUuid(), target.getName())
                        .setAction("ITEM_SPAWN").addClientContext(player).addClientContext(target, true).addItemContext(uuidItem).submit();

            }
            target.getInventory().addItem(itemStack);
            }
        }

    public Component getUsage() {
        return UtilMessage.deserialize("<yellow>Usage</yellow>: <green>legends <player>");
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }

}
