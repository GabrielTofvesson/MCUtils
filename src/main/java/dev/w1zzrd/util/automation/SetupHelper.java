package dev.w1zzrd.util.automation;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.fml.relauncher.Side;
import java.lang.reflect.*;
import java.util.Arrays;

@SuppressWarnings({"unchecked", "unused", "ConstantConditions", "deprecation"})
public final class SetupHelper {

    /**
     * Finds and instantiates all declared, static Item and Block fields in a
     * given class
     * @param clazz Class to inject objects into
     */
    public static void setup(Class<?> clazz)
    {
        setup(clazz, false);
    }

    /**
     * Finds and instantiates all declared, static Item and Block fields in a
     * given class
     * @param clazz Class to inject objects into
     * @param ignoreValues Whether or not to allow overwriting non-null fields
     */
    public static void setup(Class<?> clazz, boolean ignoreValues){
        Arrays
                .stream(clazz.getDeclaredFields())

                // We only want static IForgeRegistryEntries annotated with @Auto
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> IForgeRegistryEntry.Impl.class.isAssignableFrom(field.getType()))
                .filter(field -> field.getAnnotation(Auto.class) != null)

                // Attempt to instantiate and register automatically
                .forEach(field -> {
                    // Prepare reflective nonsense
                    Auto auto = field.getAnnotation(Auto.class);
                    field.setAccessible(true);

                    // Get registry entry field
                    IForgeRegistryEntry.Impl<?> entry = searchAndInject(field, auto.value(), ignoreValues);

                    // If an entry was either found or instantiated
                    if (entry != null) {
                        // Register entry
                        register(entry, auto.name().isEmpty() ? field.getName() : auto.name());

                        // If entry is a block, register ItemBlock
                        if (entry instanceof Block)
                            ForgeRegistries.ITEMS.register(
                                    new ItemBlock((Block) entry)
                                            .setUnlocalizedName(((Block) entry).getUnlocalizedName().substring(5))
                                            .setRegistryName(entry.getRegistryName())
                            );
                    }
                });
    }

    /**
     * Automatically attempt to instantiate an object to populate a given field
     * @param targetField Field to instantiate an object for
     * @param targetType Type to instantiate (or Infer.class)
     * @param ignoreValues Whether or not to allow overwriting of non-null field values
     * @param <T> Type of object
     * @return Object if a field value was found or instantiated. Returns null with stacktrace
     */
    private static <T> T searchAndInject(Field targetField, Class<?> targetType, boolean ignoreValues){
        try {
            // Check if we can inject anything
            if (targetField.get(null) == null || ignoreValues) {

                // Locate a fitting constructor
                Class c = targetType != Infer.class ? targetType : targetField.getType();
                Constructor constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);

                // Construct object and save it
                T t = (T) constructor.newInstance();
                targetField.set(null, t);

                return t;
            }
            else
                // If a field cannot be overwritten, but has a value, just return it
                return (T) targetField.get(null);
        }catch(Throwable t){
            t.printStackTrace();
        }

        // Critical error. Possibly declaring @Auto with no default constructor?
        return null;
    }


    /**
     * Register renders for all @Auto-tagged static fields
     * @param from Class to get fields from
     */
    public static void registerRenders(Class<?> from) {
        // Should only be run on client
        if(FMLCommonHandler.instance().getSide()==Side.SERVER || Minecraft.getMinecraft().getRenderItem()==null)
            return;

        // Check all fields in class
        Arrays
                .stream(from.getDeclaredFields())

                // We only want static IForgeRegistryEntries annotated with @Auto
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> IForgeRegistryEntry.Impl.class.isAssignableFrom(field.getType()))
                .filter(field -> field.getAnnotation(Auto.class) != null)

                // Register renders now
                .forEach( field -> {
                    field.setAccessible(true);

                    try {
                        // Get field value
                        IForgeRegistryEntry entry = (IForgeRegistryEntry) field.get(null);

                        // Get ItemBlock if entry is a block
                        if (entry instanceof Block)
                            entry = Item.getItemFromBlock((Block)entry);

                        // Register item render
                        if (entry instanceof Item)
                            Minecraft
                                    .getMinecraft()
                                    .getRenderItem()
                                    .getItemModelMesher()
                                    .register(
                                            (Item)entry,
                                            0,
                                            new ModelResourceLocation(entry.getRegistryName(), "inventory")
                                    );
                    } catch (IllegalAccessException e) {
                        // This should be literally impossible to hit, so kudos if you can make it to here
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Register a forge registry entry with a given registry
     * @param registryEntry Entry to register
     * @param name Unlocalized name of entry
     * @param <T> Some kind of Forge registry entry
     */
    private static <T extends IForgeRegistryEntry.Impl<?>> void register(T registryEntry, String name){
        // Mod resource location name
        String prefix = Loader.instance().activeModContainer().getModId().toLowerCase();

        // Set registry name if necessary
        if (registryEntry.getRegistryName() == null)
            registryEntry.setRegistryName(new ResourceLocation(prefix, name));

        // Register the entry with the appropriate registry
        if(registryEntry instanceof Item)
        {
            Item item = (Item) registryEntry;
            ForgeRegistries.ITEMS.register(item);

            // Set unlocalized name if necessary
            if (item.getUnlocalizedName().equals("item.null"))
                item.setUnlocalizedName(name);
        }
        else if(registryEntry instanceof Block)
        {
            Block block = (Block) registryEntry;
            ForgeRegistries.BLOCKS.register(block);

            // Set unlocalized name if necessary
            if (block.getUnlocalizedName().equals("tile.null"))
                block.setUnlocalizedName(name);
        }
        else if(registryEntry instanceof EntityEntry)
            ForgeRegistries.ENTITIES.register((EntityEntry) registryEntry);
    }
}
