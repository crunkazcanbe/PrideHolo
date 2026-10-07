package net.dries007.holoInventory.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

/** Pride Holo's look and behaviour (config/prideholo-client.json), edited in GuiPrideHolo. Client side only. */
public final class HoloSettings
{
    public String showMode = "always";          // always | sneak | sprint | hold | toggle
    public boolean chests = true, machines = true, tanks = true, entities = true, enderChest = true, jukebox = true;
    public float scale = 1F, itemSize = 1F;
    public int maxColumns = 9, maxItems = 81;
    public String sort = "slots";               // slots | count | name
    public String counts = "short";             // short (1.2k) | exact
    public float spin = 1F;                     // 0 = still
    public boolean bob = true, fadeIn = true;
    public String position = "front";           // front | above
    public int maxDistance = 8;
    public boolean panel = true, strip = true, glow = true, tiles = true;
    public int opacity = 95;
    public String accent = "rainbow";           // rainbow | trans | pink | blue | purple
    public boolean title = true, badge = true;
    public boolean fluids = true, energy = true, progress = true, fuel = true, moreLine = true;

    private static final File FILE = new File("config/prideholo-client.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HoloSettings cur;

    public static HoloSettings get()
    {
        if (cur == null)
        {
            if (FILE.isFile()) try (Reader r = new FileReader(FILE)) { cur = GSON.fromJson(r, HoloSettings.class); } catch (Throwable ignored) { }
            if (cur == null) cur = new HoloSettings();
        }
        return cur;
    }

    public void save()
    {
        try (Writer w = new FileWriter(FILE)) { GSON.toJson(this, w); } catch (Throwable ignored) { }
    }

    public static void reset() { cur = new HoloSettings(); cur.save(); }

    /** the rainbow strip's colours for the chosen accent */
    public int[] accentColors()
    {
        switch (accent)
        {
            case "trans": return new int[]{ 0xFF5BCEFA, 0xFFF5A9B8, 0xFFFFFFFF, 0xFFF5A9B8, 0xFF5BCEFA };
            case "pink": return new int[]{ 0xFFF5A9B8, 0xFFFF7EB9, 0xFFF5A9B8 };
            case "blue": return new int[]{ 0xFF5BCEFA, 0xFF3A7BD5, 0xFF5BCEFA };
            case "purple": return new int[]{ 0xFFB07CFF, 0xFF732982, 0xFFB07CFF };
            default: return new int[]{ 0xFFE40303, 0xFFFF8C00, 0xFFFFED00, 0xFF008026, 0xFF24408E, 0xFF732982, 0xFF5BCEFA, 0xFFF5A9B8 };
        }
    }
}
