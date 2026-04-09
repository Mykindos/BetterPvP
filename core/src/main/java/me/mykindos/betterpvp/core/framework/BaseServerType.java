package me.mykindos.betterpvp.core.framework;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BaseServerType implements ServerType {
    private final String name;
}
