package com.computermind.virusgame;

import com.computermind.sfp.Either;

import java.util.List;

import static com.computermind.sfp.Either.left;
import static com.computermind.sfp.Either.right;
import static java.util.Arrays.asList;

public enum Carta {
    TRATAMIENTO_DESCARTE("¡Descartaros!", "Tira esta carta en USAR y el resto de jugadores ¡se quedarán sin cartas en la mano!"),
    TRATAMIENTO_INFECCION("¡Infección!", "Tira esta carta en USAR y todos los virus de tus órganos pasarán aleatoriamente al resto de jugadores (si es posible)"),
    TRATAMIENTO_TRANSPLANTA_1("¡Cambio de órgano!", "Si tienes esta carta en la mano, COGE UN ÓRGANO tuyo y llévalo al órgano de otro jugador ¡y serán intercambiados!"),
    TRATAMIENTO_TRANSPLANTA_TODO("¡Transplante total!", "Tira esta carta en UN JUGADOR ¡y todos vuestros órganos serán intercambiados!"),
    TRATAMIENTO_ROBAR_ORGANO("¡Te robo un órgano!", "Tira esta carta en un órgano de otro jugador ¡y será tuyo!"),

    MEDICINA_COMODIN("Medicina comodín", "Aplica esta medicina en uno de tus órganos enfermos ¡y lo curarás!"),
    MEDICINA_1("Tiritas", "Aplica esta medicina en tu hueso enfermo ¡y lo curarás!"),
    MEDICINA_2("Vacuna", "Aplica esta medicina en tu corazón ¡y lo curarás!"),
    MEDICINA_3("Píldoras", "Aplica esta medicina en tu cerebro enfermo ¡y lo curarás!"),
    MEDICINA_4("Jarabe", "Aplica esta medicina en tu estómago ¡y lo curarás!"),

    ORGANO_COMODIN("Órgano comodín", "Lleva esta carta a TU JUGADOR y tendrás un órgano más ¡consigue cuatro sanos para ganar! Esta carta hará de cualquier órgano que te falte."),
    ORGANO_1("Hueso", "Lleva esta carta a TU JUGADOR y tendrás un órgano más ¡consigue cuatro sanos para ganar!"),
    ORGANO_2("Corazón", "Lleva esta carta a TU JUGADOR y tendrás un órgano más ¡consigue cuatro sanos para ganar!"),
    ORGANO_3("Cerebro", "Lleva esta carta a TU JUGADOR y tendrás un órgano más ¡consigue cuatro sanos para ganar!"),
    ORGANO_4("Estómago", "Lleva esta carta a TU JUGADOR y tendrás un órgano más ¡consigue cuatro sanos para ganar!"),

    VIRUS_COMODIN("Virus comodín", "Aplica este virus en el órgano de tus oponentes ¡y lo enfermarás!"),
    VIRUS_1("Virus hueso", "Aplica este virus en el hueso de tus oponentes ¡y lo enfermarás!"),
    VIRUS_2("Virus corazón", "Aplica este virus en el corazón de tus oponentes ¡y lo enfermarás!"),
    VIRUS_3("Virus cerebro", "Aplica este virus en el cerebro de tus oponentes ¡y lo enfermarás!"),
    VIRUS_4("Virus estómago", "Aplica este virus en el estómago de tus oponentes ¡y lo enfermarás!"),

    OCULTA("oculta", "Esta carta está oculta y no puedes verla...");

    private final String nombre;
    private final String help;

    Carta(String nombre, String help) {
        this.nombre = nombre;
        this.help = help;
    }

    public static boolean isOrgano(Carta carta) {
        return getOrganos().contains(carta);
    }

    private static boolean isMedicina(Carta carta) {
        return MEDICINA_COMODIN.equals(carta) || MEDICINA_1.equals(carta) || MEDICINA_2.equals(carta) || MEDICINA_3.equals(carta) || MEDICINA_4.equals(carta);
    }

    private static boolean isVirus(Carta carta) {
        return VIRUS_COMODIN.equals(carta) || VIRUS_1.equals(carta) || VIRUS_2.equals(carta) || VIRUS_3.equals(carta) || VIRUS_4.equals(carta);
    }

    public static Either<String, Carta> from(String carta) {
        try {
            return right(Carta.valueOf(carta));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return left(String.format("la cadena '%s' no parece una carta", carta));
        }
    }

    public static List<Carta> getOrganos() {
        return asList(ORGANO_COMODIN, ORGANO_1, ORGANO_2, ORGANO_3, ORGANO_4);
    }

    public String getName() {
        return nombre;
    }

    public boolean isOrgano() {
        return isOrgano(this);
    }

    public boolean isMedicina() {
        return isMedicina(this);
    }

    public boolean isVirus() {
        return isVirus(this);
    }

    public boolean admite(Carta carta) {
        final String[] a = name().split("_");
        final String[] b = carta.name().split("_");
        if (("ORGANO".equals(a[0]) && ("VIRUS".equals(b[0]) || "MEDICINA".equals(b[0]))) // un órgano admite tanto virus como medicina
                || ("VIRUS".equals(a[0]) && "MEDICINA".equals(b[0])) // un virus admite una medicina
                || ("MEDICINA".equals(a[0]) && "VIRUS".equals(b[0]))) // y una medicina un virus
            return "COMODIN".equals(a[1]) || "COMODIN".equals(b[1]) || a[1].equals(b[1]); // o alguno es comodín o ambos del mismo tipo
        return false;
    }

    public boolean isComodin() {
        return MEDICINA_COMODIN.equals(this) || VIRUS_COMODIN.equals(this) || ORGANO_COMODIN.equals(this);
    }

    public String getHelp() {
        return help;
    }
}
