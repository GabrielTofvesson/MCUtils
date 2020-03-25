package dev.w1zzrd.util.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Collection;
import static net.minecraft.util.EnumFacing.*;

@SuppressWarnings("unused")
public final class WorldHelper {

    public static final BlockPos NULL = new BlockPos(0, -1, 0);

    @SuppressWarnings("UnusedReturnValue")
    public static NBTTagCompound writeBlockPosToNBT(NBTTagCompound tag, String tagName, BlockPos pos) {
        NBTTagCompound blockPosTag = new NBTTagCompound();
        blockPosTag.setInteger("X", pos.getX());
        blockPosTag.setInteger("Y", pos.getY());
        blockPosTag.setInteger("Z", pos.getZ());

        tag.setTag(tagName, blockPosTag);

        return tag;
    }

    public static BlockPos readBlockPosFromNBT(NBTTagCompound tag, String tagName) {
        NBTTagCompound blockPosTag = tag.getCompoundTag(tagName);
        int x = blockPosTag.getInteger("X");
        int y = blockPosTag.getInteger("Y");
        int z = blockPosTag.getInteger("Z");
        return new BlockPos(x, y, z);
    }

    public static BlockPos copy(BlockPos pos) {
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static EnumFacing[] getDirectionsFromAxis(Axis axis) {
        return new EnumFacing[]{EnumFacing.getFacingFromAxis(AxisDirection.POSITIVE, axis), EnumFacing.getFacingFromAxis(AxisDirection.NEGATIVE, axis)};
    }

    public static BlockPos rotatePos(BlockPos pos, BlockPos to, Rotate by){
        return new BlockPos(by== Rotate.LEFT?to.getX()+pos.getZ()-to.getZ():to.getX()-pos.getZ()+to.getZ(), pos.getY(), by== Rotate.LEFT?to.getZ()-pos.getX()+to.getX():to.getZ()+pos.getX()-to.getX());
    }

    public static BlockPos[] rotateRow(BlockPos[] pos, BlockPos around, Rotate direction){
        BlockPos[] pos1 = new BlockPos[pos.length];
        for(int i = 0; i<pos.length; ++i) pos1[i] = rotatePos(pos[i], around, direction);
        return pos1;
    }

    public static void dropItem(World w, BlockPos pos, Collection<ItemStack> c){
        //InventoryHelper.dropInventoryItems(world, pos, this);
        for(ItemStack i : c) dropItem(w, pos, i);
    }

    public static void dropItem(World w, BlockPos pos, ItemStack... c){
        //InventoryHelper.dropInventoryItems(world, pos, this);
        for(ItemStack i : c) dropItem(w, pos, i);
    }

    public static void dropItem(World w, BlockPos pos, ItemStack i){
        if(!i.isEmpty()) {
            EntityItem e = new EntityItem(w, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, i.copy());
            e.rotationYaw = 0;
            e.motionX = 0;
            e.motionY = 0;
            e.motionZ = 0;
            w.spawnEntity(e);
            i.setCount(0);
        }
    }

    public static BlockPos getAt(BlockPos relativeTo, EnumFacing direction){
        return new BlockPos(relativeTo.getX()+(direction==SOUTH?-1:direction==NORTH?1:0), relativeTo.getY()+(direction==DOWN?-1:direction==UP?1:0), relativeTo.getZ()+(direction==WEST?-1:direction==EAST?1:0));
    }

    public static BlockPos translate(BlockPos from, EnumFacing... by){
        for(EnumFacing e : by) from = getAt(from, e);
        return from;
    }

    public static ArrayList<BlockPos> matchSurrounding(BlockPos around, IBlockAccess w, Block find, int meta){
        IBlockState b;
        BlockPos bp;
        ArrayList<BlockPos> a = new ArrayList<>();
        for(EnumFacing e : EnumFacing.values()) if((b=w.getBlockState(bp=translate(around, e))).getBlock().equals(find) && (meta<0 || b.getBlock().getMetaFromState(b)==meta)) a.add(bp);
        return a;
    }

    public enum Rotate{ LEFT, RIGHT }
}
