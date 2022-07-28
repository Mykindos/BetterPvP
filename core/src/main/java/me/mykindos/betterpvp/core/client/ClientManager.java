package me.mykindos.betterpvp.core.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientRepository;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.List;

@Singleton
public class ClientManager extends Manager<Client> {

    @Getter
    private final ClientRepository repository;

    @Inject
    public ClientManager(ClientRepository repository){
        this.repository = repository;
        loadFromList(repository.getAll());
    }

    @Override
    public void loadFromList(List<Client> objects) {
        objects.forEach(client -> addObject(client.getUuid(), client));
    }

}
