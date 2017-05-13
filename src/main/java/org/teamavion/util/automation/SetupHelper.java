package org.teamavion.util.automation;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import net.minecraftforge.fml.relauncher.Side;
import org.teamavion.util.support.Reflection;
import org.teamavion.util.support.Result;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@SuppressWarnings({"unchecked", "unused", "ConstantConditions", "deprecation"})
public final class SetupHelper {
    public static void setup(Class<?> c, boolean ignoreValues){
        for(Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                try {
                    net.minecraft.block.Block block = searchAndInject(f, BlockRegister.class, net.minecraft.block.Block.class, ignoreValues);
                    net.minecraft.item.Item item;
                    TileEntity tile;
                    if(block!=null){
                        register(block, BlockRegister.class, f, null, true);
                        GameRegistry.register(new ItemBlock(block).setUnlocalizedName(block.getUnlocalizedName().substring(5)), block.getRegistryName());
                    }
                    else if ((item=searchAndInject(f, ItemRegister.class, net.minecraft.item.Item.class, ignoreValues))!=null){
                        register(item, ItemRegister.class, f, null, true);
                    }
                }catch(Throwable fail){ fail.printStackTrace(); }
            }
        }
    }

    private static <T> T searchAndInject(Field f, Class<? extends Annotation> annotation, Class<T> searchType, boolean ignoreValues){
        Auto auto = null;
        Annotation a;
        Class<?> o;
        try {
            if (((a = f.getAnnotation(annotation)) != null ||
                    ((auto = f.getAnnotation(Auto.class)) != null && searchType.isAssignableFrom(f.getType()))) &&
                    (f.get(null) == null || ignoreValues)) {
                Class c = (auto == null && (o=get(a, "value", Class.class).value) != searchType ? o : auto != null && (o=get(auto, "value", Class.class).value) != Infer.class ? o : f.getType());
                Constructor constructor;
                try{
                    constructor = auto!=null || get(a, "material", String.class).value.length()==0?c.getDeclaredConstructor():c.getDeclaredConstructor(Material.class);
                }catch(Exception e){ constructor = c.getDeclaredConstructor(); }
                constructor.setAccessible(true);
                T t = (T) (constructor.getParameterCount()==0?constructor.newInstance():
                        constructor.newInstance(Reflection.getValue(get(a, "material", String.class).value.toUpperCase(), null, Material.class)));
                f.setAccessible(true);
                f.set(null, t);
                return t;
            }
        }catch(Throwable t){ t.printStackTrace(); return null; }
        return null;
    }

    public static void registerRenders(Class<?> from) {
        if(FMLCommonHandler.instance().getSide()==Side.SERVER || Minecraft.getMinecraft().getRenderItem()==null) return; // Should only be run on client
        for(Field f : from.getDeclaredFields()) {
            try{
                Class<?> c;
                boolean block;
                Item i;
                f.setAccessible(true);
                if (Modifier.isStatic(f.getModifiers()) && ((block=(f.getAnnotation(BlockRegister.class) != null)) || f.getAnnotation(ItemRegister.class) != null ||
                        (f.getAnnotation(Auto.class) != null && ((block=Block.class.isAssignableFrom(c=f.get(null).getClass())) || Item.class.isAssignableFrom(c))))){
                    IForgeRegistryEntry entry = (IForgeRegistryEntry) f.get(null);
                    if (entry.getRegistryName() == null)
                        throw new AssertionError("BlockRegister \"" + f.getDeclaringClass() + "#" + f.getName() + "\" has no registry tileName!");
                    try {
                        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(i=block?net.minecraft.item.Item.getItemFromBlock((net.minecraft.block.Block)f.get(null)):(Item)f.get(null),
                                0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }catch(Throwable t){ t.printStackTrace(); }
        }
    }

    private static Result<Object> get(Annotation a, String name){
        return (Result<Object>) Reflection.invokeMethod(Reflection.getMethod(a.getClass(), name), a);
    }

    private static <T> Result<T> get(Annotation a, String name, Class<T> type){ return (Result<T>) get(a, name); }

    private static <T extends IForgeRegistryEntry<?>> void register(T t, Class<? extends Annotation> c, Field f, String name, boolean autoName){
        Auto a = f.getAnnotation(Auto.class);
        Annotation b = f.getAnnotation(c);

        ModContainer mc = Loader.instance().activeModContainer();
        String prefix = mc == null || (mc instanceof InjectedModContainer && ((InjectedModContainer)mc).wrappedContainer instanceof FMLContainer) ? "minecraft" : mc.getModId().toLowerCase();
        if(t.getRegistryName()!=null) GameRegistry.register(t);
        else GameRegistry.register(t,
                (a != null && a.name().length() != 0) || (b!=null && get(b, "name", String.class).value.length() != 0) ?
                        new ResourceLocation(prefix, name=autoName?a != null ? a.name() : get(b, "name", String.class).value:name) : new ResourceLocation(prefix, name=autoName?b==null?a.name():get(b, "name", String.class).value:name));
        if(name==null) name = t.getRegistryName().getResourcePath();
        if(t instanceof net.minecraft.block.Block && ((net.minecraft.block.Block)t).getUnlocalizedName().equals("tile.null")) ((net.minecraft.block.Block)t).setUnlocalizedName(name);
        else if(t instanceof net.minecraft.item.Item && ((net.minecraft.item.Item)t).getUnlocalizedName().equals("item.null")) ((net.minecraft.item.Item)t).setUnlocalizedName(name);
    }

    public static void setup(Class<?> c){ setup(c, false); }
}
