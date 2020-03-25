package dev.w1zzrd.util.net;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.tofvesson.annotation.SyncFlag;
import net.tofvesson.data.*;

import java.lang.reflect.Field;

public class ClientVarSerializer extends Serializer {
    public ClientVarSerializer() {
        super(new Class[]{ ClientVar.class });
    }

    @Override
    public void computeSizeExplicit(Field field, SyncFlag[] syncFlags, Object o, WriteState writeState, Class<?> aClass) {
        if (ClientVar.class.isAssignableFrom(aClass)){
            try {
                ClientVar<?> var = (ClientVar<?>) field.get(o);
                Serializer innerSerializer;
                if (var != null && (innerSerializer = SyncHandler.Companion.getCompatibleSerializer(var.getType())) != null){
                    writeState.registerBits(1);
                    innerSerializer.computeSizeExplicit(var.getField(), syncFlags, var, writeState, var.getType());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deserializeExplicit(Field field, SyncFlag[] syncFlags, Object o, RBuffer rBuffer, Class<?> aClass) {
        if (ClientVar.class.isAssignableFrom(aClass)){
            try {
                ClientVar<?> var = (ClientVar<?>) field.get(o);
                Serializer innerSerializer;
                if (var != null && (innerSerializer = SyncHandler.Companion.getCompatibleSerializer(var.getType())) != null){
                    var.setChanged(rBuffer.readBit());
                    if (var.isChanged())
                        innerSerializer.deserializeExplicit(var.getField(), syncFlags, var, rBuffer, var.getType());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void serializeExplicit(Field field, SyncFlag[] syncFlags, Object o, WBuffer wBuffer, Class<?> aClass) {
        if (ClientVar.class.isAssignableFrom(aClass)){
            try {
                ClientVar<?> var = (ClientVar<?>) field.get(o);
                Serializer innerSerializer;
                if (var != null && (innerSerializer = SyncHandler.Companion.getCompatibleSerializer(var.getType())) != null){
                    wBuffer.writeBit(var.isChanged());
                    if (var.isChanged())
                        innerSerializer.serializeExplicit(var.getField(), syncFlags, var, wBuffer, var.getType());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean canSerialize(Object o, SyncFlag[] syncFlags, Class<?> aClass) {
        return canSerialize(o, syncFlags, aClass, Side.CLIENT);
    }

    @Override
    public boolean canDeserialize(Object o, SyncFlag[] syncFlags, Class<?> aClass) {
        return canSerialize(o, syncFlags, aClass, Side.SERVER);
    }

    protected boolean canSerialize(Object o, SyncFlag[] syncFlags, Class<?> aClass, Side side) {
        if (FMLCommonHandler.instance().getSide()==Side.SERVER && o != null && ClientVar.class.isAssignableFrom(aClass)) {
            Serializer ser = SyncHandler.Companion.getCompatibleSerializer(((ClientVar<?>)o).getType());
            return ser != null && ser.canSerialize(((ClientVar<?>) o).getValue(), syncFlags, ((ClientVar<?>) o).getType());
        }
        return false;
    }
}
