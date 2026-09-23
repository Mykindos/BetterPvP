package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** A player's coins, for anything in a camp that costs them. */
@Singleton
public class CampCoins {

    private final ClientManager clientManager;

    @Inject
    public CampCoins(@NotNull ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    /** Takes {@code amount} from {@code player}, or nothing and false if they do not have it. */
    public boolean take(@NotNull Player player, long amount) {
        if (amount <= 0) {
            return true;
        }
        final Gamer gamer = clientManager.search().online(player).getGamer();
        if (gamer.getBalance() < amount) {
            return false;
        }
        gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() - (int) amount);
        return true;
    }

    /** Hands {@code amount} back, such as when what it paid for fell through. */
    public void give(@NotNull Player player, long amount) {
        if (amount <= 0) {
            return;
        }
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() + (int) amount);
    }
}
