package dev.w1zzrd.util.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public abstract class ContextEvent {
    public abstract void toBytes (ByteBuf buffer);
    public abstract void fromBytes (ByteBuffer buffer);

    /**
     * Called when an event has been de-serialized
     * @param context Context of the event
     * @return Optional response. Return null for no response
     */
    public abstract @Nullable ContextEvent handle(MessageContext context);
}
