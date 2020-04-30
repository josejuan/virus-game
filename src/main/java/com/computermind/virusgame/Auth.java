package com.computermind.virusgame;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Auth {
    private String playerId;
    private String password;

    public boolean is(Auth a) {
        return playerId.equals(a.playerId) && password.equals(a.password);
    }
}
