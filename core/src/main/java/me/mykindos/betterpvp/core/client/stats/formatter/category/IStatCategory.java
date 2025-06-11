package me.mykindos.betterpvp.core.client.stats.formatter.category;

import me.mykindos.betterpvp.core.utilities.model.IStringName;
import me.mykindos.betterpvp.core.utilities.model.description.Describable;

import java.util.Set;

public interface IStatCategory extends Describable, IStringName {
    Set<IStatCategory> getChildren();
}
