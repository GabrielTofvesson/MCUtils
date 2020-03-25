package dev.w1zzrd.util.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@SuppressWarnings({"unchecked", "unused"})
public class NetworkChannel {
    public final SimpleNetworkWrapper CHANNEL;
    protected final NetMessage handler = new NetMessage();

    public NetworkChannel(SimpleNetworkWrapper from){ CHANNEL = from; }
    public NetworkChannel(String name){
        this(NetworkRegistry.INSTANCE.newSimpleChannel(name));
        CHANNEL.registerMessage(handler, EventWrapper.class, 1, Side.SERVER);
        CHANNEL.registerMessage(handler, EventWrapper.class, 2, Side.CLIENT);
    }

    public void sendToAll(ContextEvent e){ CHANNEL.sendToAll(new EventWrapper(e)); }
    public void sendTo(ContextEvent e, EntityPlayerMP player){ CHANNEL.sendTo(new EventWrapper(e), player); }
    public void sendToServer(ContextEvent e){ CHANNEL.sendToServer(new EventWrapper(e)); }
    public void sendToAllAround(ContextEvent e, NetworkRegistry.TargetPoint p){ CHANNEL.sendToAllAround(new EventWrapper(e), p); }
    public void sendToDimension(ContextEvent e, int dimID){ CHANNEL.sendToDimension(new EventWrapper(e), dimID); }

    protected static class NetMessage implements IMessageHandler<EventWrapper, IMessage>{

        @Override
        public IMessage onMessage(EventWrapper message, MessageContext ctx)
        {
            // Handle event
            ContextEvent eventResponse = message.getMessage().handle(ctx);

            // Handle response
            return eventResponse == null ? null : new EventWrapper(eventResponse);
        }
    }
}
