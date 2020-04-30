package com.computermind.virusgame;

import com.computermind.sfp.Either;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.computermind.sfp.Either.guard;
import static com.computermind.sfp.Either.left;
import static com.computermind.sfp.Either.maybe;
import static com.computermind.sfp.Either.right;
import static com.computermind.virusgame.Carta.TRATAMIENTO_DESCARTE;
import static com.computermind.virusgame.Carta.TRATAMIENTO_INFECCION;
import static com.computermind.virusgame.Carta.TRATAMIENTO_ROBAR_ORGANO;
import static com.computermind.virusgame.Carta.TRATAMIENTO_TRANSPLANTA_1;
import static com.computermind.virusgame.Carta.TRATAMIENTO_TRANSPLANTA_TODO;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class VirusGame {
    private final String password;
    private final List<Player> players;
    private final List<Carta> mazo;
    private final List<Carta> ozam;
    private final List<Msg> messages;
    private boolean finDeLaPartida;

    /**
     * -1, juego no empezado
     */
    private int currentPlayer = -1;

    public VirusGame(String password) {
        this.password = password;
        this.players = new CopyOnWriteArrayList<>();
        this.mazo = crearMazo();
        this.ozam = new CopyOnWriteArrayList<>();
        this.messages = new CopyOnWriteArrayList<>();
        this.finDeLaPartida = false;
    }

    private static List<Carta> crearMazo() {
        final List<Carta> m = new CopyOnWriteArrayList<>();
        m.add(TRATAMIENTO_TRANSPLANTA_TODO);
        m.add(Carta.TRATAMIENTO_DESCARTE);
        m.add(Carta.TRATAMIENTO_INFECCION);
        m.add(Carta.TRATAMIENTO_INFECCION);
        for (int i = 0; i < 3; i++) {
            m.add(Carta.TRATAMIENTO_ROBAR_ORGANO);
            m.add(Carta.TRATAMIENTO_TRANSPLANTA_1);
        }
        m.add(Carta.ORGANO_COMODIN);
        m.add(Carta.VIRUS_COMODIN);
        m.add(Carta.MEDICINA_COMODIN);
        for (int i = 0; i < 4; i++) {
            m.add(Carta.VIRUS_1);
            m.add(Carta.VIRUS_2);
            m.add(Carta.VIRUS_3);
            m.add(Carta.VIRUS_4);

            m.add(Carta.MEDICINA_1);
            m.add(Carta.MEDICINA_2);
            m.add(Carta.MEDICINA_3);
            m.add(Carta.MEDICINA_4);
        }
        for (int i = 0; i < 5; i++) {
            m.add(Carta.ORGANO_1);
            m.add(Carta.ORGANO_2);
            m.add(Carta.ORGANO_3);
            m.add(Carta.ORGANO_4);
        }
        Collections.shuffle(m);
        return m;
    }

    private static boolean isInmune(List<Carta> jugada) {
        return jugada.size() > 2 && jugada.stream().filter(c -> c.isOrgano()).count() == 1 && jugada.stream().filter(c -> c.isMedicina()).count() == 2;
    }

    private static boolean jugadaSana(List<Carta> jugada) {
        return jugada != null && !jugada.isEmpty() && jugada.stream().noneMatch(c -> c.isVirus());
    }

    private static boolean jugadaGanadora(Player player) {
        return player.getJugada().stream().filter(VirusGame::jugadaSana).count() > 3;
    }

    private Either<String, Player> player(String playerId) {
        for (Player p : players)
            if (p.getName().equals(playerId))
                return right(p);
        return left("El jugador no existe");
    }

    public Either<String, Void> join(Auth auth) {
        if (isPlaying())
            return left("El juego ya ha empezado, no puedes unirte!");
        return player(auth.getPlayerId())
                .either(ignore -> {
                    players.add(new Player(auth, coge3cartas()));
                    msg("'%s' se une a la partida!", auth.getPlayerId());
                    return right(null);
                }, ignore -> left("El jugador ya existe en la sala!"));
    }

    private boolean isPlaying() {
        return currentPlayer >= 0;
    }

    public Either<String, Void> start(String password) {
        return asAdmin(password)
                .guard(ignore -> !isPlaying(), "El juego ya ha empezado, no puede iniciarse otra vez!")
                .guard(ignore -> players.size() > 0, "¡No hay jugadores aún!")
                .map(ignore -> {
                    currentPlayer = (int) (Math.random() * players.size());
                    msg("¡Empieza la partida '%s'!", players.get(currentPlayer).getName());
                    return null;
                });
    }

    private boolean isAdmin(String password) {
        return this.password.equals(password);
    }

    private Either<String, Void> asAdmin(String password) {
        return isAdmin(password) ? right(null) : left("Sólo el administrador puede hacer eso");
    }

    private boolean isCurrentPlayer(Player e) {
        return isPlaying() && players.get(currentPlayer) == e;
    }

    private Either<String, Player> findPlayer(String aplayer) {
        return maybe(players.stream().filter(q -> q.getName().equals(aplayer)).findAny(), "El jugador '" + aplayer + "' no existe!");
    }

    private Either<String, Player> asPlayer(Auth player) {
        return findPlayer(player.getPlayerId()).guard(p -> p.getAuth().is(player), "El jugador no existe o contraseña inválida!");
    }

    private Either<String, Player> asCurrentPlayer(Auth player) {
        return asPlayer(player).guard(this::isCurrentPlayer, "No es el turno de '" + player.getPlayerId() + "'");
    }

    private List<Carta> coge3cartas() {
        return asList(cogeCarta(), cogeCarta(), cogeCarta());
    }

    private Carta cogeCarta() {
        if (mazo.isEmpty()) {
            if (ozam.isEmpty())
                throw new IllegalStateException("sin cartas :/");
            mazo.addAll(ozam);
            ozam.clear();
            Collections.shuffle(mazo);
        }
        return mazo.remove(0);
    }

    public Either<String, GameStatus> status(Auth auth) {
        final GameStatus st = new GameStatus();
        st.setTerminada(finDeLaPartida);
        st.setMessages(messages);
        st.setPlayers(players.stream().map(e -> e.getStatus(auth, isCurrentPlayer(e))).collect(toList()));
        return right(st);
    }

    public Either<String, Void> tirar(Auth auth, Carta tirando) {
        return asCurrentPlayer(auth)
                .bind(this::isJugando)
                .guard(p -> (tirando != null && p.getMano().contains(tirando)) || (tirando == null && p.getMano().size() < 3), "¡Tienes 3 cartas en la mano, debes usar o tirar una aquí!")
                .bind(p -> {
                    if (tirando != null) {
                        p.quitaDeMano(tirando);
                        ozam.add(tirando);
                        msg("'%s' tira '%s'", p.getName(), tirando.getName());
                    }
                    return ganaOrobayturno(p);
                });
    }

    private Either<String, Void> ganaOrobayturno(Player p) {
        if (jugadaGanadora(p))
            finDeLaPartida = true;
        else {
            p.getMano().add(cogeCarta());
            msg("'%s' roba carta", p.getName());
            currentPlayer = (currentPlayer + 1) % players.size();
            msg("Es el turno de '%s'", players.get(currentPlayer).getName());
        }
        return right(null);
    }

    private void msg(String format, Object... args) {
        messages.add(new Msg(false, String.format(format, args)));
    }

    public Either<String, Void> aplayer(Auth auth, Carta carta, String aplayer) {
        return asCurrentPlayer(auth)
                .bind(this::isJugando)
                .guard(p -> carta.isOrgano() || TRATAMIENTO_TRANSPLANTA_TODO.equals(carta), "Esa carta no puede aplicarse a un jugador")
                .guard(p -> !carta.isOrgano() || p.getName().equals(aplayer), "No puedes pasar un órgano a otro jugador")
                .guard(p -> !carta.isOrgano() || p.getOrganos().noneMatch(carta::equals), "Ese órgano ya lo tienes")
                .bind(p -> findPlayer(aplayer)
                        .map(ap -> {
                            p.quitaDeMano(carta);
                            if (carta.isOrgano()) {
                                msg("'%s' se añade un '%s'", p.getName(), carta.getName());
                                p.addOrgano(carta);
                            } else if (TRATAMIENTO_TRANSPLANTA_TODO.equals(carta)) {
                                msg("¡'%s' hace transplante total con '%s'!", p.getName(), aplayer);
                                final List<List<Carta>> xs = new ArrayList<>(p.getJugada());
                                p.setJugada(ap.getJugada());
                                ap.setJugada(xs);
                            } else {
                                throw new IllegalStateException("not implemented!");
                            }
                            return null;
                        })
                        .bind(ignore -> ganaOrobayturno(p)));
    }

    public Either<String, Void> usar(Auth auth, Carta carta) {
        return asCurrentPlayer(auth)
                .bind(this::isJugando)
                .guard(ignore -> TRATAMIENTO_DESCARTE.equals(carta) || TRATAMIENTO_INFECCION.equals(carta),
                        "Sólo '" + TRATAMIENTO_DESCARTE.getName() + "' y '" + TRATAMIENTO_INFECCION.getName() + "' pueden usarse aquí")
                .bind(player -> TRATAMIENTO_DESCARTE.equals(carta) ? usarDescarte(player) : usarInfeccion(player))
                .bind(ignore -> tirar(auth, carta));
    }

    private Either<String, Void> usarInfeccion(Player player) {
        while (posibleInfeccion(player)) ;
        return right(null);
    }

    private boolean posibleInfeccion(Player player) {
        for (List<Carta> jugada : player.getJugada())
            if (posibleInfeccion(player, jugada))
                return true;
        return false;
    }

    private boolean posibleInfeccion(Player player, List<Carta> jugada) {
        for (Carta carta : new ArrayList<>(jugada))
            if (carta.isVirus() && posibleInfeccion(player, jugada, carta))
                return true;
        return false;
    }

    private boolean posibleInfeccion(Player player, List<Carta> jugada, Carta virus) {
        // aleatoriamente elegimos los jugadores candidatos
        List<Player> ps = new ArrayList<>(players);
        Collections.shuffle(ps);
        for (Player p : ps)
            if (p != player && posibleInfeccion(player, jugada, virus, p))
                return true;
        return false;
    }

    private boolean posibleInfeccion(Player player, List<Carta> jugada, Carta virus, Player otro) {
        // lo intenta con todos los órganos a ver que pasa, como puede haber comodín, lo hace aleatoriamente
        List<Carta> organos = new ArrayList<>(Carta.getOrganos());
        Collections.shuffle(organos);
        for (Carta organo : organos)
            if (aOtroUnVirus(player, otro, virus, organo)
                    .withRight(tirar -> {
                        if (tirar)
                            ozam.add(virus);
                        jugada.remove(virus);
                    })
                    .isRight())
                return true;
        return false;
    }

    private Either<String, Void> usarDescarte(Player player) {
        for (Player p : players)
            if (p != player) {
                ozam.addAll(p.getMano());
                p.getMano().clear();
            }
        msg("¡Todos los jugadores excepto '%s' se quedan sin cartas!", player.getName());
        return right(null);
    }

    public Either<String, Void> acarta(Auth auth, Carta cartaA, String aPlayer, Carta cartaB) {
        return asCurrentPlayer(auth)
                .bind(this::isJugando)
                .guard(ignore -> cartaB.isOrgano(), "Las cartas de destino siempre tienen que ser un órgano")
                .bind(playerA -> findPlayer(aPlayer)
                        .bind(playerB -> playerA.getAuth().is(playerB.getAuth()) ? aSuCarta(playerA, cartaA, cartaB) : aOtroCarta(playerA, playerB, cartaA, cartaB))
                        .bind(tirala -> tirala ? tirar(auth, cartaA) : ganaOrobayturno(playerA).withRight(ignore -> playerA.quitaDeMano(cartaA))));
    }

    private Either<String, Boolean> aOtroCarta(Player pa, Player pb, Carta carta, Carta organo) {
        if (carta.isVirus())
            return aOtroUnVirus(pa, pb, carta, organo);

        if (carta.isOrgano())
            return aOtroTransplanta(pa, pb, carta, organo);

        if (TRATAMIENTO_ROBAR_ORGANO.equals(carta))
            return aOtroRobar(pa, pb, organo);

        return left("Al órgano de un oponente sólo puedes aplicar o un virus, o un transplante (envía tu peor órgano a su mejor órgano) o un robo");
    }

    private Either<String, Boolean> aOtroRobar(Player pa, Player pb, Carta organoB) {
        return pb
                .getJugada(organoB)
                .mapLeft(ignore -> "El jugador '" + pb.getName() + "' no tiene un '" + organoB.getName() + "'")
                .guard(jugada -> !isInmune(jugada), "No puedes robar un órgano inmune")
                .guard(ignore -> pa.getJugada(organoB).isLeft(), "Ya tienes ese órgano, no lo puedes robar.")
                .map(jugadaB -> {
                    pb.getJugada().remove(jugadaB);
                    pa.getJugada().add(jugadaB);
                    msg("'%s' roba el '%s' a '%s'", pa.getName(), organoB.getName(), pb.getName());
                    return true;
                });
    }

    private Either<String, Boolean> aOtroTransplanta(Player pa, Player pb, Carta organoA, Carta organoB) {
        return guard(pa.getMano().stream().anyMatch(TRATAMIENTO_TRANSPLANTA_1::equals), "Para poder transplantar un órgano tienes que tener la carta '" + TRATAMIENTO_TRANSPLANTA_1.getName() + "'")
                .guard(ignore -> pb.getJugada(organoA).isLeft() || organoA.equals(organoB),
                        "El jugador '" + pb.getName() + "' ya tiene un '" + organoA.getName() + "'")
                .guard(ignore -> pa.getJugada(organoB).isLeft() || organoA.equals(organoB),
                        "Ya tienes un '" + organoB.getName() + "'")
                .bind(ignore -> pa.getJugada(organoA).mapLeft(ignore2 -> "No tienes ese órgano")
                        .bind(jugadaA -> pb.getJugada(organoB).mapLeft(ignore2 -> "No tiene ese órgano")
                                .guard(jugadaB -> !isInmune(jugadaB), "No puedes trasplantar un órgano inmune")
                                .bind(jugadaB -> {
                                    // usamos la carta de transplante
                                    if (!pa.getMano().remove(TRATAMIENTO_TRANSPLANTA_1))
                                        return left("No se ha podido quitar la carta de transplante!?");
                                    ozam.add(TRATAMIENTO_TRANSPLANTA_1);
                                    // intercambiamos
                                    pa.getJugada().remove(jugadaA);
                                    pb.getJugada().remove(jugadaB);
                                    pa.getJugada().add(jugadaB);
                                    pb.getJugada().add(jugadaA);
                                    msg("'%s' cambia su '%s' por el '%s' de '%s'", pa.getName(), organoA.getName(), organoB.getName(), pb.getName());
                                    return right(false);
                                })));
    }

    private Either<String, Boolean> aOtroUnVirus(Player pa, Player pb, Carta carta, Carta organo) {
        return pb.getJugada(organo)
                .guard(ignore -> organo.admite(carta), "Un " + organo.getName() + " no admite " + carta.getName())
                .guard(jugada -> !isInmune(jugada), "No puedes poner un virus en un órgano inmune")
                .bind(jugada -> {
                    for (Carta c : new ArrayList<>(jugada))
                        if (c.isMedicina()) {
                            // si hay alguna medicina se cancela
                            msg("El '%s' anula '%s'", carta.getName(), c.getName());
                            ozam.add(c);
                            jugada.remove(c);
                            return right(true);
                        } else if (c.isVirus()) {
                            // si hay otro virus matan el órgano
                            msg("El jugador '%s' pierde su órgano '%s'", pb.getName(), organo.getName());
                            ozam.addAll(jugada);
                            pb.getJugada().remove(jugada);
                            return right(true);
                        }
                    jugada.add(carta);
                    msg("El '%s' de '%s' queda infectado por '%s'", organo.getName(), pb.getName(), carta.getName());
                    return right(false);
                });
    }

    private Either<String, Boolean> aSuCarta(Player player, Carta medicina, Carta organo) {
        return guard(medicina.isMedicina(), "Sólo puedes aplicarte medicinas")
                .guard(ignore -> organo.admite(medicina), "Esa medicina no sirve para este órgano")
                .bind(ignore -> player.getJugada(organo))
                .bind(jugada -> {
                    for (Carta c : jugada)
                        if (c.isVirus() && (organo.isComodin() || c.admite(medicina))) {
                            msg("'%s' aplica '%s' al '%s'", player.getName(), medicina.getName(), c.getName());
                            jugada.remove(c);
                            // hemos aplicado para quitar un virus
                            return right(true);
                        }
                    // no hay virus posible
                    if (jugada.stream().anyMatch(c -> c.isVirus()))
                        return left("Ouch! esta medicina no cura cierto virus?!");
                    if (isInmune(jugada))
                        return left("El órgano ya es inmune, no se puede aplicar más medicina");
                    jugada.add(medicina);
                    msg("'%s' aplica '%s' a su '%s'", player.getName(), medicina.getName(), organo.getName());
                    if (isInmune(jugada))
                        msg("¡El jugador '%s' ha inmunizado su '%s'!", player.getName(), organo.getName());
                    return right(false);
                });
    }

    private boolean invariantes() {
        boolean ok = true;
        final int totalCartas = ozam.size() + mazo.size() + players.stream().mapToInt(p -> (int) (p.getMano().size() + p.getJugada().stream().mapToLong(Collection::size).sum())).sum();
        if (totalCartas != 65) {
            System.err.printf("ERROR DE INVARIANZA: el nº de cartas totales es de %d%n", totalCartas);
            ok = false;
        }
        return ok;
    }

    private <T> Either<String, T> isJugando(T x) {
        invariantes();
        return finDeLaPartida ? left("La partida ha terminado") : right(x);
    }
}
