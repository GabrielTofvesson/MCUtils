package org.teamavion.util.smart;

import org.teamavion.util.support.Reflection;

import java.lang.reflect.Field;

/**
 * Convenience class for referring to an object that might not exist yet by referring to a value stored in a field.
 * This acts as a sort of tracker for that field.
 * @param <T> Type of the objects that should be returned when calling {@link ObjectReference#get()} on an instance of this class.
 */
public class FieldReference<T> implements ObjectReference<T> {
    protected final String fieldName;
    protected final String clazzName;
    protected final Object invokeOn;
    protected Class resolved;
    protected Field resolvedField;

    public FieldReference(String fieldName, String clazzName, Object invokeOn){ this.fieldName = fieldName; this.clazzName = clazzName; this.invokeOn = invokeOn; }
    public FieldReference(Field field, Object invokeOn){
        resolvedField = field;
        try{ resolvedField.setAccessible(true); }catch(Exception ignored){}
        resolved = resolvedField.getDeclaringClass();
        fieldName = null;
        clazzName = null;
        this.invokeOn = invokeOn;
    }
    public FieldReference(String fieldName, Class clazz, Object invokeOn){ this.fieldName = fieldName; clazzName = null; resolved = clazz; this.invokeOn = invokeOn; }

    @Override
    public T get() {
        if(resolved == null) try{ resolved = Class.forName(clazzName); }catch(Throwable e){ e.printStackTrace(); }
        if(resolvedField == null) try{ resolvedField = resolved.getDeclaredField(fieldName); resolvedField.setAccessible(true); }catch(Throwable e){ e.printStackTrace(); }
        try{ //noinspection unchecked
            return (T) resolvedField.get(null); }catch(Throwable e){ e.printStackTrace(); }
        return null;
    }

    /**
     * Create a reference to a static field in the caller class.
     * @param fieldName Name of field to refer to.
     * @param <T> Type of the object stored in the field.
     * @return Reference to the field.
     */
    public static <T> FieldReference<T> staticFromHere(String fieldName){ return fromHere(fieldName, Reflection.getCallerClass(), null); }

    /**
     * Create a reference to a instance-field in the caller class.
     * @param fieldName Name of field to refer to.
     * @param invokeOn Instance to get the value stored in the field from.
     * @param <T> Type of the object stored in the field.
     * @return Reference to the field.
     */
    public static <T> FieldReference<T> fromHere(String fieldName, Object invokeOn){ return fromHere(fieldName, Reflection.getCallerClass(), invokeOn); }

    protected static <T> FieldReference<T> fromHere(String fieldName, Class c, Object invokeOn){
        try{ return new FieldReference<T>(c.getDeclaredField(fieldName), invokeOn); }catch(Exception ignored){}
        return new FieldReference<T>(fieldName, c, invokeOn); // Probably wrong... This essentially means there is a class but not field
    }

}