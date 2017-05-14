package org.teamavion.util.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.teamavion.util.support.NetworkChannel.Event.EventType.RAW;
import static org.teamavion.util.support.NetworkChannel.Event.EventType.WORLD;

@SuppressWarnings("unchecked")
public class NetworkChannel {
    public final SimpleNetworkWrapper CHANNEL;
    protected final NetMessage n = new NetMessage();

    public NetworkChannel(SimpleNetworkWrapper from){ CHANNEL = from; }
    public NetworkChannel(String name){
        this(NetworkRegistry.INSTANCE.newSimpleChannel(name));
        CHANNEL.registerMessage(n, EventWrapper.class, 1, Side.SERVER);
        CHANNEL.registerMessage(n, EventWrapper.class, 2, Side.CLIENT);
    }

    public void registerWorldHandler(NetworkMessageHandler<WorldEvent> eventHandler){ n.worldHandler = eventHandler; }
    public void registerGeneralHandler(NetworkMessageHandler<Event> eventHandler){ n.rawHandler = eventHandler; }

    public void sendToAll(Event e){ CHANNEL.sendToAll(new EventWrapper(e)); }
    public void sendTo(Event e, EntityPlayerMP player){ CHANNEL.sendTo(new EventWrapper(e), player); }
    public void sendToServer(Event e){ CHANNEL.sendToServer(new EventWrapper(e)); }
    public void sendToAllAround(Event e, NetworkRegistry.TargetPoint p){ CHANNEL.sendToAllAround(new EventWrapper(e), p); }
    public void sendToDimension(Event e, int dimID){ CHANNEL.sendToDimension(new EventWrapper(e), dimID); }

    public void sendToAll(String e){ CHANNEL.sendToAll(new EventWrapper(new RawEvent(e))); }
    public void sendTo(String e, EntityPlayerMP player){ CHANNEL.sendTo(new EventWrapper(new RawEvent(e)), player); }
    public void sendToServer(String e){ CHANNEL.sendToServer(new EventWrapper(new RawEvent(e))); }
    public void sendToAllAround(String e, NetworkRegistry.TargetPoint p){ CHANNEL.sendToAllAround(new EventWrapper(new RawEvent(e)), p); }
    public void sendToDimension(String e, int dimID){ CHANNEL.sendToDimension(new EventWrapper(new RawEvent(e)), dimID); }

    public static class NetMessage implements IMessageHandler<EventWrapper, Event>{

        protected NetworkMessageHandler<WorldEvent> worldHandler;
        protected NetworkMessageHandler<Event> rawHandler;

        @Override
        public Event onMessage(EventWrapper message, MessageContext ctx) {
            if(message.event instanceof WorldEvent && worldHandler!=null) return worldHandler.onRecieveMessage(ctx.side, (WorldEvent) message.event);
            else return rawHandler==null?null:rawHandler.onRecieveMessage(ctx.side, message.event);
        }
    }

    public interface NetworkMessageHandler<T extends Event> {
        /**
         * Handle an incoming message from <b>side</b> with the given data format.
         * @param from Which side the data came from. (Warning: Not the current side where the data is being processed!)
         * @param data Data to process.
         * @return Message to return to sender. Note: Can be null if no data should be sent back.
         */
        Event onRecieveMessage(Side from, T data);
    }

    public static final class EventWrapper implements IMessage{
        Event event;
        public EventWrapper(Event event){ this.event = event; }
        public EventWrapper(){}

        @Override
        public void fromBytes(ByteBuf buf) {
            event = WorldEvent.isWorldEvent(buf);
            if(event.type!=WORLD) event = new RawEvent(event.data);
        }

        @Override public void toBytes(ByteBuf buf) { event.toBytes(buf); }
    }

    public static abstract class Event implements IMessage{
        protected EventType type = RAW;
        protected String data = "";
        public Event(String data){ this.data = data; }
        public Event(){ }

        public EventType getType(){ return type; }
        public String getData(){ return data; }

        @Override
        public final void fromBytes(ByteBuf buf) {
            byte[] array = buf.array();
            byte[] b = new byte[buf.readableBytes()-1];
            System.arraycopy(array, 1, b, 0, b.length);
            String s = new String(b);
            String type = s.substring(0, s.indexOf(';')).toUpperCase();
            for(EventType t : EventType.values())
                if(type.equals(t.name())) {
                    this.type = t;
                    break;
                }
            if(this.type==null) this.type = RAW;
            data = s.substring(s.indexOf(';')+1);
            deserialize();
        }

        @Override
        public final void toBytes(ByteBuf buf) {
            data = serialize();
            byte[] b = ((type==null?"RAW":type.name())+";"+(data==null?"":data)).getBytes();
            buf.writeBytes(b);
        }

        protected abstract String serialize();
        protected abstract void deserialize();

        enum EventType{ RAW, WORLD }
    }

    /**
     * Implementation of {@link Event}. Nothing in this class can't be accessed from the base {@link Event} class.
     */
    public static final class RawEvent extends Event{
        public RawEvent(String data){ super(data); }
        public RawEvent(){ super(); }
        @Override protected String serialize() { return data; }
        @Override protected void deserialize() { }
    }

    /**
     * Event carrying data about the world.
     */
    public static final class WorldEvent extends Event{
        private static final Pattern pattern = Pattern.compile("(\\d+):(\\d+):(\\d+);(\\d+);(.+)");
        private BlockPos pos = new BlockPos(0, 0, 0);
        private int id = 0;
        private JsonObject event = new JsonObject();
        public WorldEvent(BlockPos pos, int id, JsonObject event){ this.pos = pos; this.event = event; this.id = id; type = WORLD; }
        public WorldEvent(){ type = WORLD; }

        public BlockPos getPos(){ return pos; }
        public JsonObject getEvent(){ return event; }

        @Override
        protected String serialize() { return String.valueOf(pos.getX()) + ':' + pos.getY() + ':' + pos.getZ() + ';' + id + ";" + event.toString(); }

        @Override
        protected void deserialize() {
            Matcher m = pattern.matcher(data);
            if(m.matches()){
                pos = new BlockPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                id = Integer.parseInt(m.group(4));
                try{ event = (JsonObject) new JsonParser().parse(m.group(5)); }catch(JsonParseException fail){ fail.printStackTrace(); }
            }
        }

        public static WorldEvent isWorldEvent(ByteBuf b){ WorldEvent w = new WorldEvent(); w.fromBytes(b); return w; }
    }
}
