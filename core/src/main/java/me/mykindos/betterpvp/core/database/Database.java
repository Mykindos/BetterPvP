package me.mykindos.betterpvp.core.database;

import com.google.inject.Singleton;
import lombok.Getter;

import javax.inject.Inject;

@Singleton
public class Database {

    @Getter
    private IDatabaseConnection connection;

    @Inject
    public Database(IDatabaseConnection connection){
        this.connection = connection;
    }

}
