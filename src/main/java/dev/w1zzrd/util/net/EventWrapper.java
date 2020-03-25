package dev.w1zzrd.util.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.tofvesson.math.ArithmeticKt;

import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public final class EventWrapper implements IMessage {
    private ContextEvent event;

    public EventWrapper(ContextEvent event){
        if (!NetworkEventRegistry.INSTANCE.isRegistered(event.getClass()))
            throw new RuntimeException("Attempt to wrap unregistered network message!");

        this.event = event;
    }

    public EventWrapper(){}


    /**
     * Getter for the event field
     * @return IMessage object or null if message hasn't been passed yet
     */
    public ContextEvent getMessage()
    {
        return event;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        ByteBuffer buffer = buf.nioBuffer(buf.readerIndex(), buf.readableBytes());

        long evID = ArithmeticKt.readVarInt(buffer, 0);
        event = NetworkEventRegistry.INSTANCE.makeEvent((int)evID);

        if (event == null)
            throw new RuntimeException("Attempt to read event with no matching event type!");

        buffer.position(ArithmeticKt.varIntSize(evID));

        event.fromBytes(buffer);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        int eventId = NetworkEventRegistry.INSTANCE.getEventId(event.getClass());
        int size = ArithmeticKt.varIntSize(eventId);

        if (!buf.isWritable(size))
            buf.capacity(buf.capacity() + size - buf.writableBytes());

        ArithmeticKt.writeVarInt(buf.nioBuffer(buf.writerIndex(), buf.writableBytes()), 0, eventId);

        buf.writerIndex(buf.writerIndex() + size);

        event.toBytes(buf);
    }
}