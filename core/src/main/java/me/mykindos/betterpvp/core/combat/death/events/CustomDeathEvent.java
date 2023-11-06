package me.mykindos.betterpvp.core.combat.death.events;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomDeathEvent extends CustomCancellableEvent {

    private Function<LivingEntity, Component> nameFormat = entity -> Component.text(entity.getName());

    // The person to receive the death message
    private final Player receiver;

    // The entity that was killed
    private final LivingEntity killed;
    private LivingEntity killer;
    private String[] reason;

    public Component getKillerName() {
        Preconditions.checkNotNull(killer, "Killer is null");
        return nameFormat.apply(killer);
    }

    public Component getKilledName() {
        return nameFormat.apply(killed);
    }

}
