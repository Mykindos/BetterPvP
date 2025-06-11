package me.mykindos.betterpvp.core.client.stats.formatter.category;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;
@Getter
@RequiredArgsConstructor
public abstract class StatCategory implements IStatCategory {
    private final String name;
    private final Set<IStatCategory> children = new HashSet<>();
}
