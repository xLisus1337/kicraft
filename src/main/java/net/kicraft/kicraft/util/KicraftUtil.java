package net.kicraft.kicraft.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class KicraftUtil {
    // Zmienione na bardziej czytelne skróty: k, M, B, T, q, Q, s, S
    // q = quadrillion (biliard), Q = quintillion (trylion)
    private static final String[] UNITS = new String[]{"", "k", "M", "B", "T", "q", "Q", "s", "S"};

    public static String formatNumber(double value) {
        // Zabezpieczenie przed wartościami nieskończonymi lub błędnymi
        if (Double.isNaN(value) || Double.isInfinite(value)) return "MAX";

        if (value < 1000) {
            return String.valueOf((long)Math.max(0, value));
        }

        // Obliczamy wykładnik
        int exp = (int) (Math.log(value) / Math.log(1000));

        // Jeśli liczba jest większa niż nasza tablica jednostek, dajemy "MAX" lub najwyższą jednostkę
        if (exp >= UNITS.length) {
            return "OVERFLOW";
        }

        DecimalFormat df = new DecimalFormat("###.##", DecimalFormatSymbols.getInstance(Locale.US));
        String formatted = df.format(value / Math.pow(1000, exp));

        return formatted + UNITS[exp];
    }
}