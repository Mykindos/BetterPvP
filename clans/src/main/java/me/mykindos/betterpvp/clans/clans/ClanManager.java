package me.mykindos.betterpvp.clans.clans;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.List;
import java.util.Optional;

@Singleton
public class ClanManager extends Manager<Clan> {

    public Optional<Clan> getClanById(int id){
        return objects.values().stream().filter(clan -> clan.getId() == id).findFirst();
    }

    @Override
    public void loadFromList(List<Clan> objects) {
        objects.forEach(clan -> addObject(clan.getName(), clan));
    }
}
