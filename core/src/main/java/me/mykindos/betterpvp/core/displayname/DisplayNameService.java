package me.mykindos.betterpvp.core.displayname;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor(onConstructor = @__(@Inject))
@Getter
@Setter
@Singleton
public class DisplayNameService {

    private DisplayNameProvider provider;
}