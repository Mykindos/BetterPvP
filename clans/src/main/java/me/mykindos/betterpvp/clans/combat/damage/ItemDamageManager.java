package me.mykindos.betterpvp.clans.combat.damage;

import me.mykindos.betterpvp.core.framework.manager.Manager;

import javax.inject.Inject;
import javax.inject.Singleton;
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
