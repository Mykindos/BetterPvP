package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.logging.repository.ClanLogHolderRepository;
import me.mykindos.betterpvp.clans.logging.types.ClanLogHolder;
import me.mykindos.betterpvp.clans.logging.types.formatted.FormattedClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.KillClanLog;
import me.mykindos.betterpvp.clans.logging.types.log.ClanLog;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.logging.type.UUIDType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClanLogHolderManager extends Manager<ClanLogHolder> {
    public final ClanLogHolderRepository repository;

    @Inject
    public ClanLogHolderManager(ClanLogHolderRepository repository) {
        this.repository = repository;
    }

    /**
     * Get the ClanLogs associated with the Clan, updating them if needed
     * @param clan the clan
     * @return the ClanLogs
     */
    public List<FormattedClanLog> getClanLogs(Clan clan) {
        ClanLogHolder clanLogHolder = getOrLoadClanLogHolder(clan);
        if (!clanLogHolder.isClanLogsUpdated()) {
            clanLogHolder.getClanLogs().clear();
            clanLogHolder.getClanLogs().addAll(repository.getClanLogs(clan.getId()));
            clanLogHolder.setClanLogsUpdated(true);
        }
        return clanLogHolder.getClanLogs();
    }

    /**
     * Get the ClanKillLogs associated with the Clan, updating them if needed
     * @param clan the clan
     * @return the ClanKillLogs
     */
    public List<KillClanLog> getClanKillLogs(Clan clan) {
        ClanLogHolder clanLogHolder = getOrLoadClanLogHolder(clan);
        if (!clanLogHolder.isKillLogsUpdated()) {
            clanLogHolder.getClanKillLogs().clear();
            clanLogHolder.getClanKillLogs().addAll(repository.getClanKillLogs(clan));
            clanLogHolder.setKillLogsUpdated(true);
        }
        return clanLogHolder.getClanKillLogs();
    }

    /**
     * Gets the ClanLogHolder object associated with the clan, creates it if it does not exist
     * @param clan the clan
     * @return ClanLogHolder object
     */
    private ClanLogHolder getOrLoadClanLogHolder(Clan clan) {
        Optional<ClanLogHolder> optionalClanLogHolder = getObject(clan.getId());
        if (optionalClanLogHolder.isEmpty()) {
            ClanLogHolder newClanLogHolder = repository.get(clan);
            addObject(newClanLogHolder.getClanID(), newClanLogHolder);
            return newClanLogHolder;
        }
        return optionalClanLogHolder.get();
    }

    /**
     * Adds the associated ClanLog, and notifies the associated ClanLogHolder that its ClanLogs are out of date
     * @param clanLog the clanLog to add to the database
     */
    public void addClanLogs(ClanLog clanLog) {
        repository.addClanLog(clanLog);
        clanLog.getMetaUuidLogList().forEach(metaLog -> {
            if (metaLog.getUuidtype() == UUIDType.CLAN1 || metaLog.getUuidtype() == UUIDType.CLAN2) {
                getObject(metaLog.getUuid()).ifPresent(clanLogHolder -> {
                    clanLogHolder.setClanLogsUpdated(false);
                });
            }
        });
    }

    /**
     * Adds the associated kill meta data, and notifies the associated ClanLogHolder that its ClanLogs are out of date
     * @param killID the KillID
     * @param killerClan the killerClan
     * @param victimClan the victimClan
     * @param dominance the dominance gained from this kill by the Killer
     */
    public void addClanKill(UUID killID, Clan killerClan, Clan victimClan, double dominance) {
        repository.addClanKill(killID, killerClan, victimClan, dominance);
        getObject(killerClan.getId()).ifPresent(clanLogHolder -> {
            clanLogHolder.setKillLogsUpdated(false);
        });
        getObject(victimClan.getId()).ifPresent(clanLogHolder -> {
            clanLogHolder.setKillLogsUpdated(false);
        });
    }

    @Override
    public void loadFromList(List<ClanLogHolder> objects) {
        objects.forEach(object -> {
            addObject(object.getClanID(), object);
        });
    }
}
