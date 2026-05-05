package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.fishingdurabilityreduction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

@BPvPListener
@Singleton
public class FishingDurabilityReductionAttributeListener implements Listener {

    private final FishingDurabilityReductionAttribute attribute;

    @Inject
    public FishingDurabilityReductionAttributeListener(FishingDurabilityReductionAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (event.getItem().getType() != Material.FISHING_ROD) return;

        Player player = event.getPlayer();
        double bonus = attribute.getDurabilityReductionBonus(player);
        if (bonus <= 0) return;

        int reduced = (int) Math.max(0, event.getDamage() * (1 - bonus));
        event.setDamage(reduced);
    }
}
