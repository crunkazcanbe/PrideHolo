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

import net.minecraft.block.BlockJukebox;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import java.util.HashSet;

public class Helper
{
    public static boolean showOnSneak;
    public static boolean showOnSprint;
    public static boolean renderBlockName = true;
    public static boolean renderPanel = true;
    public static boolean renderMerchantName = true;
    public static HashSet<String> banned;
    public static double rotationSpeed = 1.0;

    private Helper()
    {
    }

    /** how far away (squared, blocks) a hologram may be asked for: a bit past reach */
    public static final double MAX_DIST_SQ = 12 * 12;

    public static boolean accept(TileEntity te)
    {
        if (te == null) return false;
        if (te instanceof IInventory || te instanceof BlockJukebox.TileEntityJukebox || te instanceof TileEntityEnderChest) return true;
        return Probe.cap(te, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) != null
                || Probe.cap(te, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) != null
                || Probe.cap(te, CapabilityEnergy.ENERGY) != null;
    }

    public static boolean accept(Entity entity)
    {
        try
        {
            return entity != null && (entity instanceof IInventory || entity instanceof net.minecraft.entity.IMerchant
                    || Probe.cap(entity, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) != null);
        }
        catch (Throwable t) { return false; }
    }

    public enum Type
    {
        TILE, ENTITY
    }
}
