package me.mykindos.betterpvp.progression.profession.fishing.bait;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

@Singleton
public class SpeedyBait extends BaitWeapon {

    @Inject
    public SpeedyBait(Progression plugin) {
        super(plugin, "speedy_bait");
    }

    @Override
    public void activate(Player player) {
        super.activate(player);
        UtilServer.callEvent(new PlayerThrowBaitEvent(player, new Bait(duration) {
            @Override
            public String getType() {
                return "Speedy";
            }

            @Override
            public Material getMaterial() {
                return Material.ORANGE_GLAZED_TERRACOTTA;
            }

            @Override
            public double getRadius() {
                return radius;
            }

            @Override
            protected void onTrack(FishHook hook) {
                hook.setWaitTime((int) (hook.getWaitTime() / multiplier));
            }
        }));
    }

}
