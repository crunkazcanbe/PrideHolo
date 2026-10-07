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

package net.dries007.holoInventory.network.request;

import io.netty.buffer.ByteBuf;
import net.dries007.holoInventory.Helper;
import net.dries007.holoInventory.HoloInventory;
import net.dries007.holoInventory.Probe;
import net.minecraft.entity.player.EntityPlayerMP;
import net.dries007.holoInventory.api.INamedItemHandler;
import net.dries007.holoInventory.network.response.PlainInventory;
import net.dries007.holoInventory.network.response.ResponseMessage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockJukebox;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class TileRequest extends RequestMessage
{
    private BlockPos pos;

    @SuppressWarnings("unused") // required for netty
    public TileRequest()
    {
        super();
    }

    public TileRequest(int dimension, BlockPos blockPos)
    {
        super(dimension);
        this.pos = blockPos;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        super.fromBytes(buf);
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    public static class Handler implements IMessageHandler<TileRequest, ResponseMessage>
    {
        @Override
        public ResponseMessage onMessage(TileRequest message, MessageContext ctx)
        {
            // never touch the world from the network thread (the old version did: random crashes in big packs)
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                try
                {
                    ResponseMessage r = answer(message, player);
                    if (r != null) HoloInventory.getSnw().sendTo(r, player);
                }
                catch (Throwable t)
                {
                    HoloInventory.getLogger().warn("Pride Holo: couldn't read the block at {}: {}", message.pos, t.toString());
                }
            });
            return null;
        }

        private static ResponseMessage answer(TileRequest message, EntityPlayerMP player)
        {
            // only what you can actually see: your own dimension, loaded, within reach (no peeking into far-away chests)
            if (player.dimension != message.dim) return null;
            World world = player.world;
            if (!world.isBlockLoaded(message.pos)) return null;
            if (player.getDistanceSqToCenter(message.pos) > Helper.MAX_DIST_SQ) return null;
            TileEntity te = world.getTileEntity(message.pos);
            if (te == null) return null;

            if (Helper.banned.contains(te.getClass().getCanonicalName())) return null;
            if (te instanceof ILockableContainer && !player.canOpen(((ILockableContainer) te).getLockCode())) return null;
            if (!player.isCreative() && Probe.secret(te)) return null;           // locked / unopened loot: no peeking (creative may)

            PlainInventory inv = items(world, te, message.pos, player);
            Probe.Info info = Probe.read(te);
            if (inv == null)
            {
                if (info.empty()) return null;
                inv = new PlainInventory(message.pos, te.getBlockType().getUnlocalizedName());
            }
            inv.setInfo(info);
            return inv.worthSending() ? inv : null;
        }

        private static PlainInventory items(World world, TileEntity te, BlockPos pos, EntityPlayerMP player)
        {
            if (te instanceof TileEntityEnderChest)
            {
                return new PlainInventory(pos, player.getInventoryEnderChest());
            }
            else if (te instanceof BlockJukebox.TileEntityJukebox)
            {
                InventoryBasic ib = new InventoryBasic("minecraft:jukebox", false, 1);
                ib.setInventorySlotContents(0, ((BlockJukebox.TileEntityJukebox) te).getRecord());
                return new PlainInventory(pos, ib).setName(Blocks.JUKEBOX.getUnlocalizedName());
            }
            else if (te instanceof TileEntityChest)
            {
                Block b = world.getBlockState(pos).getBlock();
                if (b instanceof BlockChest)
                {
                    IInventory i = ((BlockChest) b).getLockableContainer(world, pos);
                    return i == null ? null : new PlainInventory(pos, i);
                }
                return new PlainInventory(pos, ((TileEntityChest) te));
            }
            else if (te instanceof IInventory)
            {
                return new PlainInventory(pos, (IInventory) te);
            }
            IItemHandler iih = Probe.cap(te, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
            if (iih == null) return null;
            if (te instanceof INamedItemHandler) return new PlainInventory(pos, ((INamedItemHandler) te).getItemHandlerName(), iih);
            return new PlainInventory(pos, te.getBlockType().getUnlocalizedName(), iih);
        }
    }
}
