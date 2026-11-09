package pl.pearltrail;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia moda, zapisywane w .minecraft/config/pearltrail.json */
public class TrailConfig {

    public enum HighlightColor {
        FIOLETOWY("Fioletowy", 0xB04CFF),
        ROZOWY("Różowy", 0xFF55FF),
        NIEBIESKI("Niebieski", 0x55AAFF),
        ZIELONY("Zielony", 0x55FF55),
        ZLOTY("Złoty", 0xFFAA00),
        CZERWONY("Czerwony", 0xFF5555),
        BIALY("Biały", 0xFFFFFF);

        public final String label;
        public final int rgb;

        HighlightColor(String label, int rgb) {
            this.label = label;
            this.rgb = rgb;
        }
    }

    public boolean enabled = true;
    /** Szansa na cząsteczkę w punkcie smugi na tick (0.05 - 1.0). */
    public float density = 0.6f;
    /** Ile sekund smuga zostaje po uderzeniu perły. */
    public float fadeSeconds = 1.5f;
    /** Liczba cząsteczek w wybuchu przy uderzeniu. */
    public int burstParticles = 50;
    public boolean highlight = true;
    /** Ile sekund blok uderzenia jest podświetlony. */
    public float highlightSeconds = 4.0f;
    public HighlightColor highlightColor = HighlightColor.FIOLETOWY;

    public static TrailConfig INSTANCE = new TrailConfig();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("pearltrail.json");
    }

    public static void load() {
        try {
            if (Files.exists(path())) {
                try (Reader r = Files.newBufferedReader(path())) {
                    TrailConfig loaded = GSON.fromJson(r, TrailConfig.class);
                    if (loaded != null) INSTANCE = loaded;
                }
            }
        } catch (Exception e) {
            INSTANCE = new TrailConfig();
        }
        INSTANCE.clamp();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            try (Writer w = Files.newBufferedWriter(path())) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (Exception ignored) {
        }
    }

    private void clamp() {
        density = MathHelper.clamp(density, 0.05f, 1.0f);
        fadeSeconds = MathHelper.clamp(fadeSeconds, 0f, 10f);
        burstParticles = MathHelper.clamp(burstParticles, 0, 150);
        highlightSeconds = MathHelper.clamp(highlightSeconds, 1f, 15f);
        if (highlightColor == null) highlightColor = HighlightColor.FIOLETOWY;
    }
}
