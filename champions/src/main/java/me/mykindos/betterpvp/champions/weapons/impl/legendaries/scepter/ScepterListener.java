package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scepter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@BPvPListener
@Singleton
public class ScepterListener implements Listener {

    @Inject
    private Scepter scepter;

    @UpdateEvent
    public void doBlackHole() {
        final Iterator<Map.Entry<Player, List<BlackHole>>> iterator = scepter.blackHoles.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, List<BlackHole>> cur = iterator.next();
            final Player player = cur.getKey();
            final List<BlackHole> blackHoles = cur.getValue();
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final Iterator<BlackHole> holes = blackHoles.iterator();
            while (holes.hasNext()) {
                final BlackHole hole = holes.next();
                if (hole.isMarkForRemoval()) {
                    holes.remove();
                    continue;
                }

                hole.tick();
            }
        }
    }

}
