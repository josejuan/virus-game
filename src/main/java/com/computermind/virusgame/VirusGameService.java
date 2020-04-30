package com.computermind.virusgame;

import com.computermind.sfp.Either;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

import static com.computermind.sfp.Either.left;
import static com.computermind.sfp.Either.ofNullable;
import static com.computermind.sfp.Either.right;
import static com.computermind.virusgame.Carta.TRATAMIENTO_DESCARTE;
import static com.computermind.virusgame.Carta.TRATAMIENTO_INFECCION;

@Service
public class VirusGameService {

    private Cache<String, VirusGame> games;

    @PostConstruct
    public void initialize() {
        games = CacheBuilder
                .newBuilder()
                .maximumSize(100)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build();
    }

    private Either<String, VirusGame> get(String gameId) {
        return ofNullable(games.getIfPresent(gameId), "¡La sala no existe!");
    }

    // create game
    public Either<String, Void> create(String gameId, String password) {
        return get(gameId).either(e -> {
            games.put(gameId, new VirusGame(password));
            return right(null);
        }, ignore -> left("¡La sala ya existe!"));
    }

    public Either<String, Void> join(String gameId, Auth auth) {
        return get(gameId).bind(g -> g.join(auth));
    }

    public Either<String, GameStatus> status(String gameId, Auth auth) {
        return get(gameId).bind(g -> g.status(auth));
    }

    public Either<String, Void> start(String gameId, String password) {
        return get(gameId).bind(g -> g.start(password));
    }

    public Either<String, Void> accion(String gameId, Auth auth, String srcPlayer, String srcKind, String srcTipo, String dstPlayer, String dstKind, String dstTipo) {
        return get(gameId).bind(g -> {

            if (!auth.getPlayerId().equals(srcPlayer))
                return left("únicamente acciones del mismo jugador están implementadas!");

            if ("carta".equals(srcKind)) {
                if ("accion".equals(dstKind)) {
                    if ("pasar".equals(dstTipo))
                        return Carta.from(srcTipo).bind(c -> g.tirar(auth, c));
                    if ("help".equals(dstTipo))
                        return Carta.from(srcTipo).bind(c -> left(c.getHelp()));
                    if ("usar".equals(dstTipo))
                        return Carta.from(srcTipo).bind(c -> g.usar(auth, c));
                } else if ("player".equals(dstKind)) {
                    return Carta.from(srcTipo).bind(c -> g.aplayer(auth, c, dstPlayer));
                } else if ("carta".equals(dstKind)) {
                    return Carta.from(srcTipo).bind(a -> Carta.from(dstTipo).bind(b -> g.acarta(auth, a, dstPlayer, b)));
                }
            } else if (srcKind == null) {
                // acciones directas (sin source)
                if (dstKind.equals("accion")) {
                    if (dstTipo.equals("pasar"))
                        return g.tirar(auth, null);
                    if (dstTipo.equals("usar"))
                        return left("Tira aquí cartas especiales como '" + TRATAMIENTO_DESCARTE.getName() + "' o '" + TRATAMIENTO_INFECCION.getName() + "'");
                    if (dstTipo.equals("help"))
                        return left("Tira aquí una carta pasa saber algo de ella y cómo usarla.");
                }
            }

            // {asdf asd, carta, ORGANO_1} -> {, accion, help}

            return left(String.format("no implementado {%s, %s, %s} -> {%s, %s, %s}",
                    srcPlayer, srcKind, srcTipo, dstPlayer, dstKind, dstTipo));
        });
    }
}