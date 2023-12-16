package me.mykindos.betterpvp.core.combat.click.events;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@AllArgsConstructor
public class RightClickEvent extends CustomEvent {

    public static int DEFAULT_SHIELD = 0;
    public static int INVISIBLE_SHIELD = 1;

    private final Player player;
    private boolean useShield;
    private int shieldModelData;
    private boolean isHoldClick;
    private EquipmentSlot hand;

}
