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

package net.dries007.holoInventory;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pride Holo: what a block is DOING, read the same way for every mod — Forge fluid tanks, Forge energy (RF/FE), and the
 * progress counters machines save (cookTime/totalCookTime, progress/maxProgress, ...). Server side only, never throws.
 */
public final class Probe
{
    private Probe() {}

    public static final class Info
    {
        public final List<String> fluids = new ArrayList<>();   // fluid registry names
        public final List<int[]> tanks = new ArrayList<>();     // {amount, capacity}
        public long energy = -1, energyMax = -1;
        public int progress = -1;                               // 0..100, -1 = unknown
        public int burn = -1;                                   // fuel left 0..100 (furnaces and friends), -1 = none

        public boolean empty() { return fluids.isEmpty() && energyMax <= 0 && progress < 0 && burn < 0; }
    }

    private static final EnumFacing[] SIDES = { null, EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST };

    /** some mods only answer on a real side, some only on null: try them all */
    public static <T> T cap(ICapabilityProvider p, Capability<T> c)
    {
        if (c == null) return null;
        for (EnumFacing f : SIDES)
        {
            try { if (p.hasCapability(c, f)) { T t = p.getCapability(c, f); if (t != null) return t; } }
            catch (Throwable ignored) { }
        }
        return null;
    }

    /**
     * Locked or not-yet-opened containers stay secret (requested feature):
     * Treasure2-style hasLocks()/isLocked(), lock flags in the saved data, and loot chests whose loot isn't rolled yet
     * (reading those would also roll the loot early).
     */
    public static boolean secret(TileEntity te)
    {
        for (String m : new String[]{ "hasLocks", "isLocked", "getLocked" })
        {
            try
            {
                java.lang.reflect.Method mm = te.getClass().getMethod(m);
                if ((mm.getReturnType() == boolean.class || mm.getReturnType() == Boolean.class) && Boolean.TRUE.equals(mm.invoke(te))) return true;
            }
            catch (NoSuchMethodException ignored) { }
            catch (Throwable t) { return true; }                         // can't tell: keep it secret
        }
        String cls = te.getClass().getName();
        if (NO_NBT.contains(cls)) return false;
        try
        {
            NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
            if (tag.hasKey("LootTable")) return true;                   // dungeon/structure chest nobody opened yet
            for (String k : new String[]{ "locked", "Locked", "isLocked", "IsLocked" }) if (tag.getBoolean(k)) return true;
            if (tag.hasKey("lockStates") && tag.getTagList("lockStates", 10).tagCount() > 0) return true;
        }
        catch (Throwable t) { NO_NBT.add(cls); }
        return false;
    }

    public static Info read(ICapabilityProvider p)
    {
        Info i = new Info();
        try { fluids(p, i); } catch (Throwable ignored) { }
        try { energy(p, i); } catch (Throwable ignored) { }
        if (p instanceof TileEntity) try { progress((TileEntity) p, i); } catch (Throwable ignored) { }
        return i;
    }

    private static void fluids(ICapabilityProvider p, Info i)
    {
        IFluidHandler h = cap(p, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
        if (h == null) return;
        IFluidTankProperties[] props = h.getTankProperties();
        if (props == null) return;
        for (IFluidTankProperties t : props)
        {
            if (t == null || i.fluids.size() >= 8) continue;
            FluidStack fs = t.getContents();
            int cap = Math.max(0, t.getCapacity());
            if ((fs == null || fs.amount <= 0) && cap <= 0) continue;
            i.fluids.add(fs == null || fs.getFluid() == null ? "" : fs.getFluid().getName());
            i.tanks.add(new int[]{ fs == null ? 0 : fs.amount, cap });
        }
    }

    private static void energy(ICapabilityProvider p, Info i)
    {
        IEnergyStorage e = cap(p, CapabilityEnergy.ENERGY);
        if (e == null || e.getMaxEnergyStored() <= 0) return;
        i.energy = e.getEnergyStored();
        i.energyMax = e.getMaxEnergyStored();
    }

    // "how far along" counters, as machines from many mods name them in their saved data: {now, total}
    private static final String[][] PAIRS = {
            { "cookTime", "totalCookTime" }, { "CookTime", "CookTimeTotal" }, { "progress", "maxProgress" },
            { "Progress", "MaxProgress" }, { "progress", "progressMax" }, { "progress", "processTime" },
            { "processTime", "processTimeMax" }, { "processTime", "maxProcessTime" }, { "craftingProgress", "craftingTime" },
            { "workTime", "maxWorkTime" }, { "work", "maxWork" }, { "timer", "maxTimer" }, { "recipeProgress", "recipeDuration" },
            { "operatingTicks", "ticksRequired" }, { "progress", "total" }, { "progress", "duration" }, { "progress", "maxprogress" },
            { "currentProcessTime", "processTime" }, { "Progress", "Duration" }, { "craftProgress", "craftTime" },
    };
    private static final String[][] BURN = { { "BurnTime", "CurrentItemBurnTime" }, { "burnTime", "currentItemBurnTime" },
            { "burnTime", "maxBurnTime" }, { "fuel", "maxFuel" }, { "burnTime", "totalBurnTime" } };
    // ponytail: name-guessing; a machine that saves its progress under some other name just shows no bar
    private static final Set<String> NO_NBT = new HashSet<>();     // classes whose writeToNBT threw once

    private static void progress(TileEntity te, Info i)
    {
        if (te instanceof TileEntityFurnace)
        {
            TileEntityFurnace f = (TileEntityFurnace) te;
            int burn = f.getField(0), item = f.getField(1), cook = f.getField(2), total = f.getField(3);
            if (total > 0 && cook > 0) i.progress = pct(cook, total);
            if (burn > 0) i.burn = pct(burn, Math.max(1, item));
            return;
        }
        if (te instanceof TileEntityBrewingStand)
        {
            int t = ((TileEntityBrewingStand) te).getField(0);
            if (t > 0) i.progress = pct(400 - t, 400);
            return;
        }
        String cls = te.getClass().getName();
        if (NO_NBT.contains(cls)) return;
        NBTTagCompound tag;
        try { tag = te.writeToNBT(new NBTTagCompound()); }
        catch (Throwable t) { NO_NBT.add(cls); return; }
        int[] p = pair(tag, PAIRS, 0);
        if (p != null && p[1] > 0 && p[0] > 0) i.progress = pct(p[0], p[1]);
        int[] b = pair(tag, BURN, 0);
        if (b != null && b[0] > 0 && b[1] > 0) i.burn = pct(b[0], b[1]);
    }

    /** first {now,total} pair found, also one level down (many mods keep machine data in a sub-tag) */
    private static int[] pair(NBTTagCompound tag, String[][] pairs, int depth)
    {
        for (String[] k : pairs)
        {
            NBTBase a = tag.getTag(k[0]), b = tag.getTag(k[1]);
            if (a instanceof NBTPrimitive && b instanceof NBTPrimitive)
                return new int[]{ (int) ((NBTPrimitive) a).getDouble(), (int) ((NBTPrimitive) b).getDouble() };
        }
        if (depth > 0) return null;
        for (String key : tag.getKeySet())
        {
            NBTBase sub = tag.getTag(key);
            if (sub instanceof NBTTagCompound && !key.toLowerCase(Locale.ROOT).contains("item"))
            {
                int[] r = pair((NBTTagCompound) sub, pairs, depth + 1);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static int pct(int now, int total)
    {
        return Math.max(0, Math.min(100, (int) (100L * now / Math.max(1, total))));
    }
}
