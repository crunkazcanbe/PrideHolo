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

import net.dries007.holoInventory.Helper;
import net.dries007.holoInventory.HoloInventory;
import net.dries007.holoInventory.Probe;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.items.IItemHandler;

import io.netty.buffer.ByteBuf;
import net.dries007.holoInventory.network.response.MerchantRecipes;
import net.dries007.holoInventory.network.response.PlainInventory;
import net.dries007.holoInventory.network.response.ResponseMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IMerchant;
import net.minecraft.inventory.IInventory;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.CapabilityItemHandler;

public class EntityRequest extends RequestMessage
{
    private int id;

    @SuppressWarnings("unused") // netty needs this
    public EntityRequest()
    {
        super();
    }

    public EntityRequest(int dimension, int id)
    {
        super(dimension);
        this.id = id;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        super.fromBytes(buf);
        id = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeInt(id);
    }

    public static class Handler implements IMessageHandler<EntityRequest, ResponseMessage>
    {
        @Override
        public ResponseMessage onMessage(EntityRequest message, MessageContext ctx)
        {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                try
                {
                    ResponseMessage r = answer(message, player);
                    if (r != null) HoloInventory.getSnw().sendTo(r, player);
                }
                catch (Throwable t)
                {
                    HoloInventory.getLogger().warn("Pride Holo: couldn't read entity {}: {}", message.id, t.toString());
                }
            });
            return null;
        }

        private static ResponseMessage answer(EntityRequest message, EntityPlayerMP player)
        {
            if (player.dimension != message.dim) return null;
            Entity entity = player.world.getEntityByID(message.id);
            if (entity == null || entity == player || player.getDistanceSq(entity) > Helper.MAX_DIST_SQ) return null;

            if (entity instanceof IInventory) return new PlainInventory(message.id, (IInventory) entity);
            if (entity instanceof IMerchant) return new MerchantRecipes(message.id, (IMerchant) entity, player);
            IItemHandler h = Probe.cap(entity, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
            if (h == null) return null;
            PlainInventory inv = new PlainInventory(message.id, entity.getName(), h).setInfo(Probe.read(entity));
            return inv.worthSending() ? inv : null;
        }
    }
}
