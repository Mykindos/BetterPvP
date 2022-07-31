package me.mykindos.betterpvp.core.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientRepository;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class ClientManager extends Manager<Client> {

    @Getter
    private final ClientRepository repository;

    @Inject
    public ClientManager(ClientRepository repository){
        this.repository = repository;
        loadFromList(repository.getAll());
    }

    public Optional<Client> getObject(UUID uuid) {
        return getObject(uuid.toString());
    }

    @Override
    public void loadFromList(List<Client> objects) {
        objects.forEach(client -> addObject(client.getUuid(), client));
    }

    public Optional<Client> getClientByName(String name) {
        for(Player player : Bukkit.getOnlinePlayers()){
            if(player.getName().equalsIgnoreCase(name)) {
                return getObject(player.getUniqueId().toString());
            }
        }

        return objects.values().stream().filter(client -> client.getName().equalsIgnoreCase(name)).findFirst();

    }

}
