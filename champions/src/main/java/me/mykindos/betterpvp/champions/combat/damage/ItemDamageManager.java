package me.mykindos.betterpvp.champions.combat.damage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.List;

@Singleton
public class ItemDamageManager extends Manager<ItemDamageValue> {

    private final ItemDamageRepository repository;

    @Inject
    public ItemDamageManager(ItemDamageRepository repository) {
        this.repository = repository;
        loadFromList(repository.getAll());
    }

    @Override
    public void loadFromList(List<ItemDamageValue> values) {
        values.forEach(value -> objects.put(value.getMaterial().name(), value));
    }

}
