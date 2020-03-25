package dev.w1zzrd.util.net;

import net.tofvesson.reflect.AccessibleKt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class ClientVar<T>
{
    private final Class<T> type;
    private T value;
    private boolean changed = false;

    public ClientVar(@Nullable T value, Class<T> type)
    {
        this.value = value;
        this.type = type;
    }

    public ClientVar(@Nonnull T value)
    {
        this.value = value;
        this.type = (Class<T>) value.getClass();
    }

    public void setValue(T value)
    {
        changed |= this.value != value;
        this.value = value;
    }

    public T getValue()
    {
        return this.value;
    }

    public boolean isChanged()
    {
        return changed;
    }

    public void setChanged(boolean changed)
    {
        this.changed = changed;
    }

    public Class<T> getType()
    {
        return type;
    }

    public Field getField()
    {
        try {
            return AccessibleKt.access(ClientVar.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return null;
    }
}
