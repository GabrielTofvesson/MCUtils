package dev.w1zzrd.util.net;

import dev.w1zzrd.util.MinecraftHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.tofvesson.data.SyncHandler;
import net.tofvesson.math.ArithmeticKt;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import static net.tofvesson.math.ArithmeticKt.varIntSize;
import static net.tofvesson.math.ArithmeticKt.zigZagEncode;

public abstract class NetworkedTileEntity extends TileEntity implements ITickable {

    private final SyncHandler handler;


    public NetworkedTileEntity(){
        handler = new SyncHandler();
        handler.registerSyncObject(this);
    }

    @Override
    public void update() {
        // Only sync from server to client
        if (shouldSynchronize())
        {
            // Send data
            MinecraftHelper
                    .instance
                    .CHANNEL
                    .sendToDimension(
                            new NetworkedTileEntityEvent(
                                    getPos().getX(),
                                    getPos().getY(),
                                    getPos().getZ(),
                                    world.provider.getDimension(),
                                    handler.serialize().array()
                            ),
                            world.provider.getDimension()
                    );
        }
    }

    protected abstract boolean shouldSynchronize();

    /**
     * Called on a TileEntity immediately after a network synchronization
     * @return Optional response message
     */
    protected abstract @Nullable ContextEvent onSync();

    public static final class NetworkedTileEntityEvent extends ContextEvent {

        private int x, y, z, dimId;
        private byte[] data;

        public NetworkedTileEntityEvent(){}
        public NetworkedTileEntityEvent(int x, int y, int z, int dimId, byte[] data)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimId = dimId;
            this.data = data;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            int bytesToWrite = varIntZZSize(x) +
                    varIntZZSize(y) +
                    varIntZZSize(z) +
                    varIntZZSize(dimId) +
                    varIntSize(data.length) +
                    data.length;

            // Increase buffer to fit exactly enough bytes
            if(!buffer.isWritable(bytesToWrite))
                buffer.capacity(buffer.capacity() + (bytesToWrite - buffer.writableBytes()));

            ByteBuffer nioBuffer = ByteBuffer.wrap(buffer.array());

            // Write VarInt-compressed coordinates to the buffer
            writeZZCompressed(x, buffer, nioBuffer);
            writeZZCompressed(y, buffer, nioBuffer);
            writeZZCompressed(z, buffer, nioBuffer);
            writeZZCompressed(dimId, buffer, nioBuffer);

            // Write VarInt-compressed data size
            writeCompressed(data.length, buffer, nioBuffer);

            // Write data to buffer
            buffer.writeBytes(data);
        }

        @Override
        public void fromBytes(ByteBuffer buffer) {
            x = readZZCompressed(buffer);
            y = readZZCompressed(buffer);
            z = readZZCompressed(buffer);
            dimId = readZZCompressed(buffer);

            data = new byte[readCompressed(buffer)];
            buffer.get(data);
        }

        @Nullable
        @Override
        public ContextEvent handle(MessageContext context) {
            if (context.side == Side.CLIENT){
                // Locate the targeted TileEntity
                TileEntity target = DimensionManager.getWorld(dimId).getTileEntity(new BlockPos(x, y, z));

                // Ensure it's a proper target and deserialize
                if (target instanceof NetworkedTileEntity) {
                    ((NetworkedTileEntity) target).handler.deserialize(data);
                    return ((NetworkedTileEntity) target).onSync();
                }
            }

            return null;
        }


        /**
         * Write a VarInt representation of the given value after ZigZag encoding
         * @param value Value to write
         * @param buffer Buffer to write to
         * @param nioBuffer NIO buffer
         */
        private static void writeZZCompressed(int value, ByteBuf buffer, ByteBuffer nioBuffer){
            ArithmeticKt.writeVarInt(nioBuffer, buffer.writerIndex(), ArithmeticKt.zigZagEncode(value));
            buffer.writerIndex(buffer.writerIndex() + varIntZZSize(value));
        }

        private static void writeCompressed(int value, ByteBuf buffer, ByteBuffer nioBuffer){
            ArithmeticKt.writeVarInt(nioBuffer, buffer.writerIndex(), value);
            buffer.writerIndex(buffer.writerIndex() + varIntSize(value));
        }

        private static int readZZCompressed(ByteBuffer nioBuffer){
            long result = ArithmeticKt.zigZagDecode(ArithmeticKt.readVarInt(nioBuffer, nioBuffer.position()));
            nioBuffer.position(nioBuffer.position() + varIntZZSize(result));
            return (int)result;
        }

        private static int readCompressed(ByteBuffer nioBuffer){
            long result = ArithmeticKt.readVarInt(nioBuffer, nioBuffer.position());
            nioBuffer.position(nioBuffer.position() + ArithmeticKt.varIntSize(result));
            return (int)result;
        }

        private static int varIntZZSize(long value){
            return ArithmeticKt.varIntSize(zigZagEncode(value));
        }
    }
}
