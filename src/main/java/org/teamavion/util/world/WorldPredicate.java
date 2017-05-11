package org.teamavion.util.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public abstract class WorldPredicate {
    public abstract boolean apply(IBlockAccess w, BlockPos p, BlockPos source);
}
