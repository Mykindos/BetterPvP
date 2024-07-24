package me.mykindos.betterpvp.core.combat.armour;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class ArmourListener implements Listener {

    private final ArmourManager armourManager;

    @Inject
    public ArmourListener(ArmourManager armourManager) {
        this.armourManager = armourManager;
    }

    @EventHandler
    public void onLoreUpdate(ItemUpdateLoreEvent event) {
        if(!UtilItem.isArmour(event.getItemStack().getType())) return;
        event.getItemLore().clear();
        event.getItemLore().add(UtilMessage.deserialize("Damage reduction: <yellow>" + armourManager.getReductionForItem(event.getItemStack()) + "%"));
    }

}
