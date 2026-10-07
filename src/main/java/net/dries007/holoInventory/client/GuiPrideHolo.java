package net.dries007.holoInventory.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Items;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pride Holo settings in the Pride menu style (requested feature):
 * a live preview of the hologram on the left, every option on the right, saved as you click.
 * Opens from Mods -> Pride Holo -> Config and from Options -> "Pride Holo".
 */
public class GuiPrideHolo extends GuiScreen
{
    private final GuiScreen parent;
    private int scroll;
    private int px, py, pw, ph;                      // panel
    private int oy, listTop, listH;
    private final List<Object[]> hits = new ArrayList<>();
    private static final int[] RAINBOW = { 0xFFE40303, 0xFFFF8C00, 0xFFFFED00, 0xFF008026, 0xFF24408E, 0xFF732982, 0xFF5BCEFA, 0xFFF5A9B8 };
    private static final ItemStack[] SAMPLE = { new ItemStack(Items.DIAMOND, 12), new ItemStack(Blocks.COBBLESTONE, 640), new ItemStack(Items.IRON_INGOT, 37),
            new ItemStack(Items.REDSTONE, 2048), new ItemStack(Items.BREAD, 5), new ItemStack(Blocks.LOG, 64), new ItemStack(Items.GOLD_INGOT, 9),
            new ItemStack(Items.EMERALD, 3), new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Blocks.TORCH, 128), new ItemStack(Items.COAL, 300), new ItemStack(Items.APPLE, 7) };

    public GuiPrideHolo(GuiScreen parent) { this.parent = parent; }

    private HoloSettings s() { return HoloSettings.get(); }
    private void changed() { s().save(); }
    private void hit(int x, int y, int w, int h, Runnable r) { hits.add(new Object[]{ x, y, w, h, r }); }
    private static boolean in(int mx, int my, int x, int y, int w, int h) { return mx >= x && my >= y && mx < x + w && my < y + h; }
    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui()
    {
        pw = Math.min(width - 16, 900);
        ph = Math.min(height - 16, 520);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    @Override
    public void drawScreen(int mx, int my, float pt)
    {
        hits.clear();
        drawDefaultBackground();
        HoloSettings c = s();
        // Pride panel: same look as every Pride menu
        gradient(px, py, px + pw, py + ph, 0xF2140E22, 0xF2080510);
        int sw = pw / RAINBOW.length;
        for (int i = 0; i < RAINBOW.length; i++) Gui.drawRect(px + i * sw, py, i == RAINBOW.length - 1 ? px + pw : px + (i + 1) * sw, py + 3, RAINBOW[i]);
        fontRenderer.drawStringWithShadow("§l✦ Pride Holo", px + 10, py + 11, 0xFFFFFF);
        String sub = "Look at a chest or machine to see what's inside and what it's doing";
        fontRenderer.drawStringWithShadow(sub, px + pw - 10 - fontRenderer.getStringWidth(sub), py + 11, 0xA79FBF);
        Gui.drawRect(px + 6, py + 27, px + pw - 6, py + 28, 0x40FFFFFF);

        int cx = px + 10, cy = py + 34, cw = pw - 20, ch = ph - 34 - 30;
        int prevW = Math.min(360, cw / 2 - 6);
        drawPreview(cx, cy, prevW, ch, c);

        // options list
        int ox = cx + prevW + 10, ow = cw - prevW - 10;
        card(ox, cy, ow, ch, 0xFF5BCEFA);
        listTop = cy + 4;
        listH = ch - 8;
        scissor(ox, listTop, ow, listH);
        oy = listTop - scroll;
        int colW = (ow - 18) / 2, x1 = ox + 6, x2 = ox + 12 + colW;

        int start = oy;
        head("When it shows", x1);
        cycle(mx, my, x1, colW, "Show: " + label(c.showMode, new String[]{ "always", "sneak", "sprint", "hold", "toggle" },
                new String[]{ "Always", "While sneaking", "While sprinting", "Hold the key", "Toggle with the key" }), () -> { c.showMode = next(c.showMode, "always", "sneak", "sprint", "hold", "toggle"); changed(); });
        toggle(mx, my, x1, colW, "Chests and boxes", c.chests, () -> { c.chests = !c.chests; changed(); });
        toggle(mx, my, x1, colW, "Machines (energy / progress)", c.machines, () -> { c.machines = !c.machines; changed(); });
        toggle(mx, my, x1, colW, "Tanks (fluids)", c.tanks, () -> { c.tanks = !c.tanks; changed(); });
        toggle(mx, my, x1, colW, "Villagers, minecarts, mobs", c.entities, () -> { c.entities = !c.entities; changed(); });
        toggle(mx, my, x1, colW, "Ender chest", c.enderChest, () -> { c.enderChest = !c.enderChest; changed(); });
        toggle(mx, my, x1, colW, "Jukebox record", c.jukebox, () -> { c.jukebox = !c.jukebox; changed(); });
        head("Distance  " + c.maxDistance + " blocks", x1);
        stepper(mx, my, x1, colW, () -> { c.maxDistance = Math.max(2, c.maxDistance - 1); changed(); }, () -> { c.maxDistance = Math.min(12, c.maxDistance + 1); changed(); }, c.maxDistance / 12F);
        head("Size & layout", x1);
        head2("Hologram size  " + Math.round(c.scale * 100) + "%", x1);
        stepper(mx, my, x1, colW, () -> { c.scale = Math.max(0.5F, round(c.scale - 0.1F)); changed(); }, () -> { c.scale = Math.min(2F, round(c.scale + 0.1F)); changed(); }, c.scale / 2F);
        head2("Item size  " + Math.round(c.itemSize * 100) + "%", x1);
        stepper(mx, my, x1, colW, () -> { c.itemSize = Math.max(0.6F, round(c.itemSize - 0.1F)); changed(); }, () -> { c.itemSize = Math.min(1.4F, round(c.itemSize + 0.1F)); changed(); }, (c.itemSize - 0.6F) / 0.8F);
        head2("Items per row  " + c.maxColumns, x1);
        stepper(mx, my, x1, colW, () -> { c.maxColumns = Math.max(3, c.maxColumns - 1); changed(); }, () -> { c.maxColumns = Math.min(21, c.maxColumns + 1); changed(); }, c.maxColumns / 21F);
        head2("Most items shown  " + c.maxItems, x1);
        stepper(mx, my, x1, colW, () -> { c.maxItems = Math.max(9, c.maxItems - 9); changed(); }, () -> { c.maxItems = Math.min(162, c.maxItems + 9); changed(); }, c.maxItems / 162F);
        cycle(mx, my, x1, colW, "Order: " + label(c.sort, new String[]{ "slots", "count", "name" }, new String[]{ "As in the chest", "Most first", "A to Z" }), () -> { c.sort = next(c.sort, "slots", "count", "name"); changed(); });
        cycle(mx, my, x1, colW, "Counts: " + ("exact".equals(c.counts) ? "Exact (1234)" : "Short (1.2k)"), () -> { c.counts = "exact".equals(c.counts) ? "short" : "exact"; changed(); });
        int end1 = oy;

        oy = start;
        head("Look", x2);
        toggle(mx, my, x2, colW, "Pride panel behind it", c.panel, () -> { c.panel = !c.panel; changed(); });
        head2("Panel see-through  " + c.opacity + "%", x2);
        stepper(mx, my, x2, colW, () -> { c.opacity = Math.max(30, c.opacity - 5); changed(); }, () -> { c.opacity = Math.min(100, c.opacity + 5); changed(); }, c.opacity / 100F);
        toggle(mx, my, x2, colW, "Colour strip on top", c.strip, () -> { c.strip = !c.strip; changed(); });
        toggle(mx, my, x2, colW, "Soft glowing edge", c.glow, () -> { c.glow = !c.glow; changed(); });
        toggle(mx, my, x2, colW, "Slot tiles under items", c.tiles, () -> { c.tiles = !c.tiles; changed(); });
        cycle(mx, my, x2, colW, "Colours: " + label(c.accent, new String[]{ "rainbow", "trans", "pink", "blue", "purple" }, new String[]{ "Rainbow", "Trans flag", "Pink", "Blue", "Purple" }), () -> { c.accent = next(c.accent, "rainbow", "trans", "pink", "blue", "purple"); changed(); });
        toggle(mx, my, x2, colW, "Name at the top", c.title, () -> { c.title = !c.title; changed(); });
        toggle(mx, my, x2, colW, "Working / Idle badge", c.badge, () -> { c.badge = !c.badge; changed(); });
        head("Motion", x2);
        head2("Item spin  " + (c.spin == 0 ? "off" : Math.round(c.spin * 100) + "%"), x2);
        stepper(mx, my, x2, colW, () -> { c.spin = Math.max(0F, round(c.spin - 0.25F)); changed(); }, () -> { c.spin = Math.min(3F, round(c.spin + 0.25F)); changed(); }, c.spin / 3F);
        toggle(mx, my, x2, colW, "Gently float up and down", c.bob, () -> { c.bob = !c.bob; changed(); });
        cycle(mx, my, x2, colW, "Where: " + ("above".equals(c.position) ? "Above the block" : "In front of the block"), () -> { c.position = "above".equals(c.position) ? "front" : "above"; changed(); });
        head("What it's doing", x2);
        toggle(mx, my, x2, colW, "≈ Fluid tanks", c.fluids, () -> { c.fluids = !c.fluids; changed(); });
        toggle(mx, my, x2, colW, "⚡ Energy", c.energy, () -> { c.energy = !c.energy; changed(); });
        toggle(mx, my, x2, colW, "⚙ Progress", c.progress, () -> { c.progress = !c.progress; changed(); });
        toggle(mx, my, x2, colW, "♨ Fuel", c.fuel, () -> { c.fuel = !c.fuel; changed(); });
        toggle(mx, my, x2, colW, "\"+ N more items\" line", c.moreLine, () -> { c.moreLine = !c.moreLine; changed(); });
        int end2 = oy;
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        int total = Math.max(end1, end2) - start + 6;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - listH)));
        if (total > listH)
        {
            int knob = Math.max(10, listH * listH / total), ky = listTop + (listH - knob) * scroll / Math.max(1, total - listH);
            Gui.drawRect(ox + ow - 4, listTop, ox + ow - 1, listTop + listH, 0x30FFFFFF);
            Gui.drawRect(ox + ow - 4, ky, ox + ow - 1, ky + knob, 0xFFF5A9B8);
        }
        hits.removeIf(h -> (Integer) h[0] >= ox && ((Integer) h[1] < listTop || (Integer) h[1] + (Integer) h[3] > listTop + listH));

        // bottom buttons
        int by = py + ph - 24;
        button(mx, my, px + 10, by, 110, 18, "↺ Reset all", 0xFF7A2A30, () -> HoloSettings.reset());
        button(mx, my, px + pw - 100, by, 90, 18, "✔ Done", 0xFF2A2238, () -> mc.displayGuiScreen(parent));
        fontRenderer.drawStringWithShadow("Every change saves right away.", px + 130, by + 5, 0xA79FBF);
        super.drawScreen(mx, my, pt);
    }

    // ------------------------------------------------------------------ preview: a flat copy of the hologram

    private void drawPreview(int x, int y, int w, int h, HoloSettings c)
    {
        card(x, y, w, h, 0xFFF5A9B8);
        fontRenderer.drawStringWithShadow("§lPreview", x + 6, y + 5, 0xFFF5A9B8);
        gradient(x + 4, y + 16, x + w - 4, y + h - 4, 0xFF6B8CC7, 0xFF3E5A2A);
        int n = Math.min(SAMPLE.length, c.maxItems), cols = Math.min(c.maxColumns, Math.max(1, Math.min(6, n)));
        int rows = (n + cols - 1) / cols;
        float k = Math.min(1.4F, c.scale);
        int cell = (int) (24 * k), headH = c.title || c.badge ? 18 : 6;
        int lines = (c.fluids ? 1 : 0) + (c.energy ? 1 : 0) + (c.progress ? 1 : 0);
        int hw = Math.max(cols * cell + 12, 150), hh = headH + rows * cell + 8 + lines * 11 + 6;
        int hx = x + (w - hw) / 2, hy = y + 16 + (h - 20 - hh) / 2 + (c.bob ? (int) Math.round(Math.sin(System.currentTimeMillis() / 400.0) * 2) : 0);
        int alpha = (int) (0xF2 * c.opacity / 100F);
        if (c.panel)
        {
            if (c.glow) gradient(hx - 2, hy - 2, hx + hw + 2, hy + hh + 2, 0x40F5A9B8, 0x405BCEFA);
            gradient(hx, hy, hx + hw, hy + hh, (alpha << 24) | 0x140E22, (alpha << 24) | 0x080510);
            if (c.strip)
            {
                int[] ac = c.accentColors();
                int sw = hw / ac.length;
                for (int i = 0; i < ac.length; i++) Gui.drawRect(hx + i * sw, hy, i == ac.length - 1 ? hx + hw : hx + (i + 1) * sw, hy + 2, ac[i]);
            }
        }
        if (c.title) fontRenderer.drawStringWithShadow("§l✦ Chest", hx + 6, hy + 6, 0xFFFFFF);
        if (c.badge) { String b = "● Working"; fontRenderer.drawStringWithShadow(b, hx + hw - 6 - fontRenderer.getStringWidth(b), hy + 6, 0x8CE06A); }
        int gx = hx + (hw - cols * cell) / 2, gy = hy + headH;
        for (int i = 0; i < n; i++)
        {
            int tx = gx + (i % cols) * cell, ty = gy + (i / cols) * cell;
            if (c.tiles) Gui.drawRect(tx + 1, ty + 1, tx + cell - 1, ty + cell - 1, 0xE01C1530);
            float is = k * c.itemSize;
            GlStateManager.pushMatrix();
            GlStateManager.translate(tx + cell / 2F - 8 * is, ty + cell / 2F - 8 * is, 0);
            GlStateManager.scale(is, is, 1);
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(SAMPLE[i], 0, 0);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            String cnt = "exact".equals(c.counts) ? String.valueOf(SAMPLE[i].getCount()) : shortCount(SAMPLE[i].getCount());
            GlStateManager.disableDepth();
            fontRenderer.drawStringWithShadow(cnt, tx + cell - 1 - fontRenderer.getStringWidth(cnt), ty + cell - 9, 0xFFFFFF);
            GlStateManager.enableDepth();
        }
        int ly = gy + rows * cell + 6;
        if (c.fluids) ly = bar(hx + 6, ly, hw - 12, 0.62F, 0xFF3A7BD5, "≈ Water  10k / 16k mB");
        if (c.energy) ly = bar(hx + 6, ly, hw - 12, 0.4F, 0xFFE0383E, "⚡ 40k / 100k FE");
        if (c.progress) bar(hx + 6, ly, hw - 12, (System.currentTimeMillis() % 3000L) / 3000F, 0xFF8CE06A, "⚙ Working " + (int) ((System.currentTimeMillis() % 3000L) / 30) + "%");
    }

    private int bar(int x, int y, int w, float f, int col, String label)
    {
        Gui.drawRect(x, y, x + w, y + 9, 0xE0100A1C);
        gradient(x, y, x + (int) (w * f), y + 9, col | 0xFF404040 & 0xFFFFFFFF, col);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 3, y + 1, 0);
        GlStateManager.scale(0.75F, 0.75F, 1);
        fontRenderer.drawStringWithShadow(label, 0, 0, 0xFFFFFF);
        GlStateManager.popMatrix();
        return y + 11;
    }

    private static String shortCount(int n) { return n < 1000 ? String.valueOf(n) : n < 1_000_000 ? String.format("%.1fk", n / 1000F).replace(".0k", "k") : String.format("%.1fM", n / 1e6F); }

    // ------------------------------------------------------------------ widgets

    private void head(String t, int x) { oy += 2; fontRenderer.drawStringWithShadow("§d§l" + t, x, oy + 2, 0xFFFFFF); oy += 13; }
    private void head2(String t, int x) { fontRenderer.drawStringWithShadow("§7" + t, x, oy + 1, 0xFFFFFF); oy += 11; }

    private void toggle(int mx, int my, int x, int w, String label, boolean on, Runnable r)
    {
        if (in(mx, my, x, oy, w, 14)) Gui.drawRect(x - 2, oy - 1, x + w, oy + 14, 0x30FFFFFF);
        Gui.drawRect(x, oy + 3, x + 16, oy + 11, on ? 0xFF8CE06A : 0xFF3D2168);
        Gui.drawRect(on ? x + 9 : x + 1, oy + 4, on ? x + 15 : x + 7, oy + 10, 0xFFFFFFFF);
        fontRenderer.drawStringWithShadow(fontRenderer.trimStringToWidth(label, w - 24), x + 22, oy + 3, on ? 0xFFFFFF : 0xA79FBF);
        hit(x, oy, w, 14, r);
        oy += 15;
    }

    private void cycle(int mx, int my, int x, int w, String label, Runnable r)
    {
        button(mx, my, x, oy, w, 16, "◀ " + fontRenderer.trimStringToWidth(label, w - 30) + " ▶", 0xFF2A2238, r);
        oy += 19;
    }

    private void stepper(int mx, int my, int x, int w, Runnable minus, Runnable plus, float frac)
    {
        button(mx, my, x, oy, 16, 13, "-", 0xFF2A2238, minus);
        button(mx, my, x + w - 16, oy, 16, 13, "+", 0xFF2A2238, plus);
        Gui.drawRect(x + 20, oy + 5, x + w - 20, oy + 8, 0xFF1C1530);
        Gui.drawRect(x + 20, oy + 5, x + 20 + (int) ((w - 40) * Math.max(0F, Math.min(1F, frac))), oy + 8, 0xFFF5A9B8);
        oy += 16;
    }

    private void button(int mx, int my, int x, int y, int w, int h, String label, int color, Runnable r)
    {
        boolean over = in(mx, my, x, y, w, h);
        Gui.drawRect(x, y, x + w, y + h, over ? brighten(color) : color);
        Gui.drawRect(x, y + h - 1, x + w, y + h, 0x60000000);
        if (over) Gui.drawRect(x, y, x + w, y + 1, 0x80FFFFFF);
        fontRenderer.drawStringWithShadow(label, x + (w - fontRenderer.getStringWidth(label)) / 2f, y + (h - 8) / 2f, 0xFFFFFF);
        hit(x, y, w, h, r);
    }

    private static void card(int x, int y, int w, int h, int edge) { Gui.drawRect(x, y, x + w, y + h, 0x70000000); Gui.drawRect(x, y, x + w, y + 1, edge); }

    private static int brighten(int c)
    {
        int r = Math.min(255, ((c >> 16) & 255) + 40), g = Math.min(255, ((c >> 8) & 255) + 40), b = Math.min(255, (c & 255) + 40);
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static float round(float v) { return Math.round(v * 100) / 100F; }

    private static String next(String cur, String... all) { for (int i = 0; i < all.length; i++) if (all[i].equals(cur)) return all[(i + 1) % all.length]; return all[0]; }

    private static String label(String cur, String[] ids, String[] names) { for (int i = 0; i < ids.length; i++) if (ids[i].equals(cur)) return names[i]; return names[0]; }

    private void scissor(int x, int y, int w, int h)
    {
        int f = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, Math.max(0, w * f), Math.max(0, h * f));
    }

    private static void gradient(int x0, int y0, int x1, int y1, int top, int bottom)
    {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        b.pos(x1, y0, 0).color((top >> 16) & 255, (top >> 8) & 255, top & 255, (top >>> 24) & 255).endVertex();
        b.pos(x0, y0, 0).color((top >> 16) & 255, (top >> 8) & 255, top & 255, (top >>> 24) & 255).endVertex();
        b.pos(x0, y1, 0).color((bottom >> 16) & 255, (bottom >> 8) & 255, bottom & 255, (bottom >>> 24) & 255).endVertex();
        b.pos(x1, y1, 0).color((bottom >> 16) & 255, (bottom >> 8) & 255, bottom & 255, (bottom >>> 24) & 255).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException
    {
        if (button != 0) return;
        for (int i = hits.size() - 1; i >= 0; i--)
        {
            Object[] h = hits.get(i);
            if (in(mx, my, (Integer) h[0], (Integer) h[1], (Integer) h[2], (Integer) h[3]))
            {
                mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                ((Runnable) h[4]).run();
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException { if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent); }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scroll -= Integer.signum(d) * 30;
    }
}
