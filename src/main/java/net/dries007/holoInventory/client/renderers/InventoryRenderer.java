/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 - 2017 Dries K. Aka Dries007
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.dries007.holoInventory.client.renderers;

import net.dries007.holoInventory.client.HoloSettings;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import net.dries007.holoInventory.Probe;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import net.dries007.holoInventory.client.ClientEventHandler;
import net.dries007.holoInventory.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class InventoryRenderer implements IRenderer
{
    private final String name;
    private final List<ItemStack> stacks;
    private final int more;
    private int shownMore;
    private final Probe.Info info;

    public InventoryRenderer(final String name, List<ItemStack> input)
    {
        this(name, input, 0, new Probe.Info());
    }

    public InventoryRenderer(final String name, List<ItemStack> input, int more, Probe.Info info)
    {
        this.more = more;
        this.info = info == null ? new Probe.Info() : info;
        // Minecraft & Localization. One giant clusterfuck.
        String tmp = I18n.format(name);
        if (!tmp.equals(name)) this.name = tmp;
        else
        {
            String name2 = name + ".name";
            tmp = I18n.format(name2);
            if (!tmp.equals(name2)) this.name = tmp;
            else this.name = name;
        }

        this.stacks = new ArrayList<>(input.size());
        for (ItemStack stack : input)
        outer: {
            if (stack == null) continue;
            for (ItemStack stack2 : stacks)
            {
                if (!ItemStack.areItemStackTagsEqual(stack, stack2) || !ItemStack.areItemsEqual(stack, stack2)) continue;
                stack2.grow(stack.getCount());
                break outer;
            }
            this.stacks.add(stack);
        }
    }

    @Override
    public void render(WorldClient world, RayTraceResult hit, Vec3d pos)
    {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        RenderItem ri = mc.getRenderItem();

        GlStateManager.translate(pos.x - TileEntityRendererDispatcher.staticPlayerX, pos.y - TileEntityRendererDispatcher.staticPlayerY, pos.z - TileEntityRendererDispatcher.staticPlayerZ);

        GlStateManager.rotate(-rm.playerViewY, 0.0F, 0.5F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 0.5F, 0.0F, 0.0F);
        GlStateManager.translate(0, 0, -0.5);

        double d = pos.distanceTo(new Vec3d(TileEntityRendererDispatcher.staticPlayerX, TileEntityRendererDispatcher.staticPlayerY, TileEntityRendererDispatcher.staticPlayerZ));

        if (d < 1.75) return;
        HoloSettings cfg = HoloSettings.get();
        if (d > cfg.maxDistance + 1.5) return;
        if (cfg.bob) GlStateManager.translate(0, Math.sin(System.currentTimeMillis() / 600.0) * 0.03, 0);
        if ("above".equals(cfg.position)) GlStateManager.translate(0, 0.9, 0.3);
        GlStateManager.scale(d * 0.2 * cfg.scale, d * 0.2 * cfg.scale, d * 0.2 * cfg.scale);
        List<ItemStack> stacks = new ArrayList<>(this.stacks);
        if ("count".equals(cfg.sort)) stacks.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        else if ("name".equals(cfg.sort)) stacks.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
        int hidden = 0;
        if (stacks.size() > cfg.maxItems) { for (int i = cfg.maxItems; i < stacks.size(); i++) hidden += stacks.get(i).getCount(); stacks = stacks.subList(0, cfg.maxItems); }
        shownMore = more + hidden;

        int cols;
        if (stacks.isEmpty()) cols = 0;
        else if (stacks.size() <= 9) cols = stacks.size();
        else if (stacks.size() <= 27) cols = 9;
        else if (stacks.size() <= 54) cols = 11;
        else if (stacks.size() <= 90) cols = 14;
        else if (stacks.size() <= 109) cols = 18;
        else cols = 21;
        cols = Math.min(cols, Math.max(1, cfg.maxColumns));
        int rows = cols == 0 ? 0 : (stacks.size() + cols - 1) / cols;
        List<String[]> lines = infoLines();

        if (rows > 4) GlStateManager.scale(0.8, 0.8, 0.8);

        // ---- Pride look (requested feature): same dark panel, rainbow strip and tiles as the
        //      Pride menus. Drawn in "pixel space": 0.01 world units per pixel, 40 px per item cell, viewer-facing.
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        int gridW = cols * 40, gridTop = rows == 0 ? -20 : -20 * rows - 20, gridBottom = rows == 0 ? -20 : 20 * rows - 20;
        int pw = Math.max(gridW, 200) + 20, half = pw / 2;
        int headH = 30, barsH = lines.isEmpty() ? 0 : lines.size() * 15 + 6;
        int top = gridTop - headH - 6, bottom = gridBottom + 8 + barsH;
        boolean working = info.progress > 0 && info.progress < 100;
        long now = System.currentTimeMillis();

        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.scale(0.01, 0.01, 0.01);
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        int alpha = (int) (0xF2 * Math.max(30, Math.min(100, cfg.opacity)) / 100F);
        if (Helper.renderPanel && cfg.panel)
        {
            if (cfg.glow) gradient(-half - 2, top - 2, half + 2, bottom + 2, 0x30F5A9B8, 0x305BCEFA);       // soft glow edge
            gradient(-half, top, half, bottom, (alpha << 24) | 0x140E22, (alpha << 24) | 0x080510);
            int[] strip = cfg.accentColors();
            int n = strip.length, sw = pw / n;
            if (cfg.strip) for (int i = 0; i < n; i++) {
                float wave = 0.85F + 0.15F * (float) Math.sin(now / 500.0 - i * 0.7);
                quad(-half + i * sw, top, i == n - 1 ? half : -half + (i + 1) * sw, top + 3, shade(strip[i], wave));
            }
            quad(-half + 6, top + headH - 2, half - 6, top + headH - 1, 0x40FFFFFF);            // hairline under the title
            // item slots
            if (cfg.tiles) for (int i = 0; i < stacks.size(); i++) {
                int c = i % cols, r = i / cols, cx = 40 * c - 20 * cols + 20, cy = 40 * r - 20 * rows;
                quad(cx - 18, cy - 18, cx + 18, cy + 18, 0xE01C1530);
                quad(cx - 18, cy - 18, cx + 18, cy - 17, 0x30FFFFFF);
                quad(cx - 18, cy + 17, cx + 18, cy + 18, 0x60000000);
            }
        }
        if (cfg.badge)
        {
            // status badge
            String st = working ? "\u25CF Working" : info.burn > 0 ? "\u25CF Burning" : "\u25CB Idle";
            int sc = working ? 0xFF8CE06A : info.burn > 0 ? 0xFFFF8C00 : 0xFF8A8499;
            float pulse = working ? 0.7F + 0.3F * (float) Math.sin(now / 200.0) : 1F;
            int bw = mc.fontRenderer.getStringWidth(st) + 10;
            quad(half - bw - 6, top + 8, half - 6, top + 22, (((int) (0x60 * pulse)) << 24) | (sc & 0xFFFFFF));
            GlStateManager.enableTexture2D();
            mc.fontRenderer.drawStringWithShadow(st, half - bw - 1, top + 11, sc);
            GlStateManager.disableTexture2D();
        }
        // title
        GlStateManager.enableTexture2D();
        if (Helper.renderBlockName && cfg.title)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-half + 8, top + 9, 0);
            GlStateManager.scale(1.4, 1.4, 1);
            String t = mc.fontRenderer.trimStringToWidth("\u2726 " + name, (int) ((pw - 110) / 1.4));
            mc.fontRenderer.drawStringWithShadow("\u00A7l" + t, 0, 0, 0xFFFFFF);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
        GlStateManager.enableDepth();

        int r = 0;
        int c = 0;
        for (final ItemStack stack : stacks)
        {
            RenderHelper.renderStack(ri, stack, cols, c, rows, r);
            if (++c == cols)
            {
                r++;
                c = 0;
            }
        }
        // Draw stack sizes later, to draw over the items (disableDepth)
        r = 0;
        c = 0;
        GlStateManager.disableDepth();
        for (final ItemStack stack : stacks)
        {
            RenderHelper.renderName(mc.fontRenderer, stack, cols, c, rows, r, ClientEventHandler.TEXT_COLOR);
            if (++c == cols)
            {
                r++;
                c = 0;
            }
        }

        // what it's doing: tanks, energy, progress, fuel — Pride bars under the items
        if (!lines.isEmpty())
        {
            int bw = pw - 24;
            GlStateManager.pushMatrix();
            GlStateManager.rotate(180, 0, 0, 1);
            GlStateManager.scale(0.01, 0.01, 0.01);
            for (int i = 0; i < lines.size(); i++)
            {
                String[] l = lines.get(i);
                int y = gridBottom + 10 + i * 15, color = (int) Long.parseLong(l[2], 16);
                float frac = (float) Math.max(0, Math.min(1, Double.parseDouble(l[1])));
                int fill = (int) (bw * frac);
                GlStateManager.disableTexture2D();
                quad(-bw / 2, y, bw / 2, y + 12, 0xE0100A1C);
                if (fill > 0)
                {
                    gradient(-bw / 2, y, -bw / 2 + fill, y + 12, brighten(color), color);
                    quad(-bw / 2, y, -bw / 2 + fill, y + 1, 0x60FFFFFF);
                }
                quad(-bw / 2, y + 11, bw / 2, y + 12, 0x50000000);
                GlStateManager.enableTexture2D();
                mc.fontRenderer.drawStringWithShadow(l[0], -bw / 2 + 4, y + 2, 0xFFFFFF);
            }
            GlStateManager.popMatrix();
        }
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
    }

    private static final int[] RAINBOW = { 0xFFE40303, 0xFFFF8C00, 0xFFFFED00, 0xFF008026, 0xFF24408E, 0xFF732982, 0xFF5BCEFA, 0xFFF5A9B8 };

    private static int shade(int argb, float k)
    {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * k)), g = Math.min(255, (int) (((argb >> 8) & 0xFF) * k)), b = Math.min(255, (int) ((argb & 0xFF) * k));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int brighten(int c)
    {
        int r = Math.min(255, ((c >> 16) & 255) + 50), g = Math.min(255, ((c >> 8) & 255) + 50), b = Math.min(255, (c & 255) + 50);
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static void quad(int x0, int y0, int x1, int y1, int c) { gradient(x0, y0, x1, y1, c, c); }

    private static void gradient(int x0, int y0, int x1, int y1, int top, int bottom)
    {
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        b.pos(x1, y0, 0).color((top >> 16) & 255, (top >> 8) & 255, top & 255, (top >>> 24) & 255).endVertex();
        b.pos(x0, y0, 0).color((top >> 16) & 255, (top >> 8) & 255, top & 255, (top >>> 24) & 255).endVertex();
        b.pos(x0, y1, 0).color((bottom >> 16) & 255, (bottom >> 8) & 255, bottom & 255, (bottom >>> 24) & 255).endVertex();
        b.pos(x1, y1, 0).color((bottom >> 16) & 255, (bottom >> 8) & 255, bottom & 255, (bottom >>> 24) & 255).endVertex();
        Tessellator.getInstance().draw();
    }

    /** {text, fill 0..1, ARGB hex} for every bar */
    private List<String[]> infoLines()
    {
        List<String[]> out = new ArrayList<>();
        HoloSettings cfg = HoloSettings.get();
        if (cfg.fluids) for (int i = 0; i < info.fluids.size(); i++)
        {
            int[] t = info.tanks.get(i);
            FluidStack fs = info.fluids.get(i).isEmpty() ? null : FluidRegistry.getFluidStack(info.fluids.get(i), Math.max(1, t[0]));
            String fname = fs == null ? I18n.format("prideholo.empty") : fs.getLocalizedName();
            int col = 0xFF3A7BD5;
            if (fs != null) try { col = 0xFF000000 | (fs.getFluid().getColor(fs) & 0xFFFFFF); if ((col & 0xFFFFFF) == 0xFFFFFF) col = fs.getFluid().isGaseous(fs) ? 0xFFB0B0C0 : 0xFF3A7BD5; } catch (Throwable ignored) { }
            out.add(new String[]{ "\u2248 " + fname + "  " + RenderHelper.count(t[0]) + " / " + RenderHelper.count(t[1]) + " mB", String.valueOf(t[1] <= 0 ? 0 : t[0] / (double) t[1]), Integer.toHexString(col) });
        }
        if (cfg.energy && info.energyMax > 0)
            out.add(new String[]{ "⚡ " + RenderHelper.count(info.energy) + " / " + RenderHelper.count(info.energyMax) + " FE", String.valueOf(info.energy / (double) info.energyMax), "FFE0383E" });
        if (cfg.progress && info.progress >= 0)
            out.add(new String[]{ "\u2699 " + I18n.format("prideholo.working", info.progress), String.valueOf(info.progress / 100.0), "FF8CE06A" });
        if (cfg.fuel && info.burn >= 0)
            out.add(new String[]{ "\u2668 " + I18n.format("prideholo.fuel", info.burn), String.valueOf(info.burn / 100.0), "FFFF8C00" });
        if (cfg.moreLine && shownMore > 0)
            out.add(new String[]{ I18n.format("prideholo.more", RenderHelper.count(shownMore)), "0", "00000000" });
        return out;
    }

    @Override
    public boolean shouldRender()
    {
        return !stacks.isEmpty() || more > 0 || !info.empty();
    }
}
