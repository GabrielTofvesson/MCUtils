package dev.w1zzrd.util.support;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

@SuppressWarnings("unused")
public final class Reflection {
    public static Object getValue(String name, Object on, Class<?> from){
        try{
            Field f = from.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(on);
        }catch(Exception e){ e.printStackTrace(); }
        return null;
    }

    public static void setValue(String name, Object on, Object value, Class<?> from){
        try{
            Field f = from.getDeclaredField(name);
            f.setAccessible(true);
            if(f.getType().isPrimitive()){
                String s;
                Method m = Field.class.getDeclaredMethod("set"+(s=f.getType().getSimpleName()).substring(0, 1)+f.getType().getSimpleName().substring(1, s.length()),
                        Object.class, f.getType());
                m.invoke(on, value); // Unboxing is done automatically
            }
            else f.set(on, value);
        }catch(Exception e){ e.printStackTrace(); }
    }

    public static boolean isNestedClass(Object o){ return getEnclosingReference(o, false)!=null; }
    public static Object getEnclosingReference(Object o){ return getEnclosingReference(o, true); }

    private static Object getEnclosingReference(Object nestedClass, boolean error){
        try{
            Field f = nestedClass.getClass().getDeclaredField("this$0");
            f.setAccessible(true);
            return f.get(nestedClass);
        }catch(Exception e){ if(error) e.printStackTrace(); return null; }
    }

    @SuppressWarnings("ThrowableNotThrown")
    public static Class getCallerClass(boolean ignoreSpecial, int depth){
        StackTraceElement[] trace = new Exception().getStackTrace();
        boolean foundFirst = false, isReflective;
        for(int i = 0; i<trace.length; ++i) {
            if (i < depth || ((trace[i].isNativeMethod() || trace[i].getClassName().startsWith("java.lang.reflect") ||
                    trace[i].getClassName().startsWith("sun.reflect")) && ignoreSpecial)) continue;
            //if(!foundFirst){
            //    foundFirst = true;
            //    continue;
            //}
            try{ return Class.forName(trace[i].getClassName()); }catch(Throwable e){ e.printStackTrace(); return null; }
        }
        return null;
    }

    public static Class getCallerClass(boolean ignoreSpecial){ return getCallerClass(ignoreSpecial, 2); }
    public static Class getCallerClass(){ return getCallerClass(true, 2); }

    public static Result<?> invokeMethod(Method m, Object invokee, Object... parameters){
        try{
            m.setAccessible(true);
            return new Result<>(m.invoke(invokee, parameters), true, null);
        }catch (Throwable t){ return new Result<>(null, false, t); }
    }

    public static Method getMethod(Class<?> c, String name, Class<?>... params){
        try{
            Method m = c.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        }catch(Throwable t){ return null; }
    }

    public static Field[] findMatchingValues(Object to, Class<?> in, Object instance, boolean isPrimitive){
        ArrayList<Field> a = new ArrayList<>();
        Object o;
        for(Field f : in.getDeclaredFields())
            try{
                f.setAccessible(true);
                if((o=f.get(instance))==to || (to!=null && to.equals(o))) a.add(f);
            }catch(Throwable ignored){ }
        return a.toArray(new Field[a.size()]);
    }
}
