package dev.w1zzrd.util;

import dev.w1zzrd.util.net.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import net.tofvesson.data.Serializer;
import net.tofvesson.data.SyncHandler;

import java.util.Arrays;

@net.minecraftforge.fml.common.Mod(modid = MinecraftHelper.MODID, name = MinecraftHelper.NAME)
public class MinecraftHelper {

    public static final String MODID = "minecrafthelper";
    public static final String NAME = "Minecraft Utils";

    @Mod.Instance(value = MODID)
    public static MinecraftHelper instance;


    public NetworkChannel CHANNEL = new NetworkChannel(MODID);

    public MinecraftHelper() {
        NetworkEventRegistry.INSTANCE.registerEventType(
                NetworkedTileEntity.NetworkedTileEntityEvent.class,
                NetworkedTileEntity.NetworkedTileEntityEvent::new
        );

        // Make all default serializers server-authoritative
        Arrays.stream(SyncHandler.Companion.getRegisteredSerializers())
                .forEach(ser -> {
                    SyncHandler.Companion.unregisterSerializer(ser);
                    SyncHandler.Companion.registerSerializer(new SidedSerializerWrapper(ser));
                });
    }
}
