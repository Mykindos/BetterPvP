package me.mykindos.betterpvp.core.framework;


import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class ModuleLoadedEvent extends CustomEvent {

    private final String moduleName;

}
