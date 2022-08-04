package me.mykindos.betterpvp.core.framework;


import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class ModuleLoadedEvent extends CustomCancellableEvent {

    private final String moduleName;

}
