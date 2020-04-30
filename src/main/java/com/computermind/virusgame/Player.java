package com.computermind.virusgame;

import com.computermind.sfp.Either;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static com.computermind.sfp.Either.guard;
import static com.computermind.sfp.Either.left;
import static com.computermind.virusgame.Carta.OCULTA;
import static java.util.stream.Collectors.toList;

@Getter
public class Player {
    private final Auth auth;
    private final List<Carta> mano;
    private final List<List<Carta>> jugada;

    public Player(Auth auth, List<Carta> cartas) {
        this.auth = auth;
        this.mano = new CopyOnWriteArrayList<>(cartas);
        this.jugada = new CopyOnWriteArrayList<>();
    }

    public PlayerStatus getStatus(Auth auth_, boolean currentPlayer) {
        PlayerStatus ps = new PlayerStatus();
        ps.setId(auth.getPlayerId());
        ps.setName(getName());
        ps.setMano(auth.is(auth_) ? mano : mano.stream().map(ignore -> OCULTA).collect(toList()));
        ps.setJugada(getJugada());
        ps.setCurrent(currentPlayer);
        return ps;
    }

    public Stream<Carta> getOrganos() {
        return getJugada().stream().flatMap(Collection::stream).filter(c -> c.isOrgano());
    }

    public void addOrgano(Carta carta) {
        if (!carta.isOrgano())
            throw new IllegalStateException("se esperaba un órgano");
        List<Carta> xs = new CopyOnWriteArrayList<>();
        xs.add(carta);
        jugada.add(xs);
    }

    public void setJugada(List<List<Carta>> xs) {
        jugada.clear();
        jugada.addAll(xs);
    }

    public Either<String, List<Carta>> getJugada(Carta organo) {
        return guard(organo.isOrgano(), "La carta debe ser un órgano")
                .bind(ignore -> getJugada()
                        .stream()
                        .filter(xs -> organo.equals(xs.get(0)))
                        .<Either<String, List<Carta>>>map(Either::right)
                        .findAny()
                        .orElse(left("No existe ese órgano en la jugada")));
    }

    public String getName() {
        return getAuth().getPlayerId();
    }

    public void quitaDeMano(Carta carta) {
        if (getMano().contains(carta))
            getMano().remove(carta);
        else
            throw new IllegalStateException("no se puede quitar esa carta de la mano!!!");
    }
}
