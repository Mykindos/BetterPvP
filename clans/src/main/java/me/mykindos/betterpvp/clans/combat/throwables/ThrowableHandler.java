package me.mykindos.betterpvp.clans.combat.throwables;

import com.google.inject.Singleton;
import lombok.Getter;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class ThrowableHandler {

    @Getter
    private final List<ThrowableItem> throwables = new ArrayList<>();

    public void addThrowable(Item item, LivingEntity entity, String name, long expire){
        throwables.add(new ThrowableItem(item, entity, name, expire));
    }

    public Optional<ThrowableItem> getThrowable(Item item) {
        return throwables.stream().filter(throwable -> throwable.getItem().equals(item)).findFirst();

    }

}
