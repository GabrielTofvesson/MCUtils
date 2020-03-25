package dev.w1zzrd.util.world;

import com.google.common.base.Optional;
import dev.w1zzrd.util.support.Log;
import dev.w1zzrd.util.support.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import dev.w1zzrd.util.smart.ImmutableReference;
import dev.w1zzrd.util.smart.ObjectReference;

import java.util.*;

@SuppressWarnings("all")
public class MultiBlock {
    /**
     * Wildcard search operator
     */
    public static final char WLD = '*';

    protected final Map<Integer, Plane> structure = new HashMap<Integer, Plane>();
    protected final Map<Character, Pair<ObjectReference<Block>, Optional<Integer>>> mappings = new HashMap<Character, Pair<ObjectReference<Block>, Optional<Integer>>>();
    public final HashMap<ObjectReference<? extends Block>, WorldPredicate> customBlockHandler = new HashMap<ObjectReference<? extends Block>, WorldPredicate>();
    protected boolean strict = false;

    /**
     * Add a new search layer. Higher numbers are ordered behind lower numbers (relative to centre).
     * @param relativeOffset Offset behind centre position. Higher number => further back.
     * @param layer The specified search plane to use for this layer
     * @return Reference to this object for chaining.
     */
    public MultiBlock addLayer(int relativeOffset, Plane layer){ structure.put(relativeOffset, layer); return this; }

    /**
     * Remove search layer at the given offset.
     * @param relativeOffset  Offset behind centre position. Higher number => further back.
     * @return Reference to this object for chaining.
     */
    public MultiBlock removeLayer(int relativeOffset){ structure.remove(relativeOffset); return this; }

    /**
     * Get the search layer at the given offset.
     * @param relativeOffset  Offset behind centre position. Higher number => further back.
     * @return Plane defining search for this layer.
     */
    public Plane getLayer(int relativeOffset){ return structure.get(relativeOffset); }

    /**
     * Add a copy of a given search layer at the supplied relative offset. Higher numbers are ordered behind lower numbers (relative to centre).
     * <em>NOTE:</em> This uses the {@link Plane#copy()} method, meaning that manipulating one of the referenced layers will not affect the other.
     * @param relativeOffset Offset behind centre position. Higher number => further back.
     * @param source Layer to copy from.
     * @return Reference to this object for chaining.
     */
    public MultiBlock addLayer(int relativeOffset, int source){ if(structure.containsKey(source)) structure.put(relativeOffset, structure.get(source)); return this; }

    /**
     * Add a given amount of identical layers. Starting at layer <em><b>startOff</b></em> and ending at layer <em><b>startOff</b></em> + <em><b>count</b></em> - 1.
     * @param startOff Starting layer offset.
     * @param count Amount of layers to add
     * @param of Plane to use.
     * @return Reference to this object for chaining.
     */
    public MultiBlock addLayers(int startOff, int count, Plane of){
        for(int i = 0; i<count; ++i) addLayer(i+startOff, of.copy());
        return this;
    }

    /**
     * Add a given amount of identical layers. Starting at layer <em><b>startOff</b></em> and ending at layer <em><b>startOff</b></em> + <em><b>count</b></em> - 1.
     * @param startOff Starting layer offset.
     * @param count Amount of layers to add
     * @param source Layer to copy plane from.
     * @return Reference to this object for chaining.
     */
    public MultiBlock addLayers(int startOff, int count, int source){ addLayers(startOff, count, structure.get(source)); return this; }

    /**
     * Registers a character-block mapping. This is used to correlate a supplied character in the search planes with a block in game.<br>
     * <em>NOTE:</em> Character defined in {@link MultiBlock#WLD} cannot be mapped to a block.
     * @param blockMapping Character to correlate with a block
     * @param mappedBlock BlockRegister to search when the specified character is found in a plane.
     * @return Reference to this object for chaining.
     */
    public MultiBlock registerMapping(char blockMapping, Block mappedBlock){
        if(blockMapping==WLD) throw new RuntimeException("Can't assign wildcard mapping '"+WLD+"' to "+mappedBlock);
        mappings.put(blockMapping, new Pair<ObjectReference<Block>, Optional<Integer>>(new ImmutableReference<Block>(mappedBlock), Optional.<Integer>absent()));
        return this;
    }

    public <T extends WorldPredicate> MultiBlock registerCustomBlockHandler(Block check, T t){
        customBlockHandler.put(new ImmutableReference<Block>(check), t);
        return this;
    }
    public <T extends WorldPredicate> MultiBlock registerCustomBlockHandler(ObjectReference<Block> check, T t){
        customBlockHandler.put(check, t);
        return this;
    }

    /**
     * Delete all character-block mappings.
     * @return Reference to this object for chaining.
     */
    public MultiBlock clearMappings(){ mappings.clear(); return this; }

    /**
     * Registers a character-block mapping with a specific metadata. This is used to correlate a supplied character in the search planes with a block in game.<br>
     * <em>NOTE:</em> Character defined in {@link MultiBlock#WLD} cannot be mapped to a block.
     * @param blockMapping Character to correlate with a block
     * @param mappedBlock BlockRegister to search when the specified character is found in a plane.
     * @param meta Specific metadata to allow when searching.
     * @return Reference to this object for chaining.
     */
    public MultiBlock registerMapping(char blockMapping, Block mappedBlock, int meta){
        if(blockMapping==WLD) throw new RuntimeException("Can't assign wildcard mapping '"+WLD+"' to "+mappedBlock);
        mappings.put(blockMapping, new Pair<ObjectReference<Block>, Optional<Integer>>(new ImmutableReference<Block>(mappedBlock), Optional.of(meta)));
        return this;
    }

    /**
     * Registers a character-block mapping. This mapping uses {@link ObjectReference}. This means that the supplied block reference doesn't necessarily have to exist at this time.
     * The only requirement is that a proper value exists when {@link MultiBlock#findStructure(World, BlockPos)} (or any overloads) can be invoked.
     * This is used to correlate a supplied character in the search planes with a block in game.<br>
     * <em>NOTE:</em> Character defined in {@link MultiBlock#WLD} cannot be mapped to a block.
     * @param blockMapping Character to correlate with a block
     * @param reference Reference to block to search when the specified character is found in a plane.
     * @return Reference to this object for chaining.
     */
    public MultiBlock registerLazyMapping(char blockMapping, ObjectReference<Block> reference){
        if(blockMapping==WLD) throw new RuntimeException("Can't assign wildcard mapping '"+WLD+"' to "+reference);
        mappings.put(blockMapping, new Pair<ObjectReference<Block>, Optional<Integer>>(reference, Optional.<Integer>absent()));
        return this;
    }

    /**
     * Registers a character-block mapping with a specific metadata. This mapping uses {@link ObjectReference}. This means that the supplied block reference doesn't necessarily have to exist at this time.
     * The only requirement is that a proper value exists when {@link MultiBlock#findStructure(World, BlockPos)} (or any overloads) can be invoked.
     * This is used to correlate a supplied character in the search planes with a block in game.<br>
     * <em>NOTE:</em> Character defined in {@link MultiBlock#WLD} cannot be mapped to a block.
     * @param blockMapping Character to correlate with a block
     * @param reference Reference to block to search when the specified character is found in a plane.
     * @param meta Specific metadata to allow when searching.
     * @return Reference to this object for chaining.
     */
    public MultiBlock registerLazyMapping(char blockMapping, ObjectReference<Block> reference, int meta){
        if(blockMapping==WLD) throw new RuntimeException("Can't assign wildcard mapping '"+WLD+"' to "+reference);
        mappings.put(blockMapping, new Pair<ObjectReference<Block>, Optional<Integer>>(reference, Optional.of(meta)));
        return this;
    }

    /**
     * Gets a character-block mapping from a given character if it exists.
     * @param of Character corresponding to mapping.
     * @return Mapped block and metadata if metadata was specified. If no mapping is linked to the given character, return is null.
     */
    public Pair<ObjectReference<Block>, Optional<Integer>> getMapping(char of){ return mappings.containsKey(of)?mappings.get(of):null; }

    /**
     * Search for the given block structure/collection in any/all of the horizontal directions.
     * @param w World to search in.
     * @param centre Where the search is originating from. (In the case of multiblocks, this is usually the controller's position).
     * @return Array of all directions the structure was found.
     */
    public EnumFacing[] findStructure(IBlockAccess w, BlockPos centre){ return findStructure(w, centre, EnumFacing.HORIZONTALS); }

    /**
     * Search for the given block structure/collection in the given orientations.
     * @param w World to search in.
     * @param centre Where the search is originating from. (In the case of multiblocks, this is usually the controller's position).
     * @param search Which directions the structure is allowed to face. (Direction is considered as the direction of an observer standing in layer -1 looking at layer 0)
     * @return Array of all directions where the structure was found.
     */
    public EnumFacing[] findStructure(IBlockAccess w, BlockPos centre, EnumFacing... search){
        List<EnumFacing> l = new ArrayList<EnumFacing>();
        for(EnumFacing f : search)
            if(findStructure(w, centre, f))
                l.add(f);
        return l.toArray(new EnumFacing[l.size()]);
    }

    /**
     * Create an exact (yet completely separate) copy of this object. This means that no amount of manipulations to the planes, layers or block mappings will affect the original object.
     * @return The copy.
     */
    public MultiBlock copy(){
        MultiBlock m = new MultiBlock();
        Pair<ObjectReference<Block>, Optional<Integer>> p;
        for(Integer i : structure.keySet()) m.structure.put(i, structure.get(i).copy());
        for(Character c : mappings.keySet())
            m.mappings.put(c, new Pair<ObjectReference<Block>, Optional<Integer>>(
                    new ImmutableReference<Block>((p=mappings.get(c)).getKey().get()),
                    Optional.fromNullable(p.getValue().isPresent()?p.getValue().get():null))
            );
        m.strict = strict;
        return m;
    }

    protected boolean findStructure(IBlockAccess w, BlockPos centre, EnumFacing dir){ return getMap(w, centre, dir)!=null; }

    /**
     * Gets a flattened array of all positions corresponding to the supplied model with the properly calculated metadata values (only if supplied or in strict mode).
     * @param w World to account for.
     * @param centre Which position should be considered the "controller".
     * @param direction Direction the structure should point towards. (Higher relative offset values will be further away in this direction).
     * @return The flattened array.
     */
    public Pair<BlockPos, Optional<IBlockState>>[] getMap(IBlockAccess w, BlockPos centre, EnumFacing direction){
        if(!isHorizontal(direction)) return null;
        Plane p;
        Pair<ObjectReference<Block>, Optional<Integer>> p1;
        IBlockState b;
        HashMap<Block, Integer> strictMap = null;
        ArrayList<Pair<BlockPos, Optional<IBlockState>>> realMap = new ArrayList<Pair<BlockPos, Optional<IBlockState>>>();
        if(strict) strictMap = new HashMap<Block, Integer>();
        boolean b1 = false;
        boolean state = false;
        ObjectReference<? extends Block> o;
        WorldPredicate w1;
        for(Integer i : structure.keySet())
            for(Pair<BlockPos, Character> pos : (p=structure.get(i)).getPosMap(centre, i, direction))
                if((b1 = !pos.getKey().equals(centre)) /* Ignore controller block */ &&
                        !(((o=getFor(w.getBlockState(pos.getKey()).getBlock()))!=null &&
                                ((w1=customBlockHandler.get(o)) instanceof MBPredicate?((MBPredicate)w1).apply(w, pos.getKey(), pos.getValue(), strictMap, this, centre):w1.apply(w, pos.getKey(), centre))) ||
                                matches(w, pos.getKey(), pos.getValue(), strictMap))){
                    return null;
                }else if(b1 && strict) {
                    if (!strictMap.containsKey((b = w.getBlockState(pos.getKey())))) strictMap.put(b.getBlock(), b.getBlock().getMetaFromState(b)); // Temporary strict-mode metadata mapping
                    else return null;
                    realMap.add(new Pair(pos.getKey(), b));
                }else if(b1)
                    realMap.add(new Pair<BlockPos, Optional<IBlockState>>(pos.getKey(),
                            (p1=mappings.get(pos.getValue())).getValue().isPresent()?Optional.of(w.getBlockState(pos.getKey())):Optional.<IBlockState>absent()));
        return realMap.toArray(new Pair[realMap.size()]);
    }

    protected ObjectReference<? extends Block> getFor(Block b){
        for(ObjectReference<? extends Block> o : customBlockHandler.keySet()) if(b.equals(o.get())) return o;
        return null;
    }

    /**
     * Defines whether or not blocks in the structure (defined under the same character mapping) that doesn't have a
     * specified metadata should enforce matching metadata for that character-block mapping based on metadata inferred from first block of that type found.
     * <em>Example:</em> Searching for a structure with one character mapping - say 'b', the strict mode will ensure that
     * (despite no specific metadata was defined) all blocks attempting to match 'b' must have the same metadata.
     * This is good for generalizing a block structure where there are mutiple versions of the same block that should be supported
     * without allowing for mixing without needing to create multiple MultiBlock objects (one for each).
     * @param strict Whether or not to enforce strict mode.
     * @return Reference to this object for chaining.
     */
    public MultiBlock setStrict(boolean strict){ this.strict = strict; return this; }

    public void debug(){
        Log.println("\n-- MB structure debug --\n-- Strict: "+strict+" --\n-- Begin character map dump --");
        for(Character c : mappings.keySet()) Log.println("-- '"+c+"' -> Meta: "+(mappings.get(c).getValue().isPresent()?mappings.get(c).getValue():"no")+", BlockRegister: "+mappings.get(c).getKey().get());
        Log.println("-- End character map dump --\n-- Begin plane dump --");
        for(Integer i : structure.keySet()){
            Log.println("-- V2 offset: "+i+" --");
            structure.get(i).debug();
        }
        Log.println("-- End plane dump --\n-- Begin custom block handler dump --");
        for(ObjectReference<? extends Block> o : customBlockHandler.keySet()) Log.println("-- "+o.get()+" --");
        Log.println("-- End custom block handler dubug --\nThis concludes the dump :)\n");
    }

    /**
     * The assigned wildcard character {@link MultiBlock#WLD} will always be considered a wildcard.
     * @param w World to check.
     * @param at Position to check.
     * @param mapping Character mapping to check.
     * @return Whether or not the given position matches the multi-block structure.
     */
    protected boolean matches(IBlockAccess w, BlockPos at, char mapping, HashMap<Block, Integer> strictMap){
        if(mapping!=WLD && !mappings.containsKey(mapping)) throw new RuntimeException("Unregistered character-block mapping '"+mapping+"'");
        IBlockState b;
        Pair<ObjectReference<Block>, Optional<Integer>> p;
        return
                mapping==WLD || (
                        (b=w.getBlockState(at)).getBlock().equals((p=mappings.get(mapping)).getKey().get()) &&
                        (!p.getValue().isPresent() || b.getBlock().getMetaFromState(b)==p.getValue().or(0)) &&
                        (!strictMap.containsKey(b.getBlock()) || b.getBlock().getMetaFromState(b)==strictMap.get(b.getBlock()))
                );
    }

    protected final boolean isHorizontal(EnumFacing e){ for(EnumFacing e1 : EnumFacing.HORIZONTALS) if(e1==e) return true; return false; }


    public static BlockPos[] clean(Pair<BlockPos, Optional<IBlockState>>[] mapData){
        BlockPos[] b = new BlockPos[mapData.length];
        int ctr = -1;
        for(Pair<BlockPos, Optional<IBlockState>> p : mapData) b[++ctr] = p.getKey();
        return b;
    }

    public static abstract class MBPredicate extends WorldPredicate{
        private IBlockAccess w;
        private BlockPos at;
        private char mapping;
        private HashMap<Block, Integer> strictMap;
        private MultiBlock mb;

        protected boolean matchesDefault(){ return w==null || at==null || strictMap==null || mb==null || mb.matches(w, at, mapping, strictMap); }

        boolean apply(IBlockAccess w, BlockPos at, char mapping, HashMap<Block, Integer> strictMap, MultiBlock mb, BlockPos source){
            this.w = w; this.at = at; this.strictMap = strictMap; this.mb = mb; this.mapping = mapping;
            boolean b = apply(w, at, source);
            w = null; at = null; strictMap = null; mb = null;
            return b;
        }
    }

    /**
     * A two-dimensional plane of characters representing blocks.
     */
    public static class Plane{
        List<char[]> rows = new ArrayList<char[]>();
        final int vOff, hOff;

        /**
         * Create a new plane with a vertical and horizontal offset.
         * @param vOff Offset from leftmost column in the plane that should be considered the centre.
         * @param hOff Offset from bottommost row in the plane that should be considered the centre.
         */
        public Plane(int vOff, int hOff){ this.vOff = vOff; this.hOff = hOff; }

        /**
         * Add a row to the bottom of the plane.
         * @param row Defined block mappings
         * @return  Reference to this object for chaining.
         */
        public Plane addRow(char... row){ rows.add(row); return this; }

        /**
         * Add a complete 2d plane of character-block mappings. If previous rows were defined, this plane will be concatenated under it/them.
         * @param plane
         * @return Reference to this object for chaining.
         */
        public Plane addPlane(char[]... plane){ for(char[] c : plane) addRow(c); return this; }

        /**
         * Add a plane filled solely with the given map-character with the specified dimensions.
         * @param map Character to create the plane with.
         * @param width Width of filled plane.
         * @param height Height of filled plane.
         * @return Reference to this object for chaining.
         */
        public Plane addPlane(char map, int width, int height){
            char[][] c = new char[height][width];
            for(int i = 0; i<height; ++i)
                for(int j = 0; j<width; ++j)
                    c[i][j] = map;
            addPlane(c);
            return this;
        }

        public Plane graftPlane(char map, int width, int height, int hOff, int vOff){
            int vCtr, hCtr = 0;
            for(int i = 0; i<rows.size(); ++i){
                if(hCtr>=height) break;
                if(i<hOff) continue;
                vCtr = 0;
                char[] c = rows.get(i);
                if(c.length<vOff+width){
                    char[] c1 = new char[vOff+width];
                    System.arraycopy(c, 0, c1, 0, c.length);
                    c = c1;
                }
                for(int j = 0; j<c.length; ++j)
                    if(j<vOff) continue;
                    else if(vCtr>=width) break;
                    else c[++vCtr] = map;
                rows.set(i, c);
                ++hCtr;
            }
            for(int i = 0; i<height-hCtr; ++i){
                char[] c = new char[vOff+width];
                for(int j = 0; j<vOff; ++j) c[j] = WLD;
                for(int j = 0; j<width; ++j) c[vOff+j] = map;
                rows.add(c);
            }
            return this;
        }

        /**
         * Replace a mapped character at a given x and y coordinate (x defines offset from left, y defines offset from top).
         * @param map Net character to replace with.
         * @param x Offset from left of map (ignores all previously defined offsets).
         * @param y Offset from top of map (ignores all previously defined offsets).
         * @return Reference to this object for chaining.
         */
        public Plane replace(char map, int x, int y){
            rows.get(y)[x] = map;
            return this;
        }

        /**
         * Create an array mapping this plane to an in-world block position based on the supplied centre, the vertical offset and the horizontal offset.
         * After the map is created, it is rotated to align with the given direction.
         * @param centre Specified centre of the plane (as dictated by verital and horizontal offset).
         * @param xOff Offset in the second vertical axis. For simplicity, this is referred to as Axis-X when referring the the rotationally ambiguous plane (pre-rotation).
         * @param direction Axis to align plane with.
         * @return BlockRegister-position-correlated character map relative to the centre.
         */
        public Pair<BlockPos, Character>[] getPosMap(BlockPos centre, int xOff, EnumFacing direction){
            int largest = 0;
            for(char[] c : rows) if(c.length>largest) largest = c.length;
            int largest1 = largest*rows.size();
            Pair<BlockPos, Character>[] b = new Pair[largest1];
            for(int i = 0; i<rows.size(); ++i) {
                char[] c = rows.get(i);
                for (int j = 0; j<largest; ++j)
                    b[i*largest+j] = new Pair<BlockPos, Character>(
                            rotate(new BlockPos(centre.getX() + xOff, centre.getY() + i - vOff, centre.getZ() + j - hOff), centre, direction),
                            j < c.length ? c[j] : MultiBlock.WLD
                    );
            }
            return b;
        }

        /**
         * Generates a simple mapping defining all points that should be checked. This method ignores any character mappings.<br>
         * @param centre Specified centre of the plane (as dictated by verital and horizontal offset).
         * @param xOff Offset in the second vertical axis. For simplicity, this is referred to as Axis-X when referring the the rotationally ambiguous plane (pre-rotation).
         * @param direction Axis to align plane with.
         * @param includeImplicit Whether or not to include implicitly defined block positions (i.e. filler, wildcard points the ensure that the plane is rectangular).
         * @return BlockPos array relative to the centre.
         */
        public BlockPos[] getSimpleMap(BlockPos centre, int xOff, EnumFacing direction, boolean includeImplicit){
            int i1 = 0, i2 = -1;
            BlockPos[] b;
            if(includeImplicit) {
                for (char[] c : rows) if (c.length > i1) i1 = c.length;
                i2 = i1 * rows.size();
                b = new BlockPos[i2];
                for (int i = 0; i < i2; ++i)
                    b[i] = rotate(new BlockPos(centre.getX() + xOff, centre.getY() + i - vOff, centre.getZ() + i % i1 - hOff), centre, direction);
            }else{
                for(char[] c : rows) i1+=c.length;
                b = new BlockPos[i1];
                for(int i = 0; i<rows.size(); ++i) {
                    char[] c = rows.get(i);
                    for (int j = 0; j<c.length; ++j) b[++i2] = rotate(new BlockPos(centre.getX() + xOff, centre.getY() + i - vOff, centre.getZ() + j - hOff), centre, direction);
                }
            }
            return b;
        }

        /**
         * Calculate a rotation around a certain position such that the new position is considered aligned with the supplied direction.
         * @param pos Position to rotate.
         * @param around Position to pivot rotation around.
         * @param align How to align the rotation.
         * @return A new BlockPos object defining a rotated version of the given position around the pivot.
         */
        public BlockPos rotate(BlockPos pos, BlockPos around, EnumFacing align){
            if(align==EnumFacing.EAST) return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
            return WorldHelper.rotatePos(
                            align==EnumFacing.WEST?WorldHelper.rotatePos(pos, around, WorldHelper.Rotate.RIGHT):
                                    pos, around, align==EnumFacing.SOUTH||align==EnumFacing.WEST?WorldHelper.Rotate.RIGHT:WorldHelper.Rotate.LEFT);
        }

        /**
         * Create an exact (and completely separate) copy of this plane.
         * @return Reference to this object for chaining.
         */
        public Plane copy(){
            Plane p = new Plane(vOff, hOff);
            for(char[] c : rows) p.rows.add(Arrays.copyOf(c, c.length));
            return p;
        }

        /**
         * Debug method to dump information about plane.
         */
        public void debug(){
            Log.println("\n-- Plane debug --\n-- Vertical offset: "+vOff+" --\n-- Horizontal offset: "+hOff+" --\n-- Begin search plane dump --");
            for(char[] c : rows){
                Log.print("\t");
                for(char c1 : c) Log.print(c1+" ");
                Log.println();
            }
            Log.println("-- End search plane dump --\n");
        }
    }
}