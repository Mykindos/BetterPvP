package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;

@BPvPListener
public class ClanBankListener extends ClanListener {

    @Inject
    @Config(path="clans.bank.interestIntervalInHours", defaultValue = "24")
    private int interestIntervalInHours;

    @Inject
    @Config(path="clans.bank.interestIntervalDeviation", defaultValue = "6")
    private int interestIntervalDeviation;

    @Inject
    @Config(path="clans.bank.interestRate", defaultValue = "2.5")
    private double interestRate;

    @Inject
    public ClanBankListener(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

}
