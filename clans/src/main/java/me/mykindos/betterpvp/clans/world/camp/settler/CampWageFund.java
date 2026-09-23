package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.core.world.settler.wage.CoinAccount;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** The coins a camp keeps to pay its settlers, on the camp record. */
@Singleton
public class CampWageFund implements CoinAccount {

    private final CampStore store;

    @Inject
    public CampWageFund(@NotNull CampStore store) {
        this.store = store;
    }

    @Override
    public long balance(@NotNull SiteKey site) {
        return store.cached(site.getOwnerId()).map(Camp::getWageFund).orElse(0L);
    }

    @Override
    public void withdraw(@NotNull SiteKey site, long amount) {
        store.cached(site.getOwnerId()).ifPresent(camp -> {
            camp.setWageFund(Math.max(0, camp.getWageFund() - amount));
            store.changed(site.getOwnerId());
        });
    }

    public void deposit(@NotNull SiteKey site, long amount) {
        store.cached(site.getOwnerId()).ifPresent(camp -> {
            camp.setWageFund(camp.getWageFund() + amount);
            store.changed(site.getOwnerId());
        });
    }
}
