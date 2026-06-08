package edu.gazi.ceng479.bfs.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link ArgParser}, including the Turkish-locale case-folding regression. */
class ArgParserTest {

    private final Locale original = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(original);
    }

    @Test
    void parsesAllModesUnderTurkishLocale() {
        // Regression: Turkish 'i'->'İ' uppercase broke valueOf("UI")/("VERIFY").
        Locale.setDefault(new Locale("tr", "TR"));
        assertEquals(Config.Mode.UI, ArgParser.parse(new String[]{"--mode", "ui", "--port", "9090"}).mode);
        assertEquals(Config.Mode.VERIFY,
                ArgParser.parse(new String[]{"--mode", "verify", "--gen", "n=10,deg=2"}).mode);
        assertEquals(Config.Mode.SEQ,
                ArgParser.parse(new String[]{"--mode", "seq", "--gen", "n=10,deg=2"}).mode);
    }

    @Test
    void parsesGenAndThreadList() {
        Config c = ArgParser.parse(new String[]{"--mode", "bench", "--gen", "n=500,deg=8,seed=7",
                "--threads-list", "1,2,4,8", "--reps", "5"});
        assertEquals(500, c.genN);
        assertEquals(8, c.genDeg);
        assertEquals(7L, c.genSeed);
        assertArrayEquals(new int[]{1, 2, 4, 8}, c.threadList);
        assertEquals(5, c.reps);
    }

    @Test
    void portAndDefaults() {
        Config c = ArgParser.parse(new String[]{"--mode", "ui", "--port", "8000"});
        assertEquals(8000, c.port);
    }

    @Test
    void rejectsUnknownAndModelessGraphRequirement() {
        assertThrows(IllegalArgumentException.class, () -> ArgParser.parse(new String[]{"--bogus"}));
        // seq without a graph source is invalid
        assertThrows(IllegalArgumentException.class, () -> ArgParser.parse(new String[]{"--mode", "seq"}));
    }
}
