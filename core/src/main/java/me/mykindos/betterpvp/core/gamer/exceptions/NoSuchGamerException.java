package me.mykindos.betterpvp.core.gamer.exceptions;

import java.util.UUID;

public class NoSuchGamerException extends RuntimeException {

    public NoSuchGamerException(String message){
        super(message);
    }

    public NoSuchGamerException(UUID uuid){
        super(uuid.toString());
    }
}
