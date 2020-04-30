package com.computermind.virusgame;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlayerStatus {
    private String id;
    private String name;
    private List<Carta> mano;
    private List<List<Carta>> jugada;
    private boolean current;
}
