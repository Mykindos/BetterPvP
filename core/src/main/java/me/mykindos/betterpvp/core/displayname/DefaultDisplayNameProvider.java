package me.mykindos.betterpvp.core.displayname;

import com.google.inject.Singleton;
import org.bukkit.entity.Entity;

@Singleton
public class DefaultDisplayNameProvider implements DisplayNameProvider {

    @Override
    public String getDisplayName(Entity entity, Entity viewer) {
        return "<yellow>" + entity.getName() + "</yellow>";
    }
}