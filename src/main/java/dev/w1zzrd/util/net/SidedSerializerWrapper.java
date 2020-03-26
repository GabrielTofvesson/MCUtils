package dev.w1zzrd.util.net;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import net.minecraftforge.fml.relauncher.Side;
import net.tofvesson.annotation.SyncFlag;
import net.tofvesson.data.RBuffer;
import net.tofvesson.data.Serializer;
import net.tofvesson.data.WBuffer;
import net.tofvesson.data.WriteState;

import java.lang.reflect.Field;

public class SidedSerializerWrapper extends Serializer {

    public static final String flagSideClientOwner = "clientSide";
    public static final String flagSideServerOwner = "serverSide";

    public static final SyncFlag clientOwned = SyncFlag.Companion.createFlag(flagSideClientOwner);
    public static final SyncFlag serverOwned = SyncFlag.Companion.createFlag(flagSideServerOwner);


    protected final Serializer wrap;

    public SidedSerializerWrapper(Serializer wrap) {
        super(wrap.getRegisteredTypes());

        this.wrap = wrap;
    }

    protected final boolean isSender(SyncFlag[] flags) {
        SyncFlag side = isServer() ? serverOwned : clientOwned;
        SyncFlag other = side == serverOwned ? clientOwned : serverOwned;

        boolean hasOther = false;

        // Check if sided ownership was declared
        for(SyncFlag flag : flags)
            if (flag == side)
                return true;
            else if (flag == other)
                hasOther = true;

        // If no side has claimed ownership, assume both sides own it
        return !hasOther;
    }

    protected final boolean isRecipient(SyncFlag[] flags) {
        SyncFlag side = isServer() ? serverOwned : clientOwned;
        SyncFlag other = side == serverOwned ? clientOwned : serverOwned;

        boolean hasCurrent = false;
        boolean hasOther = false;

        // Check if sided ownership was declared
        for(SyncFlag flag : flags)
            if (flag == side) {
                if (hasOther) return true;
                else hasCurrent = true;
            }
            else if (flag == other) {
                if (hasCurrent) return true;
                else hasOther = true;
            }

        // If no side has (or both sides have) claimed ownership, assume ownership
        return (hasCurrent == hasOther) || hasOther;
    }



    @Override
    public boolean canSerialize(Object o, SyncFlag[] syncFlags, Class<?> aClass) {
        return isSender(syncFlags) && wrap.canSerialize(o, syncFlags, aClass);
    }

    @Override
    public boolean canDeserialize(Object obj, SyncFlag[] flags, Class<?> type) {
        return isRecipient(flags) && wrap.canDeserialize(obj, flags, type);
    }

    @Override
    public boolean canSerialize(Class<?> type) {
        return wrap.canSerialize(type);
    }

    @Override
    public void computeSizeExplicit(Field field, SyncFlag[] syncFlags, Object o, WriteState writeState, Class<?> aClass) {
        if (!isSender(syncFlags)) return;
        wrap.computeSizeExplicit(field, syncFlags, o, writeState, aClass);
    }

    @Override
    public void deserializeExplicit(Field field, SyncFlag[] syncFlags, Object o, RBuffer rBuffer, Class<?> aClass) {
        if (!isRecipient(syncFlags)) return;
        wrap.deserializeExplicit(field, syncFlags, o, rBuffer, aClass);
    }

    @Override
    public void serializeExplicit(Field field, SyncFlag[] syncFlags, Object o, WBuffer wBuffer, Class<?> aClass) {
        if (!isSender(syncFlags)) return;
        wrap.serializeExplicit(field, syncFlags, o, wBuffer, aClass);
    }


    /**
     * Checks if the current code is part of a Minecraft server
     * @return True if code is being run in a server thread, else false
     */
    public static boolean isServer() {
        return Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER;
    }
}
