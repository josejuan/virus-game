package com.computermind.virusgame;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.computermind.virusgame.Resp.from;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@CrossOrigin(origins = "*")
@RestController("virusgame")
@RequestMapping("/api")
@Validated
public class Controller {

    @Autowired
    private VirusGameService service;

    private static String clean(String s) {
        if (s == null)
            return "";
        String w = s.trim();
        return w.equals("") ? null : w;
    }

    @RequestMapping(path = "/new", method = POST)
    public Resp<Void> newGame(final String gameId, final String password) {
        return from(service.create(gameId, password));
    }

    @RequestMapping(path = "/join", method = POST)
    public Resp<Void> joinGame(final String gameId, final String player, final String password) {
        return from(service.join(gameId, new Auth(player, password)));
    }

    @RequestMapping(path = "/start", method = POST)
    public Resp<Void> startGame(final String gameId, final String password) {
        return from(service.start(gameId, password));
    }

    @RequestMapping(path = "/status", method = POST)
    public Resp<GameStatus> getStatus(final String gameId, final String player, final String password) {
        return from(service.status(gameId, new Auth(player, password)));
    }

    @RequestMapping(path = "/accion", method = POST)
    public Resp<Void> accion(final String gameId, final String player, final String password,
                             final String srcPlayer, final String srcKind, final String srcTipo,
                             final String dstPlayer, final String dstKind, final String dstTipo) {
        return from(service.accion(gameId, new Auth(player, password), clean(srcPlayer), clean(srcKind), clean(srcTipo), clean(dstPlayer), clean(dstKind), clean(dstTipo)));
    }
}