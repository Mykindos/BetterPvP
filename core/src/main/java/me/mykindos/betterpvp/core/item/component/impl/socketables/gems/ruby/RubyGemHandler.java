package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.ruby;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RubyGemHandler {

    private final RubyGem rubyGem;

    @Inject
    public RubyGemHandler(RubyGem rubyGem) {
        this.rubyGem = rubyGem;
    }

}
