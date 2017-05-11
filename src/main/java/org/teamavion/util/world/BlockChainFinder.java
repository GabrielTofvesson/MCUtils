package org.teamavion.util.world;

import com.google.common.base.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.teamavion.util.smart.ImmutableReference;
import org.teamavion.util.smart.ObjectReference;
import org.teamavion.util.support.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.minecraft.util.EnumFacing.*;
import static org.teamavion.util.world.BlockChainFinder.SearchMode.*;

/**
 * Finds a multi-block structure of a dynamic size based on allowed/disallowed block types.
 */
@SuppressWarnings("unused")
public class BlockChainFinder {

    protected final HashMap<Integer, Pair<ObjectReference<? extends Block>, Optional<Integer>>> search = new HashMap<>();
    protected SearchMode defaultSearchMode = CARDINAL;
    protected boolean strict = false;
    public final HashMap<ObjectReference<? extends Block>, WorldPredicate> customBlockHandler = new HashMap<>();
    protected int maxSize = -1;

    public boolean isAllowed(Block b, int meta){
        for(Pair<ObjectReference<? extends Block>, Optional<Integer>> p : search.values()) if(b.equals(p.getKey().get()) && (!p.getValue().isPresent() || p.getValue().get()==meta)) return true;
        return false;
    }
    public boolean isAllowed(Block b){ for(Pair<ObjectReference<? extends Block>, Optional<Integer>> p : search.values()) if(b.equals(p.getKey().get())) return true; return false; }
    public boolean isAllowed(ObjectReference<Block> b, int meta){
        for(Pair<ObjectReference<? extends Block>, Optional<Integer>> p : search.values()) if(p.getKey().equals(b) && (!p.getValue().isPresent() || p.getValue().get()==meta)) return true;
        return false;
    }
    public boolean isAllowed(ObjectReference<Block> b){ for(Pair<ObjectReference<? extends Block>, Optional<Integer>> p : search.values()) if(p.getKey().equals(b)) return true; return false; }
    public boolean isAllowed(IBlockState b){ return isAllowed(b.getBlock(), b.getBlock().getMetaFromState(b)); }
    public BlockChainFinder setMaxSize(int maxSize){ this.maxSize = maxSize; return this; }
    public boolean hasSpecificMeta(Block b){ for(Pair<ObjectReference<? extends Block>, Optional<Integer>> p : search.values()) if(p.getKey().equals(b) && p.getValue().isPresent()) return true; return false; }
    public <T extends Block> BlockChainFinder registerCustomBlockHandler(T b, WorldPredicate p){ return registerCustomBlockHandler(new ImmutableReference<>(b), p); }
    public <T extends Block> BlockChainFinder registerCustomBlockHandler(ObjectReference<T> b, WorldPredicate p){ customBlockHandler.put(b, p); return this; }
    protected boolean isAllowedStrict(IBlockState b, ArrayList<Pair<Block, Integer>> a){
        Block b1 = b.getBlock();
        int i = b1.getMetaFromState(b);
        boolean foundBlock = false;
        for(Pair<Block, Integer> p : a)
            if(p.getKey().equals(b1)){
                if(p.getValue()==i) return true;
                foundBlock = true;
            }
        return !foundBlock;
    }

    protected boolean isRegistered(Block b, ArrayList<Pair<Block, Integer>> a){
        for(Pair<Block, Integer> p : a) if(p.getKey().equals(b)) return true;
        return false;
    }

    public BlockChainFinder setStrict(boolean strict){ this.strict = strict; return this; }
    public BlockChainFinder add(int id, Block b, int meta){
        if(!isAllowed(b, meta)) search.put(id, new Pair<ObjectReference<? extends Block>, Optional<Integer>>(new ImmutableReference<>(b), Optional.of(meta)));
        return this;
    }

    public BlockChainFinder add(int id, Block b){
        if(!isAllowed(b)) search.put(id, new Pair<ObjectReference<? extends Block>, Optional<Integer>>(new ImmutableReference<>(b), Optional.<Integer>absent()));
        clearWithMeta(b);
        return this;
    }

    public BlockChainFinder add(int id, ObjectReference<Block> typeRef){
        if(!isAllowed(typeRef)) search.put(id, new Pair<ObjectReference<? extends Block>, Optional<Integer>>(typeRef, Optional.<Integer>absent()));
        return this;
    }

    public BlockChainFinder add(int id, ObjectReference<Block> typeRef, int meta){
        if(!isAllowed(typeRef)) search.put(id, new Pair<ObjectReference<? extends Block>, Optional<Integer>>(typeRef, Optional.of(meta)));
        return this;
    }

    public BlockChainFinder remove(int id){
        search.remove(id);
        return this;
    }

    public Optional<Integer> getIdFor(Block b){
        Pair<ObjectReference<? extends Block>, Optional<Integer>> p;
        for(Integer i : search.keySet()) if(b.equals((p=search.get(i)).getKey().get()) && !p.getValue().isPresent()) return Optional.of(i);
        return Optional.absent();
    }

    public Optional<Integer> getIdFor(Block b, int meta){
        Pair<ObjectReference<? extends Block>, Optional<Integer>> p;
        for(Integer i : search.keySet()) if(b.equals((p=search.get(i)).getKey().get()) && p.getValue().isPresent() && p.getValue().get()==meta) return Optional.of(i);
        return Optional.absent();
    }

    protected void clearWithMeta(Block b){
        Pair<ObjectReference<? extends Block>, Optional<Integer>> p;
        for(Integer i : search.keySet()) if(b.equals((p=search.get(i)).getKey().get()) && p.getValue().isPresent()) search.remove(i);
    }

    protected void purge(){ for(Pair<ObjectReference<? extends Block>, Optional<Integer>> p : search.values()) if(!p.getValue().isPresent()) clearWithMeta(p.getKey().get()); }

    public BlockChainFinder setDefaultSearchMode(SearchMode mode){ defaultSearchMode = mode; return this; }

    public List<BlockPos> find(IBlockAccess w, BlockPos start){ return find(w, start, defaultSearchMode); }
    public List<BlockPos> find(IBlockAccess w, BlockPos start, SearchMode mode){
        purge();
        ArrayList<BlockPos> a = new ArrayList<>();
        a.add(start);
        findAt(w, start, a, new ArrayList<Pair<Block, Integer>>(), mode, start);
        return a;
    }

    protected void findAt(IBlockAccess w, BlockPos at, List<BlockPos> exclude, ArrayList<Pair<Block, Integer>> mStrict, SearchMode mode, BlockPos source){
        BlockPos[] temp = new BlockPos[mode==CARDINAL?6:mode==CARDINAL_DIAGONAL?14:mode==CROSS?8:26];
        ArrayList<BlockPos> nPos = new ArrayList<>();
        int ctr = -1;
        if(mode!=CROSS){
            for(int i = 0; i< values().length; ++i) temp[++ctr] = WorldHelper.getAt(at, values()[i]);
            if(mode==CARDINAL_DIAGONAL || mode==CUBIC){
                for(int i = 0; (i/2)<EnumFacing.HORIZONTALS.length; i+=2){
                    temp[++ctr] = WorldHelper.translate(at, HORIZONTALS[i], UP);
                    temp[++ctr] = WorldHelper.translate(at, HORIZONTALS[i], DOWN);
                }
            }
        }
        if(mode==CROSS || mode==CUBIC){
            temp[++ctr] = WorldHelper.translate(at, UP, EAST, NORTH);
            temp[++ctr] = WorldHelper.translate(at, UP, EAST, SOUTH);
            temp[++ctr] = WorldHelper.translate(at, UP, WEST, NORTH);
            temp[++ctr] = WorldHelper.translate(at, UP, WEST, SOUTH);
            temp[++ctr] = WorldHelper.translate(at, DOWN, EAST, NORTH);
            temp[++ctr] = WorldHelper.translate(at, DOWN, EAST, SOUTH);
            temp[++ctr] = WorldHelper.translate(at, DOWN, WEST, NORTH);
            temp[++ctr] = WorldHelper.translate(at, DOWN, WEST, SOUTH);
        }
        IBlockState b1;
        boolean b2;
        WorldPredicate p;
        for(BlockPos b : temp)
            if(((p=getCustomBlockHandler((b1=w.getBlockState(b)).getBlock()))==null || (p instanceof MBPredicate?((MBPredicate)p).apply(w, b, exclude, mStrict, this, source):p.apply(w, b, source))) &&
                    defaultApply(exclude, mStrict, b, w, b1)) { // Handling system
                if(exclude.size()+1>maxSize && maxSize>1) return;
                exclude.add(b);
                nPos.add(b);
                if(strict && !isRegistered(b1.getBlock(), mStrict) && hasSpecificMeta(b1.getBlock())) mStrict.add(new Pair<>(b1.getBlock(), b1.getBlock().getMetaFromState(b1)));
            }
        for(BlockPos b : nPos) findAt(w, b, exclude, mStrict, mode, source); // Recursively find more until all blocks have been found
    }

    boolean defaultApply(List<BlockPos> exclude, ArrayList<Pair<Block, Integer>> mStrict, BlockPos b, IBlockAccess w, IBlockState b1){
        return (!exclude.contains(b) && isAllowed(w.getBlockState(b)) && (!strict || (!hasSpecificMeta(b1.getBlock()) && isAllowedStrict(b1, mStrict))));
    }

    public WorldPredicate getCustomBlockHandler(Block b){
        for(ObjectReference<? extends Block> o : customBlockHandler.keySet()) if(b.equals(o.get())) return customBlockHandler.get(o);
        return null;
    }

    protected ArrayList<Pair<ObjectReference<? extends Block>, Optional<Integer>>> findByBlock(Block b){
        ArrayList<Pair<ObjectReference<? extends Block>, Optional<Integer>>> a;
        a = new ArrayList<>();
        for(Pair<ObjectReference<? extends Block>, Optional<Integer>> p : search.values()) if(p.getKey().get().equals(b)) a.add(p);
        return a;
    }

    public void isBlockViable(){}

    public enum SearchMode{ CARDINAL, CARDINAL_DIAGONAL, CROSS, CUBIC }

    public static abstract class MBPredicate extends WorldPredicate {

        private IBlockAccess w;
        private BlockPos p;
        private List<BlockPos> exclude;
        private ArrayList<Pair<Block, Integer>> mStrict;
        private BlockChainFinder inst;

        public boolean isRegularlyViable(){ return inst.defaultApply(exclude, mStrict, p, w, w.getBlockState(p)); }

        final boolean apply(IBlockAccess w, BlockPos p, List<BlockPos> exclude, ArrayList<Pair<Block, Integer>> mStrict, BlockChainFinder inst, BlockPos source){
            this.w = w;
            this.p = p;
            this.exclude = exclude;
            this.mStrict = mStrict;
            this.inst = inst;
            return apply(w, p, source);
        }
    }

}
