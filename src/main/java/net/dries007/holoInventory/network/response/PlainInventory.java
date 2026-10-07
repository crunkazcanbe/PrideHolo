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

package net.dries007.holoInventory.network.response;

import com.google.common.base.Strings;
import io.netty.buffer.ByteBuf;
import net.dries007.holoInventory.Probe;
import net.dries007.holoInventory.client.renderers.InventoryRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class PlainInventory extends ResponseMessage
{
    /** more different items than this and the rest is summed up as "+N more" (giant storage blocks, AE drives...) */
    private static final int MAX_KINDS = 162;
    /** an item whose extra data is bigger than this travels without it (backpacks/shulkers inside chests) */
    private static final int MAX_TAG_CHARS = 4096;

    private String name;
    private List<ItemStack> stacks;
    private int more;
    private Probe.Info info = new Probe.Info();

    @SuppressWarnings("unused") // netty needs it
    public PlainInventory()
    {

    }

    public PlainInventory(int id, IInventory ii)
    {
        super(id);
        scan(ii);
    }

    public PlainInventory(BlockPos pos, IInventory ii)
    {
        super(pos);
        scan(ii);
    }

    public PlainInventory(int id, String name, IItemHandler ii)
    {
        super(id);
        scan(ii);
        this.name = name;
    }

    public PlainInventory(BlockPos pos, String name, IItemHandler ii)
    {
        super(pos);
        scan(ii);
        this.name = name;
    }

    /** a block with no items at all (a tank, a battery, a generator) */
    public PlainInventory(BlockPos pos, String name)
    {
        super(pos);
        this.name = name;
        stacks = new ArrayList<>();
    }

    public PlainInventory setInfo(Probe.Info info)
    {
        if (info != null) this.info = info;
        return this;
    }

    public boolean worthSending()
    {
        return !stacks.isEmpty() || more > 0 || !info.empty();
    }

    /** same items are merged here already, so a 1000-slot block sends a short list */
    private void add(ItemStack s)
    {
        if (s == null || s.isEmpty()) return;
        for (ItemStack have : stacks)
        {
            if (ItemStack.areItemsEqual(have, s) && ItemStack.areItemStackTagsEqual(have, s))
            {
                have.setCount((int) Math.min(Integer.MAX_VALUE, (long) have.getCount() + s.getCount()));
                return;
            }
        }
        if (stacks.size() >= MAX_KINDS) { more += s.getCount(); return; }
        ItemStack copy = s.copy();
        NBTTagCompound tag = copy.getTagCompound();
        if (tag != null && tag.toString().length() > MAX_TAG_CHARS)
        {
            NBTTagCompound keep = new NBTTagCompound();
            if (tag.hasKey("display")) keep.setTag("display", tag.getTag("display"));
            if (tag.hasKey("ench")) keep.setTag("ench", tag.getTag("ench"));
            copy.setTagCompound(keep.getKeySet().isEmpty() ? null : keep);
        }
        stacks.add(copy);
    }

    private void scan(IItemHandler ii)
    {
        int size = ii.getSlots();
        stacks = new ArrayList<>(Math.min(size, MAX_KINDS));
        for (int i = 0; i < size; i++) add(ii.getStackInSlot(i));
    }

    private void scan(IInventory ii)
    {
        name = ii.getName();
        int size = ii.getSizeInventory();
        stacks = new ArrayList<>(Math.min(size, MAX_KINDS));
        for (int i = 0; i < size; i++) add(ii.getStackInSlot(i));
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        super.fromBytes(buf);
        name = ByteBufUtils.readUTF8String(buf);
        int size = buf.readInt();
        stacks = new ArrayList<>(Math.min(size, MAX_KINDS));
        for (int i = 0; i < size; i++)
        {
            ItemStack s = ByteBufUtils.readItemStack(buf);
            s.setCount(buf.readInt());          // vanilla's writer squeezes the count into one byte
            if (!s.isEmpty()) stacks.add(s);
        }
        more = buf.readInt();
        int tanks = buf.readByte();
        for (int i = 0; i < tanks; i++)
        {
            info.fluids.add(ByteBufUtils.readUTF8String(buf));
            info.tanks.add(new int[]{ buf.readInt(), buf.readInt() });
        }
        info.energy = buf.readLong();
        info.energyMax = buf.readLong();
        info.progress = buf.readByte();
        info.burn = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        super.toBytes(buf);
        ByteBufUtils.writeUTF8String(buf, Strings.nullToEmpty(name));
        buf.writeInt(stacks.size());
        for (ItemStack stack : stacks)
        {
            ByteBufUtils.writeItemStack(buf, stack);
            buf.writeInt(stack.getCount());
        }
        buf.writeInt(more);
        buf.writeByte(info.fluids.size());
        for (int i = 0; i < info.fluids.size(); i++)
        {
            ByteBufUtils.writeUTF8String(buf, info.fluids.get(i));
            buf.writeInt(info.tanks.get(i)[0]);
            buf.writeInt(info.tanks.get(i)[1]);
        }
        buf.writeLong(info.energy);
        buf.writeLong(info.energyMax);
        buf.writeByte(info.progress);
        buf.writeByte(info.burn);
    }

    public PlainInventory setName(String name)
    {
        this.name = name;
        return this;
    }

    public static class Handler implements IMessageHandler<PlainInventory, IMessage>
    {
        @Override
        public IMessage onMessage(PlainInventory message, MessageContext ctx)
        {
            // build the renderer on the game thread (it translates names), never on the network thread
            Minecraft.getMinecraft().addScheduledTask(() ->
                    ResponseMessage.handle(message, new InventoryRenderer(message.name, message.stacks, message.more, message.info)));
            return null;
        }
    }
}
