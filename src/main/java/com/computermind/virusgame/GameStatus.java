package com.computermind.virusgame;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GameStatus {
    private boolean terminada;
    private List<PlayerStatus> players;
    private List<Msg> messages;
}
