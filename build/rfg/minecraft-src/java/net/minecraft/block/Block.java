package net.minecraft.block;

import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryNamespacedDefaultedByKey;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Block extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<Block>
{
    /** ResourceLocation for the Air block */
    private static final ResourceLocation AIR_ID = new ResourceLocation("air");
    public static final RegistryNamespacedDefaultedByKey<ResourceLocation, Block> REGISTRY = net.minecraftforge.registries.GameData.getWrapperDefaulted(Block.class);
    @Deprecated //Modders: DO NOT use this! Use GameRegistry
    public static final ObjectIntIdentityMap<IBlockState> BLOCK_STATE_IDS = net.minecraftforge.registries.GameData.getBlockStateIDMap();
    public static final AxisAlignedBB FULL_BLOCK_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    @Nullable
    public static final AxisAlignedBB NULL_AABB = null;
    private CreativeTabs displayOnCreativeTab;
    protected boolean fullBlock;
    /** How much light is subtracted for going through this block */
    protected int lightOpacity;
    protected boolean translucent;
    /** Amount of light emitted */
    protected int lightValue;
    /** Flag if block should use the brightest neighbor light value as its own */
    protected boolean useNeighborBrightness;
    /** Indicates how many hits it takes to break a block. */
    public float blockHardness;
    /** Indicates how much this block can resist explosions */
    protected float blockResistance;
    protected boolean enableStats;
    /**
     * Flags whether or not this block is of a type that needs random ticking. Ref-counted by ExtendedBlockStorage in
     * order to broadly cull a chunk from the random chunk update list for efficiency's sake.
     */
    protected boolean needsRandomTick;
    /** true if the Block contains a Tile Entity */
    protected boolean hasTileEntity;
    /** Sound of stepping on the block */
    protected SoundType blockSoundType;
    public float blockParticleGravity;
    protected final Material material;
    /** The Block's MapColor */
    protected final MapColor blockMapColor;
    /** Determines how much velocity is maintained while moving on top of this block */
    @Deprecated // Forge: State/world/pos/entity sensitive version below
    public float slipperiness;
    protected final BlockStateContainer blockState;
    private IBlockState defaultBlockState;
    private String translationKey;

    public static int getIdFromBlock(Block blockIn)
    {
        return REGISTRY.getIDForObject(blockIn);
    }

    /**
     * Get a unique ID for the given BlockState, containing both BlockID and metadata
     */
    public static int getStateId(IBlockState state)
    {
        Block block = state.getBlock();
        return getIdFromBlock(block) + (block.getMetaFromState(state) << 12);
    }

    public static Block getBlockById(int id)
    {
        return REGISTRY.getObjectById(id);
    }

    /**
     * Get a BlockState by it's ID (see getStateId)
     */
    public static IBlockState getStateById(int id)
    {
        int i = id & 4095;
        int j = id >> 12 & 15;
        return getBlockById(i).getStateFromMeta(j);
    }

    public static Block getBlockFromItem(@Nullable Item itemIn)
    {
        return itemIn instanceof ItemBlock ? ((ItemBlock)itemIn).getBlock() : Blocks.AIR;
    }

    @Nullable
    public static Block getBlockFromName(String name)
    {
        ResourceLocation resourcelocation = new ResourceLocation(name);

        if (REGISTRY.containsKey(resourcelocation))
        {
            return REGISTRY.getObject(resourcelocation);
        }
        else
        {
            try
            {
                return REGISTRY.getObjectById(Integer.parseInt(name));
            }
            catch (NumberFormatException var3)
            {
                return null;
            }
        }
    }

    /**
     * Determines if the block is solid enough on the top side to support other blocks, like redstone components.
     * @deprecated prefer calling {@link IBlockState#isTopSolid()} wherever possible
     */
    @Deprecated
    public boolean isTopSolid(IBlockState state)
    {
        return state.getMaterial().isOpaque() && state.isFullCube();
    }

    /**
     * @return true if the state occupies all of its 1x1x1 cube
     * @deprecated prefer calling {@link IBlockState#isFullBlock()}
     */
    @Deprecated
    public boolean isFullBlock(IBlockState state)
    {
        return this.fullBlock;
    }

    /**
     * @return true if the passed entity is allowed to spawn on this block.
     * @deprecated prefer calling {@link IBlockState#canEntitySpawn(Entity)}
     */
    @Deprecated
    public boolean canEntitySpawn(IBlockState state, Entity entityIn)
    {
        return true;
    }

    /**
     * Get how much light is subtracted for going through this block
     * @deprecated prefer calling {@link IBlockState#getLightOpacity()}
     */
    @Deprecated
    public int getLightOpacity(IBlockState state)
    {
        return this.lightOpacity;
    }

    /**
     * Used in the renderer to apply ambient occlusion
     * @deprecated prefer calling {@link IBlockState#isTranslucent()}
     */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public boolean isTranslucent(IBlockState state)
    {
        return this.translucent;
    }

    /**
     * Amount of light emitted
     * @deprecated prefer calling {@link IBlockState#getLightValue()}
     */
    @Deprecated
    public int getLightValue(IBlockState state)
    {
        return this.lightValue;
    }

    /**
     * Should block use the brightest neighbor light value as its own
     * @deprecated call via {@link IBlockState#useNeighborBrightness()} whenever possible. Implementing/overriding is
     * fine.
     */
    @Deprecated
    public boolean getUseNeighborBrightness(IBlockState state)
    {
        return this.useNeighborBrightness;
    }

    /**
     * Get a material of block
     * @deprecated call via {@link IBlockState#getMaterial()} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public Material getMaterial(IBlockState state)
    {
        return this.material;
    }

    /**
     * Get the MapColor for this Block and the given BlockState
     * @deprecated call via {@link IBlockState#getMapColor(IBlockAccess,BlockPos)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        return this.blockMapColor;
    }

    /**
     * Convert the given metadata into a BlockState for this Block
     */
    @Deprecated
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState();
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    public int getMetaFromState(IBlockState state)
    {
        if (state.getPropertyKeys().isEmpty())
        {
            return 0;
        }
        else
        {
            throw new IllegalArgumentException("Don't know how to convert " + state + " back into data...");
        }
    }

    /**
     * Get the actual Block state of this Block at the given position. This applies properties not visible in the
     * metadata, such as fence connections.
     */
    @Deprecated
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        return state;
    }

    /**
     * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed
     * blockstate.
     * @deprecated call via {@link IBlockState#withRotation(Rotation)} whenever possible. Implementing/overriding is
     * fine.
     */
    @Deprecated
    public IBlockState withRotation(IBlockState state, Rotation rot)
    {
        return state;
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed
     * blockstate.
     * @deprecated call via {@link IBlockState#withMirror(Mirror)} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
    {
        return state;
    }

    public Block(Material blockMaterialIn, MapColor blockMapColorIn)
    {
        this.enableStats = true;
        this.blockSoundType = SoundType.STONE;
        this.blockParticleGravity = 1.0F;
        this.slipperiness = 0.6F;
        this.material = blockMaterialIn;
        this.blockMapColor = blockMapColorIn;
        this.blockState = this.createBlockState();
        this.setDefaultState(this.blockState.getBaseState());
        this.fullBlock = this.getDefaultState().isOpaqueCube();
        this.lightOpacity = this.fullBlock ? 255 : 0;
        this.translucent = !blockMaterialIn.blocksLight();
    }

    public Block(Material materialIn)
    {
        this(materialIn, materialIn.getMaterialMapColor());
    }

    /**
     * Sets the footstep sound for the block. Returns the object for convenience in constructing.
     */
    protected Block setSoundType(SoundType sound)
    {
        this.blockSoundType = sound;
        return this;
    }

    /**
     * Sets how much light is blocked going through this block. Returns the object for convenience in constructing.
     */
    public Block setLightOpacity(int opacity)
    {
        this.lightOpacity = opacity;
        return this;
    }

    /**
     * Sets the light value that the block emits. Returns resulting block instance for constructing convenience.
     */
    public Block setLightLevel(float value)
    {
        this.lightValue = (int)(15.0F * value);
        return this;
    }

    /**
     * Sets the the blocks resistance to explosions. Returns the object for convenience in constructing.
     */
    public Block setResistance(float resistance)
    {
        this.blockResistance = resistance * 3.0F;
        return this;
    }

    protected static boolean isExceptionBlockForAttaching(Block attachBlock)
    {
        return attachBlock instanceof BlockShulkerBox || attachBlock instanceof BlockLeaves || attachBlock instanceof BlockTrapDoor || attachBlock == Blocks.BEACON || attachBlock == Blocks.CAULDRON || attachBlock == Blocks.GLASS || attachBlock == Blocks.GLOWSTONE || attachBlock == Blocks.ICE || attachBlock == Blocks.SEA_LANTERN || attachBlock == Blocks.STAINED_GLASS;
    }

    protected static boolean isExceptBlockForAttachWithPiston(Block attachBlock)
    {
        return isExceptionBlockForAttaching(attachBlock) || attachBlock == Blocks.PISTON || attachBlock == Blocks.STICKY_PISTON || attachBlock == Blocks.PISTON_HEAD;
    }

    /**
     * Indicate if a material is a normal solid opaque cube
     * @deprecated call via {@link IBlockState#isBlockNormalCube()} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public boolean isBlockNormalCube(IBlockState state)
    {
        return state.getMaterial().blocksMovement() && state.isFullCube();
    }

    /**
     * Used for nearly all game logic (non-rendering) purposes. Use Forge-provided isNormalCube(IBlockAccess, BlockPos)
     * instead.
     * @deprecated call via {@link IBlockState#isNormalCube()} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public boolean isNormalCube(IBlockState state)
    {
        return state.getMaterial().isOpaque() && state.isFullCube() && !state.canProvidePower();
    }

    /**
     * @deprecated call via {@link IBlockState#causesSuffocation()} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public boolean causesSuffocation(IBlockState state)
    {
        return this.material.blocksMovement() && this.getDefaultState().isFullCube();
    }

    /**
     * @deprecated call via {@link IBlockState#isFullCube()} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public boolean isFullCube(IBlockState state)
    {
        return true;
    }

    /**
     * @deprecated call via {@link IBlockState#hasCustomBreakingProgress()} whenever possible. Implementing/overriding
     * is fine.
     */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public boolean hasCustomBreakingProgress(IBlockState state)
    {
        return false;
    }

    /**
     * Determines if an entity can path through this block
     */
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos)
    {
        return !this.material.blocksMovement();
    }

    /**
     * The type of render function called. MODEL for mixed tesr and static model, MODELBLOCK_ANIMATED for TESR-only,
     * LIQUID for vanilla liquids, INVISIBLE to skip all rendering
     * @deprecated call via {@link IBlockState#getRenderType()} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.MODEL;
    }

    /**
     * Whether this Block can be replaced directly by other blocks (true for e.g. tall grass)
     */
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
    {
        return worldIn.getBlockState(pos).getMaterial().isReplaceable();
    }

    /**
     * Sets how many hits it takes to break a block.
     */
    public Block setHardness(float hardness)
    {
        this.blockHardness = hardness;

        if (this.blockResistance < hardness * 5.0F)
        {
            this.blockResistance = hardness * 5.0F;
        }

        return this;
    }

    public Block setBlockUnbreakable()
    {
        this.setHardness(-1.0F);
        return this;
    }

    /**
     * @deprecated call via {@link IBlockState#getBlockHardness(World,BlockPos)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    public float getBlockHardness(IBlockState blockState, World worldIn, BlockPos pos)
    {
        return this.blockHardness;
    }

    /**
     * Sets whether this block type will receive random update ticks
     */
    public Block setTickRandomly(boolean shouldTick)
    {
        this.needsRandomTick = shouldTick;
        return this;
    }

    /**
     * Returns whether or not this block is of a type that needs random ticking. Called for ref-counting purposes by
     * ExtendedBlockStorage in order to broadly cull a chunk from the random chunk update list for efficiency's sake.
     */
    public boolean getTickRandomly()
    {
        return this.needsRandomTick;
    }

    @Deprecated //Forge: New State sensitive version.
    public boolean hasTileEntity()
    {
        return hasTileEntity(getDefaultState());
    }

    /**
     * @deprecated call via {@link IBlockState#getBoundingBox(IBlockAccess,BlockPos)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return FULL_BLOCK_AABB;
    }

    /**
     * @deprecated call via {@link IBlockState#getPackedLightmapCoords(IBlockAccess,BlockPos)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public int getPackedLightmapCoords(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        int i = source.getCombinedLight(pos, state.getLightValue(source, pos));

        if (i == 0 && state.getBlock() instanceof BlockSlab)
        {
            pos = pos.down();
            state = source.getBlockState(pos);
            return source.getCombinedLight(pos, state.getLightValue(source, pos));
        }
        else
        {
            return i;
        }
    }

    /**
     * @deprecated call via {@link IBlockState#shouldSideBeRendered(IBlockAccess,BlockPos,EnumFacing)} whenever
     * possible. Implementing/overriding is fine.
     */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        AxisAlignedBB axisalignedbb = blockState.getBoundingBox(blockAccess, pos);

        switch (side)
        {
            case DOWN:

                if (axisalignedbb.minY > 0.0D)
                {
                    return true;
                }

                break;
            case UP:

                if (axisalignedbb.maxY < 1.0D)
                {
                    return true;
                }

                break;
            case NORTH:

                if (axisalignedbb.minZ > 0.0D)
                {
                    return true;
                }

                break;
            case SOUTH:

                if (axisalignedbb.maxZ < 1.0D)
                {
                    return true;
                }

                break;
            case WEST:

                if (axisalignedbb.minX > 0.0D)
                {
                    return true;
                }

                break;
            case EAST:

                if (axisalignedbb.maxX < 1.0D)
                {
                    return true;
                }
        }

        return !blockAccess.getBlockState(pos.offset(side)).doesSideBlockRendering(blockAccess, pos.offset(side), side.getOpposite());
    }

    /**
     * Get the geometry of the queried face at the given position and state. This is used to decide whether things like
     * buttons are allowed to be placed on the face, or how glass panes connect to the face, among other things.
     * <p>
     * Common values are {@code SOLID}, which is the default, and {@code UNDEFINED}, which represents something that
     * does not fit the other descriptions and will generally cause other things not to connect to the face.
     * 
     * @return an approximation of the form of the given face
     * @deprecated call via {@link IBlockState#getBlockFaceShape(IBlockAccess,BlockPos,EnumFacing)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
    {
        return BlockFaceShape.SOLID;
    }

    @Deprecated
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
    {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, state.getCollisionBoundingBox(worldIn, pos));
    }

    /**
     * @deprecated call via {@link IBlockState#addCollisionBoxToList(World,BlockPos,AxisAlignedBB,List,Entity,boolean)}
     * whenever possible. Implementing/overriding is fine.
     */
    protected static void addCollisionBoxToList(BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable AxisAlignedBB blockBox)
    {
        if (blockBox != NULL_AABB)
        {
            AxisAlignedBB axisalignedbb = blockBox.offset(pos);

            if (entityBox.intersects(axisalignedbb))
            {
                collidingBoxes.add(axisalignedbb);
            }
        }
    }

    /**
     * @deprecated call via {@link IBlockState#getCollisionBoundingBox(IBlockAccess,BlockPos)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos)
    {
        return blockState.getBoundingBox(worldIn, pos);
    }

    /**
     * Return an AABB (in world coords!) that should be highlighted when the player is targeting this Block
     * @deprecated call via {@link IBlockState#getSelectedBoundingBox(World,BlockPos)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos)
    {
        return state.getBoundingBox(worldIn, pos).offset(pos);
    }

    /**
     * Used to determine ambient occlusion and culling when rebuilding chunks for render
     * @deprecated call via {@link IBlockState#isOpaqueCube()} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public boolean isOpaqueCube(IBlockState state)
    {
        return true;
    }

    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid)
    {
        return this.isCollidable();
    }

    /**
     * Returns if this block is collidable. Only used by fire, although stairs return that of the block that the stair
     * is made of (though nobody's going to make fire stairs, right?)
     */
    public boolean isCollidable()
    {
        return true;
    }

    /**
     * Called randomly when setTickRandomly is set to true (used by e.g. crops to grow, etc.)
     */
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random)
    {
        this.updateTick(worldIn, pos, state, random);
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles). Note that
     * this method is unrelated to {@link randomTick} and {@link #needsRandomTick}, and will always be called regardless
     * of whether the block can receive random update ticks
     */
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand)
    {
    }

    /**
     * Called after a player destroys this Block - the posiiton pos may no longer hold the state indicated.
     */
    public void onPlayerDestroy(World worldIn, BlockPos pos, IBlockState state)
    {
    }

    /**
     * Called when a neighboring block was changed and marks that this state should perform any checks during a neighbor
     * change. Cases may include when redstone power is updated, cactus blocks popping off due to a neighboring solid
     * block, etc.
     */
    @Deprecated
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
    }

    /**
     * How many world ticks before ticking
     */
    public int tickRate(World worldIn)
    {
        return 10;
    }

    /**
     * Called after the block is set in the Chunk data, but before the Tile Entity is set
     */
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state)
    {
    }

    /**
     * Called serverside after this block is replaced with another in Chunk, but before the Tile Entity is updated
     */
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        if (hasTileEntity(state) && !(this instanceof BlockContainer))
        {
            worldIn.removeTileEntity(pos);
        }
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    public int quantityDropped(Random random)
    {
        return 1;
    }

    /**
     * Get the Item that this Block should drop when harvested.
     */
    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Item.getItemFromBlock(this);
    }

    /**
     * Get the hardness of this Block relative to the ability of the given player
     * @deprecated call via {@link IBlockState#getPlayerRelativeBlockHardness(EntityPlayer,World,BlockPos)} whenever
     * possible. Implementing/overriding is fine.
     */
    @Deprecated
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World worldIn, BlockPos pos)
    {
        return net.minecraftforge.common.ForgeHooks.blockStrength(state, player, worldIn, pos);
    }

    /**
     * Spawn this Block's drops into the World as EntityItems
     */
    public final void dropBlockAsItem(World worldIn, BlockPos pos, IBlockState state, int fortune)
    {
        this.dropBlockAsItemWithChance(worldIn, pos, state, 1.0F, fortune);
    }

    /**
     * Spawns this Block's drops into the World as EntityItems.
     */
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune)
    {
        if (!worldIn.isRemote && !worldIn.restoringBlockSnapshots) // do not drop items while restoring blockstates, prevents item dupe
        {
            List<ItemStack> drops = getDrops(worldIn, pos, state, fortune); // use the old method until it gets removed, for backward compatibility
            chance = net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(drops, worldIn, pos, state, fortune, chance, false, harvesters.get());

            for (ItemStack drop : drops)
            {
                if (worldIn.rand.nextFloat() <= chance)
                {
                    spawnAsEntity(worldIn, pos, drop);
                }
            }
        }
    }

    /**
     * Spawns the given ItemStack as an EntityItem into the World at the given position
     */
    public static void spawnAsEntity(World worldIn, BlockPos pos, ItemStack stack)
    {
        if (!worldIn.isRemote && !stack.isEmpty() && worldIn.getGameRules().getBoolean("doTileDrops")&& !worldIn.restoringBlockSnapshots) // do not drop items while restoring blockstates, prevents item dupe
        {
            if (captureDrops.get())
            {
                capturedDrops.get().add(stack);
                return;
            }
            float f = 0.5F;
            double d0 = (double)(worldIn.rand.nextFloat() * 0.5F) + 0.25D;
            double d1 = (double)(worldIn.rand.nextFloat() * 0.5F) + 0.25D;
            double d2 = (double)(worldIn.rand.nextFloat() * 0.5F) + 0.25D;
            EntityItem entityitem = new EntityItem(worldIn, (double)pos.getX() + d0, (double)pos.getY() + d1, (double)pos.getZ() + d2, stack);
            entityitem.setDefaultPickupDelay();
            worldIn.spawnEntity(entityitem);
        }
    }

    /**
     * Spawns the given amount of experience into the World as XP orb entities
     */
    public void dropXpOnBlockBreak(World worldIn, BlockPos pos, int amount)
    {
        if (!worldIn.isRemote && worldIn.getGameRules().getBoolean("doTileDrops"))
        {
            while (amount > 0)
            {
                int i = EntityXPOrb.getXPSplit(amount);
                amount -= i;
                worldIn.spawnEntity(new EntityXPOrb(worldIn, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, i));
            }
        }
    }

    /**
     * Gets the metadata of the item this Block can drop. This method is called when the block gets destroyed. It
     * returns the metadata of the dropped item based on the old metadata of the block.
     */
    public int damageDropped(IBlockState state)
    {
        return 0;
    }

    /**
     * Returns how much this block can resist explosions from the passed in entity.
     */
    @Deprecated //Forge: State sensitive version
    public float getExplosionResistance(Entity exploder)
    {
        return this.blockResistance / 5.0F;
    }

    /**
     * Ray traces through the blocks collision from start vector to end vector returning a ray trace hit.
     * @deprecated call via {@link IBlockState#collisionRayTrace(World,BlockPos,Vec3d,Vec3d)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    @Nullable
    public RayTraceResult collisionRayTrace(IBlockState blockState, World worldIn, BlockPos pos, Vec3d start, Vec3d end)
    {
        return this.rayTrace(pos, start, end, blockState.getBoundingBox(worldIn, pos));
    }

    @Nullable
    protected RayTraceResult rayTrace(BlockPos pos, Vec3d start, Vec3d end, AxisAlignedBB boundingBox)
    {
        Vec3d vec3d = start.subtract((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        Vec3d vec3d1 = end.subtract((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        RayTraceResult raytraceresult = boundingBox.calculateIntercept(vec3d, vec3d1);
        return raytraceresult == null ? null : new RayTraceResult(raytraceresult.hitVec.add((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()), raytraceresult.sideHit, pos);
    }

    /**
     * Called when this Block is destroyed by an Explosion
     */
    public void onExplosionDestroy(World worldIn, BlockPos pos, Explosion explosionIn)
    {
    }

    /**
     * Gets the render layer this block will render on. SOLID for solid blocks, CUTOUT or CUTOUT_MIPPED for on-off
     * transparency (glass, reeds), TRANSLUCENT for fully blended transparency (stained glass)
     */
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer()
    {
        return BlockRenderLayer.SOLID;
    }

    /**
     * Check whether this Block can be placed at pos, while aiming at the specified side of an adjacent block
     */
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side)
    {
        return this.canPlaceBlockAt(worldIn, pos);
    }

    /**
     * Checks if this block can be placed exactly at the given position.
     */
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        return worldIn.getBlockState(pos).getBlock().isReplaceable(worldIn, pos);
    }

    /**
     * Called when the block is right clicked by a player.
     */
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        return false;
    }

    /**
     * Called when the given entity walks on this Block
     */
    public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn)
    {
    }

    // Forge: use getStateForPlacement
    /**
     * Called by ItemBlocks just before a block is actually set in the world, to allow for adjustments to the
     * IBlockstate
     */
    @Deprecated
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        return this.getStateFromMeta(meta);
    }

    public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn)
    {
    }

    public Vec3d modifyAcceleration(World worldIn, BlockPos pos, Entity entityIn, Vec3d motion)
    {
        return motion;
    }

    /**
     * @deprecated call via {@link IBlockState#getWeakPower(IBlockAccess,BlockPos,EnumFacing)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return 0;
    }

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     * @deprecated call via {@link IBlockState#canProvidePower()} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public boolean canProvidePower(IBlockState state)
    {
        return false;
    }

    /**
     * Called When an Entity Collided with the Block
     */
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn)
    {
    }

    /**
     * @deprecated call via {@link IBlockState#getStrongPower(IBlockAccess,BlockPos,EnumFacing)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return 0;
    }

    /**
     * Spawns the block's drops in the world. By the time this is called the Block has possibly been set to air via
     * Block.removedByPlayer
     */
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack)
    {
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);

        if (this.canSilkHarvest(worldIn, pos, state, player) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0)
        {
            java.util.List<ItemStack> items = new java.util.ArrayList<ItemStack>();
            ItemStack itemstack = this.getSilkTouchDrop(state);

            if (!itemstack.isEmpty())
            {
                items.add(itemstack);
            }

            net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(items, worldIn, pos, state, 0, 1.0f, true, player);
            for (ItemStack item : items)
            {
                spawnAsEntity(worldIn, pos, item);
            }
        }
        else
        {
            harvesters.set(player);
            int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);
            this.dropBlockAsItem(worldIn, pos, state, i);
            harvesters.set(null);
        }
    }

    @Deprecated //Forge: State sensitive version
    protected boolean canSilkHarvest()
    {
        return this.getDefaultState().isFullCube() && !this.hasTileEntity(silk_check_state.get());
    }

    protected ItemStack getSilkTouchDrop(IBlockState state)
    {
        Item item = Item.getItemFromBlock(this);
        int i = 0;

        if (item.getHasSubtypes())
        {
            i = this.getMetaFromState(state);
        }

        return new ItemStack(item, 1, i);
    }

    /**
     * Get the quantity dropped based on the given fortune level
     */
    public int quantityDroppedWithBonus(int fortune, Random random)
    {
        return this.quantityDropped(random);
    }

    /**
     * Called by ItemBlocks after a block is set in the world, to allow post-place logic
     */
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
    }

    /**
     * Return true if an entity can be spawned inside the block (used to get the player's bed spawn location)
     */
    public boolean canSpawnInBlock()
    {
        return !this.material.isSolid() && !this.material.isLiquid();
    }

    public Block setTranslationKey(String key)
    {
        this.translationKey = key;
        return this;
    }

    /**
     * Gets the localized name of this block. Used for the statistics page.
     */
    public String getLocalizedName()
    {
        return I18n.translateToLocal(this.getTranslationKey() + ".name");
    }

    /**
     * Returns the unlocalized name of the block with "tile." appended to the front.
     */
    public String getTranslationKey()
    {
        return "tile." + this.translationKey;
    }

    /**
     * Called on server when World#addBlockEvent is called. If server returns true, then also called on the client. On
     * the Server, this may perform additional changes to the world, like pistons replacing the block with an extended
     * base. On the client, the update may involve replacing tile entities or effects such as sounds or particles
     * @deprecated call via {@link IBlockState#onBlockEventReceived(World,BlockPos,int,int)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id, int param)
    {
        return false;
    }

    /**
     * Return the state of blocks statistics flags - if the block is counted for mined and placed.
     */
    public boolean getEnableStats()
    {
        return this.enableStats;
    }

    protected Block disableStats()
    {
        this.enableStats = false;
        return this;
    }

    /**
     * @deprecated call via {@link IBlockState#getMobilityFlag()} whenever possible. Implementing/overriding is fine.
     */
    @Deprecated
    public EnumPushReaction getPushReaction(IBlockState state)
    {
        return this.material.getPushReaction();
    }

    /**
     * @deprecated call via {@link IBlockState#getAmbientOcclusionLightValue()} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public float getAmbientOcclusionLightValue(IBlockState state)
    {
        return state.isBlockNormalCube() ? 0.2F : 1.0F;
    }

    /**
     * Block's chance to react to a living entity falling on it.
     */
    public void onFallenUpon(World worldIn, BlockPos pos, Entity entityIn, float fallDistance)
    {
        entityIn.fall(fallDistance, 1.0F);
    }

    /**
     * Called when an Entity lands on this Block. This method *must* update motionY because the entity will not do that
     * on its own
     */
    public void onLanded(World worldIn, Entity entityIn)
    {
        entityIn.motionY = 0.0D;
    }

    @Deprecated // Forge: Use more sensitive version below: getPickBlock
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state)
    {
        return new ItemStack(Item.getItemFromBlock(this), 1, this.damageDropped(state));
    }

    /**
     * returns a list of blocks with the same ID, but different meta (eg: wood returns 4 blocks)
     */
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items)
    {
        items.add(new ItemStack(this));
    }

    /**
     * Returns the CreativeTab to display the given block on.
     */
    public CreativeTabs getCreativeTab()
    {
        return this.displayOnCreativeTab;
    }

    public Block setCreativeTab(CreativeTabs tab)
    {
        this.displayOnCreativeTab = tab;
        return this;
    }

    /**
     * Called before the Block is set to air in the world. Called regardless of if the player's tool can actually
     * collect this block
     */
    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player)
    {
    }

    /**
     * Called similar to random ticks, but only when it is raining.
     */
    public void fillWithRain(World worldIn, BlockPos pos)
    {
    }

    public boolean requiresUpdates()
    {
        return true;
    }

    /**
     * Return whether this block can drop from an explosion.
     */
    public boolean canDropFromExplosion(Explosion explosionIn)
    {
        return true;
    }

    public boolean isAssociatedBlock(Block other)
    {
        return this == other;
    }

    public static boolean isEqualTo(Block blockIn, Block other)
    {
        if (blockIn != null && other != null)
        {
            return blockIn == other ? true : blockIn.isAssociatedBlock(other);
        }
        else
        {
            return false;
        }
    }

    /**
     * @deprecated call via {@link IBlockState#hasComparatorInputOverride()} whenever possible. Implementing/overriding
     * is fine.
     */
    @Deprecated
    public boolean hasComparatorInputOverride(IBlockState state)
    {
        return false;
    }

    /**
     * @deprecated call via {@link IBlockState#getComparatorInputOverride(World,BlockPos)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        return 0;
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[0]);
    }

    public BlockStateContainer getBlockState()
    {
        return this.blockState;
    }

    protected final void setDefaultState(IBlockState state)
    {
        this.defaultBlockState = state;
    }

    /**
     * Gets the default state for this block
     */
    public final IBlockState getDefaultState()
    {
        return this.defaultBlockState;
    }

    /**
     * Get the OffsetType for this Block. Determines if the model is rendered slightly offset.
     */
    public Block.EnumOffsetType getOffsetType()
    {
        return Block.EnumOffsetType.NONE;
    }

    /**
     * @deprecated call via {@link IBlockState#getOffset(IBlockAccess,BlockPos)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Deprecated
    public Vec3d getOffset(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        Block.EnumOffsetType block$enumoffsettype = this.getOffsetType();

        if (block$enumoffsettype == Block.EnumOffsetType.NONE)
        {
            return Vec3d.ZERO;
        }
        else
        {
            long i = MathHelper.getCoordinateRandom(pos.getX(), 0, pos.getZ());
            return new Vec3d(((double)((float)(i >> 16 & 15L) / 15.0F) - 0.5D) * 0.5D, block$enumoffsettype == Block.EnumOffsetType.XYZ ? ((double)((float)(i >> 20 & 15L) / 15.0F) - 1.0D) * 0.2D : 0.0D, ((double)((float)(i >> 24 & 15L) / 15.0F) - 0.5D) * 0.5D);
        }
    }

    @Deprecated // Forge - World/state/pos/entity sensitive version below
    public SoundType getSoundType()
    {
        return this.blockSoundType;
    }

    public String toString()
    {
        return "Block{" + REGISTRY.getNameForObject(this) + "}";
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
    }

    /* ======================================== FORGE START =====================================*/
    //For ForgeInternal use Only!
    protected ThreadLocal<EntityPlayer> harvesters = new ThreadLocal();
    private ThreadLocal<IBlockState> silk_check_state = new ThreadLocal();
    protected static java.util.Random RANDOM = new java.util.Random(); // Useful for random things without a seed.

    /**
     * Gets the slipperiness at the given location at the given state. Normally
     * between 0 and 1.
     * <p>
     * Note that entities may reduce slipperiness by a certain factor of their own;
     * for {@link net.minecraft.entity.EntityLivingBase}, this is {@code .91}.
     * {@link net.minecraft.entity.item.EntityItem} uses {@code .98}, and
     * {@link net.minecraft.entity.projectile.EntityFishHook} uses {@code .92}.
     *
     * @param state state of the block
     * @param world the world
     * @param pos the position in the world
     * @param entity the entity in question
     * @return the factor by which the entity's motion should be multiplied
     */
    public float getSlipperiness(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable Entity entity)
    {
        return slipperiness;
    }

    /**
     * Sets the base slipperiness level. Normally between 0 and 1.
     * <p>
     * <b>Calling this method may have no effect on the function of this block</b>,
     * or may not have the expected result. This block is free to caclculate
     * its slipperiness arbitrarily. This method is guaranteed to work on the
     * base {@code Block} class.
     *
     * @param slipperiness the base slipperiness of this block
     * @see #getSlipperiness(IBlockState, IBlockAccess, BlockPos, Entity)
     */
    public void setDefaultSlipperiness(float slipperiness)
    {
        this.slipperiness = slipperiness;
    }

    /**
     * Get a light value for this block, taking into account the given state and coordinates, normal ranges are between 0 and 15
     *
     * @param state Block state
     * @param world The current world
     * @param pos Block position in world
     * @return The light value
     */
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return state.getLightValue();
    }

    /**
     * Checks if a player or entity can use this block to 'climb' like a ladder.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @param entity The entity trying to use the ladder, CAN be null.
     * @return True if the block should act like a ladder
     */
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) { return false; }

    /**
     * Return true if the block is a normal, solid cube.  This
     * determines indirect power state, entity ejection from blocks, and a few
     * others.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @return True if the block is a full cube
     */
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return state.isNormalCube();
    }

    /**
     * Check if the face of a block should block rendering.
     *
     * Faces which are fully opaque should return true, faces with transparency
     * or faces which do not span the full size of the block should return false.
     *
     * @param state The current block state
     * @param world The current world
     * @param pos Block position in world
     * @param face The side to check
     * @return True if the block is opaque on the specified side.
     */
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face)
    {
        return state.isOpaqueCube();
    }

    /**
     * Checks if the block is a solid face on the given side, used by placement logic.
     *
     * @param base_state The base state, getActualState should be called first
     * @param world The current world
     * @param pos Block position in world
     * @param side The side to check
     * @return True if the block is solid on the specified side.
     */
    @Deprecated //Use IBlockState.getBlockFaceShape
    public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side)
    {
        if (base_state.isTopSolid() && side == EnumFacing.UP) // Short circuit to vanilla function if its true
            return true;

        if (this instanceof BlockSlab)
        {
            IBlockState state = this.getActualState(base_state, world, pos);
            return base_state.isFullBlock()
                  || (state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP    && side == EnumFacing.UP  )
                  || (state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM && side == EnumFacing.DOWN);
        }
        else if (this instanceof BlockFarmland)
        {
            return (side != EnumFacing.DOWN && side != EnumFacing.UP);
        }
        else if (this instanceof BlockStairs)
        {
            IBlockState state = this.getActualState(base_state, world, pos);
            boolean flipped = state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP;
            BlockStairs.EnumShape shape = (BlockStairs.EnumShape)state.getValue(BlockStairs.SHAPE);
            EnumFacing facing = (EnumFacing)state.getValue(BlockStairs.FACING);
            if (side == EnumFacing.UP) return flipped;
            if (side == EnumFacing.DOWN) return !flipped;
            if (facing == side) return true;
            if (flipped)
            {
                if (shape == BlockStairs.EnumShape.INNER_LEFT ) return side == facing.rotateYCCW();
                if (shape == BlockStairs.EnumShape.INNER_RIGHT) return side == facing.rotateY();
            }
            else
            {
                if (shape == BlockStairs.EnumShape.INNER_LEFT ) return side == facing.rotateY();
                if (shape == BlockStairs.EnumShape.INNER_RIGHT) return side == facing.rotateYCCW();
            }
            return false;
        }
        else if (this instanceof BlockSnow)
        {
            IBlockState state = this.getActualState(base_state, world, pos);
            return ((Integer)state.getValue(BlockSnow.LAYERS)) >= 8;
        }
        else if (this instanceof BlockHopper && side == EnumFacing.UP)
        {
            return true;
        }
        else if (this instanceof BlockCompressedPowered)
        {
            return true;
        }
        return isNormalCube(base_state, world, pos);
    }

    /**
     * Determines if this block should set fire and deal fire damage
     * to entities coming into contact with it.
     *
     * @param world The current world
     * @param pos Block position in world
     * @return True if the block should deal damage
     */
    public boolean isBurning(IBlockAccess world, BlockPos pos)
    {
        return false;
    }

    /**
     * Determines this block should be treated as an air block
     * by the rest of the code. This method is primarily
     * useful for creating pure logic-blocks that will be invisible
     * to the player and otherwise interact as air would.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @return True if the block considered air
     */
    public boolean isAir(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return state.getMaterial() == Material.AIR;
    }

    /**
     * Determines if the player can harvest this block, obtaining it's drops when the block is destroyed.
     *
     * @param player The player damaging the block
     * @param pos The block's current position
     * @return True to spawn the drops
     */
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player)
    {
        return net.minecraftforge.common.ForgeHooks.canHarvestBlock(this, player, world, pos);
    }

    /**
     * Called when a player removes a block.  This is responsible for
     * actually destroying the block, and the block is intact at time of call.
     * This is called regardless of whether the player can harvest the block or
     * not.
     *
     * Return true if the block is actually destroyed.
     *
     * Note: When used in multiplayer, this is called on both client and
     * server sides!
     *
     * @param state The current state.
     * @param world The current world
     * @param player The player damaging the block, may be null
     * @param pos Block position in world
     * @param willHarvest True if Block.harvestBlock will be called after this, if the return in true.
     *        Can be useful to delay the destruction of tile entities till after harvestBlock
     * @return True if the block is actually destroyed.
     */
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
    {
        this.onBlockHarvested(world, pos, state, player);
        return world.setBlockState(pos, net.minecraft.init.Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
    }

    /**
     * Chance that fire will spread and consume this block.
     * 300 being a 100% chance, 0, being a 0% chance.
     *
     * @param world The current world
     * @param pos Block position in world
     * @param face The face that the fire is coming from
     * @return A number ranging from 0 to 300 relating used to determine if the block will be consumed by fire
     */
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face)
    {
        return net.minecraft.init.Blocks.FIRE.getFlammability(this);
    }

    /**
     * Called when fire is updating, checks if a block face can catch fire.
     *
     *
     * @param world The current world
     * @param pos Block position in world
     * @param face The face that the fire is coming from
     * @return True if the face can be on fire, false otherwise.
     */
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face)
    {
        return getFlammability(world, pos, face) > 0;
    }

    /**
     * Called when fire is updating on a neighbor block.
     * The higher the number returned, the faster fire will spread around this block.
     *
     * @param world The current world
     * @param pos Block position in world
     * @param face The face that the fire is coming from
     * @return A number that is used to determine the speed of fire growth around the block
     */
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face)
    {
        return net.minecraft.init.Blocks.FIRE.getEncouragement(this);
    }

    /**
     * Currently only called by fire when it is on top of this block.
     * Returning true will prevent the fire from naturally dying during updating.
     * Also prevents firing from dying from rain.
     *
     * @param world The current world
     * @param pos Block position in world
     * @param side The face that the fire is coming from
     * @return True if this block sustains fire, meaning it will never go out.
     */
    public boolean isFireSource(World world, BlockPos pos, EnumFacing side)
    {
        if (side != EnumFacing.UP)
            return false;
        if (this == Blocks.NETHERRACK || this == Blocks.MAGMA)
            return true;
        if ((world.provider instanceof net.minecraft.world.WorldProviderEnd) && this == Blocks.BEDROCK)
            return true;
        return false;
    }

    private boolean isTileProvider = this instanceof ITileEntityProvider;
    /**
     * Called throughout the code as a replacement for block instanceof BlockContainer
     * Moving this to the Block base class allows for mods that wish to extend vanilla
     * blocks, and also want to have a tile entity on that block, may.
     *
     * Return true from this function to specify this block has a tile entity.
     *
     * @param state State of the current block
     * @return True if block has a tile entity, false otherwise
     */
    public boolean hasTileEntity(IBlockState state)
    {
        return isTileProvider;
    }

    /**
     * Called throughout the code as a replacement for ITileEntityProvider.createNewTileEntity
     * Return the same thing you would from that function.
     * This will fall back to ITileEntityProvider.createNewTileEntity(World) if this block is a ITileEntityProvider
     *
     * @param state The state of the current block
     * @return A instance of a class extending TileEntity
     */
    @Nullable
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        if (isTileProvider)
        {
            return ((ITileEntityProvider)this).createNewTileEntity(world, getMetaFromState(state));
        }
        return null;
    }

    /**
     * State and fortune sensitive version, this replaces the old (int meta, Random rand)
     * version in 1.1.
     *
     * @param state Current state
     * @param fortune Current item fortune level
     * @param random Random number generator
     * @return The number of items to drop
     */
    public int quantityDropped(IBlockState state, int fortune, Random random)
    {
        return quantityDroppedWithBonus(fortune, random);
    }

    /**
     * @deprecated use {@link #getDrops(NonNullList, IBlockAccess, BlockPos, IBlockState, int)}
     */
    @Deprecated
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        NonNullList<ItemStack> ret = NonNullList.create();
        getDrops(ret, world, pos, state, fortune);
        return ret;
    }

    /**
     * This gets a complete list of items dropped from this block.
     *
     * @param drops add all items this block drops to this drops list
     * @param world The current world
     * @param pos Block position in world
     * @param state Current state
     * @param fortune Breakers fortune level
     */
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        Random rand = world instanceof World ? ((World)world).rand : RANDOM;

        int count = quantityDropped(state, fortune, rand);
        for (int i = 0; i < count; i++)
        {
            Item item = this.getItemDropped(state, rand, fortune);
            if (item != Items.AIR)
            {
                drops.add(new ItemStack(item, 1, this.damageDropped(state)));
            }
        }
    }

    /**
     * Return true from this function if the player with silk touch can harvest this block directly, and not it's normal drops.
     *
     * @param world The world
     * @param pos Block position in world
     * @param state current block state
     * @param player The player doing the harvesting
     * @return True if the block can be directly harvested using silk touch
     */
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player)
    {
        silk_check_state.set(state);;
        boolean ret = this.canSilkHarvest();
        silk_check_state.set(null);
        return ret;
    }

    /**
     * Determines if a specified mob type can spawn on this block, returning false will
     * prevent any mob from spawning on the block.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @param type The Mob Category Type
     * @return True to allow a mob of the specified category to spawn, false to prevent it.
     */
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.entity.EntityLiving.SpawnPlacementType type)
    {
        return isSideSolid(state, world, pos, EnumFacing.UP);
    }

    /**
     * Determines if this block is classified as a Bed, Allowing
     * players to sleep in it, though the block has to specifically
     * perform the sleeping functionality in it's activated event.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @param player The player or camera entity, null in some cases.
     * @return True to treat this as a bed
     */
    public boolean isBed(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable Entity player)
    {
        return this == net.minecraft.init.Blocks.BED;
    }

    /**
     * Returns the position that the player is moved to upon
     * waking up, or respawning at the bed.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @param player The player or camera entity, null in some cases.
     * @return The spawn position
     */
    @Nullable
    public BlockPos getBedSpawnPosition(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EntityPlayer player)
    {
        if (world instanceof World)
            return BlockBed.getSafeExitLocation((World)world, pos, 0);
        return null;
    }

    /**
     * Called when a user either starts or stops sleeping in the bed.
     *
     * @param world The current world
     * @param pos Block position in world
     * @param player The player or camera entity, null in some cases.
     * @param occupied True if we are occupying the bed, or false if they are stopping use of the bed
     */
    public void setBedOccupied(IBlockAccess world, BlockPos pos, EntityPlayer player, boolean occupied)
    {
        if (world instanceof World)
        {
            IBlockState state = world.getBlockState(pos);
            state = state.getBlock().getActualState(state, world, pos);
            state = state.withProperty(BlockBed.OCCUPIED, occupied);
            ((World)world).setBlockState(pos, state, 4);
        }
    }

    /**
     * Returns the direction of the block. Same values that
     * are returned by BlockDirectional
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @return Bed direction
     */
    public EnumFacing getBedDirection(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return (EnumFacing)getActualState(state, world, pos).getValue(BlockHorizontal.FACING);
    }

    /**
     * Determines if the current block is the foot half of the bed.
     *
     * @param world The current world
     * @param pos Block position in world
     * @return True if the current block is the foot side of a bed.
     */
    public boolean isBedFoot(IBlockAccess world, BlockPos pos)
    {
        return getActualState(world.getBlockState(pos), world, pos).getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT;
    }

    /**
     * Called when a leaf should start its decay process.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     */
    public void beginLeavesDecay(IBlockState state, World world, BlockPos pos){}

    /**
     * Determines if this block can prevent leaves connected to it from decaying.
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @return true if the presence this block can prevent leaves from decaying.
     */
    public boolean canSustainLeaves(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return false;
    }

    /**
     * Determines if this block is considered a leaf block, used to apply the leaf decay and generation system.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @return true if this block is considered leaves.
     */
    public boolean isLeaves(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return state.getMaterial() == Material.LEAVES;
    }

    /**
     * Used during tree growth to determine if newly generated leaves can replace this block.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @return true if this block can be replaced by growing leaves.
     */
    public boolean canBeReplacedByLeaves(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return isAir(state, world, pos) || isLeaves(state, world, pos); //!state.isFullBlock();
    }

    /**
     *
     * @param world The current world
     * @param pos Block position in world
     * @return  true if the block is wood (logs)
     */
    public boolean isWood(IBlockAccess world, BlockPos pos)
    {
        return false;
    }

    /**
     * Determines if the current block is replaceable by Ore veins during world generation.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @param target The generic target block the gen is looking for, Standards define stone
     *      for overworld generation, and neatherack for the nether.
     * @return True to allow this block to be replaced by a ore
     */
    public boolean isReplaceableOreGen(IBlockState state, IBlockAccess world, BlockPos pos, com.google.common.base.Predicate<IBlockState> target)
    {
        return target.apply(state);
    }

    /**
     * Location sensitive version of getExplosionResistance
     *
     * @param world The current world
     * @param pos Block position in world
     * @param exploder The entity that caused the explosion, can be null
     * @param explosion The explosion
     * @return The amount of the explosion absorbed.
     */
    public float getExplosionResistance(World world, BlockPos pos, @Nullable Entity exploder, Explosion explosion)
    {
        return getExplosionResistance(exploder);
    }

    /**
     * Called when the block is destroyed by an explosion.
     * Useful for allowing the block to take into account tile entities,
     * state, etc. when exploded, before it is removed.
     *
     * @param world The current world
     * @param pos Block position in world
     * @param explosion The explosion instance affecting the block
     */
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion)
    {
        world.setBlockToAir(pos);
        onExplosionDestroy(world, pos, explosion);
    }

    /**
     * Determine if this block can make a redstone connection on the side provided,
     * Useful to control which sides are inputs and outputs for redstone wires.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @param side The side that is trying to make the connection, CAN BE NULL
     * @return True to make the connection
     */
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EnumFacing side)
    {
        return state.canProvidePower() && side != null;
    }

    /**
     * Determines if a torch can be placed on the top surface of this block.
     * Useful for creating your own block that torches can be on, such as fences.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @return True to allow the torch to be placed
     */
    public boolean canPlaceTorchOnTop(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        if (this == Blocks.END_GATEWAY || this == Blocks.LIT_PUMPKIN)
        {
            return false;
        }
        else if (state.isTopSolid() || this instanceof BlockFence || this == Blocks.GLASS || this == Blocks.COBBLESTONE_WALL || this == Blocks.STAINED_GLASS)
        {
            return true;
        }
        else
        {
            BlockFaceShape shape = state.getBlockFaceShape(world, pos, EnumFacing.UP);
            return (shape == BlockFaceShape.SOLID || shape == BlockFaceShape.CENTER || shape == BlockFaceShape.CENTER_BIG) && !isExceptionBlockForAttaching(this);
        }
    }

    /**
     * Called when a user uses the creative pick block button on this block
     *
     * @param target The full target the player is looking at
     * @return A ItemStack to add to the player's inventory, empty itemstack if nothing should be added.
     */
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
    {
        return getItem(world, pos, state);
    }

    /**
     * Used by getTopSolidOrLiquidBlock while placing biome decorations, villages, etc
     * Also used to determine if the player can spawn on this block.
     *
     * @return False to disallow spawning
     */
    public boolean isFoliage(IBlockAccess world, BlockPos pos)
    {
        return false;
    }

    /**
     * Allows a block to override the standard EntityLivingBase.updateFallState
     * particles, this is a server side method that spawns particles with
     * WorldServer.spawnParticle
     *
     * @param world The current Server world
     * @param blockPosition of the block that the entity landed on.
     * @param iblockstate State at the specific world/pos
     * @param entity the entity that hit landed on the block.
     * @param numberOfParticles that vanilla would have spawned.
     * @return True to prevent vanilla landing particles form spawning.
     */
    public boolean addLandingEffects(IBlockState state, net.minecraft.world.WorldServer worldObj, BlockPos blockPosition, IBlockState iblockstate, EntityLivingBase entity, int numberOfParticles )
    {
        return false;
    }

    /**
     * Allows a block to override the standard vanilla running particles.
     * This is called from {@link Entity#spawnRunningParticles} and is called both,
     * Client and server side, it's up to the implementor to client check / server check.
     * By default vanilla spawns particles only on the client and the server methods no-op.
     *
     * @param state  The BlockState the entity is running on.
     * @param world  The world.
     * @param pos    The position at the entities feet.
     * @param entity The entity running on the block.
     * @return True to prevent vanilla running particles from spawning.
     */
    public boolean addRunningEffects(IBlockState state, World world, BlockPos pos, Entity entity)
    {
        return false;
    }

    /**
     * Spawn a digging particle effect in the world, this is a wrapper
     * around EffectRenderer.addBlockHitEffects to allow the block more
     * control over the particles. Useful when you have entirely different
     * texture sheets for different sides/locations in the world.
     *
     * @param state The current state
     * @param world The current world
     * @param target The target the player is looking at {x/y/z/side/sub}
     * @param manager A reference to the current particle manager.
     * @return True to prevent vanilla digging particles form spawning.
     */
    @SideOnly(Side.CLIENT)
    public boolean addHitEffects(IBlockState state, World worldObj, RayTraceResult target, net.minecraft.client.particle.ParticleManager manager)
    {
        return false;
    }

    /**
     * Spawn particles for when the block is destroyed. Due to the nature
     * of how this is invoked, the x/y/z locations are not always guaranteed
     * to host your block. So be sure to do proper sanity checks before assuming
     * that the location is this block.
     *
     * @param world The current world
     * @param pos Position to spawn the particle
     * @param manager A reference to the current particle manager.
     * @return True to prevent vanilla break particles from spawning.
     */
    @SideOnly(Side.CLIENT)
    public boolean addDestroyEffects(World world, BlockPos pos, net.minecraft.client.particle.ParticleManager manager)
    {
        return false;
    }

    /**
     * Determines if this block can support the passed in plant, allowing it to be planted and grow.
     * Some examples:
     *   Reeds check if its a reed, or if its sand/dirt/grass and adjacent to water
     *   Cacti checks if its a cacti, or if its sand
     *   Nether types check for soul sand
     *   Crops check for tilled soil
     *   Caves check if it's a solid surface
     *   Plains check if its grass or dirt
     *   Water check if its still water
     *
     * @param state The Current state
     * @param world The current world
     * @param pos Block position in world
     * @param direction The direction relative to the given position the plant wants to be, typically its UP
     * @param plantable The plant that wants to check
     * @return True to allow the plant to be planted/stay.
     */
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, net.minecraftforge.common.IPlantable plantable)
    {
        IBlockState plant = plantable.getPlant(world, pos.offset(direction));
        net.minecraftforge.common.EnumPlantType plantType = plantable.getPlantType(world, pos.offset(direction));

        if (plant.getBlock() == net.minecraft.init.Blocks.CACTUS)
        {
            return this == net.minecraft.init.Blocks.CACTUS || this == net.minecraft.init.Blocks.SAND;
        }

        if (plant.getBlock() == net.minecraft.init.Blocks.REEDS && this == net.minecraft.init.Blocks.REEDS)
        {
            return true;
        }

        if (plantable instanceof BlockBush && ((BlockBush)plantable).canSustainBush(state))
        {
            return true;
        }

        switch (plantType)
        {
            case Desert: return this == net.minecraft.init.Blocks.SAND || this == net.minecraft.init.Blocks.HARDENED_CLAY || this == net.minecraft.init.Blocks.STAINED_HARDENED_CLAY;
            case Nether: return this == net.minecraft.init.Blocks.SOUL_SAND;
            case Crop:   return this == net.minecraft.init.Blocks.FARMLAND;
            case Cave:   return state.isSideSolid(world, pos, EnumFacing.UP);
            case Plains: return this == net.minecraft.init.Blocks.GRASS || this == net.minecraft.init.Blocks.DIRT || this == net.minecraft.init.Blocks.FARMLAND;
            case Water:  return state.getMaterial() == Material.WATER && state.getValue(BlockLiquid.LEVEL) == 0;
            case Beach:
                boolean isBeach = this == net.minecraft.init.Blocks.GRASS || this == net.minecraft.init.Blocks.DIRT || this == net.minecraft.init.Blocks.SAND;
                boolean hasWater = (world.getBlockState(pos.east()).getMaterial() == Material.WATER ||
                                    world.getBlockState(pos.west()).getMaterial() == Material.WATER ||
                                    world.getBlockState(pos.north()).getMaterial() == Material.WATER ||
                                    world.getBlockState(pos.south()).getMaterial() == Material.WATER);
                return isBeach && hasWater;
        }

        return false;
    }

    /**
     * Called when a plant grows on this block, only implemented for saplings using the WorldGen*Trees classes right now.
     * Modder may implement this for custom plants.
     * This does not use ForgeDirection, because large/huge trees can be located in non-representable direction,
     * so the source location is specified.
     * Currently this just changes the block to dirt if it was grass.
     *
     * Note: This happens DURING the generation, the generation may not be complete when this is called.
     *
     * @param state The current state
     * @param world Current world
     * @param pos Block position in world
     * @param source Source plant's position in world
     */
    public void onPlantGrow(IBlockState state, World world, BlockPos pos, BlockPos source)
    {
        if (this == net.minecraft.init.Blocks.GRASS || this == net.minecraft.init.Blocks.FARMLAND)
        {
            world.setBlockState(pos, net.minecraft.init.Blocks.DIRT.getDefaultState(), 2);
        }
    }

    /**
     * Checks if this soil is fertile, typically this means that growth rates
     * of plants on this soil will be slightly sped up.
     * Only vanilla case is tilledField when it is within range of water.
     *
     * @param world The current world
     * @param pos Block position in world
     * @return True if the soil should be considered fertile.
     */
    public boolean isFertile(World world, BlockPos pos)
    {
        if (this == net.minecraft.init.Blocks.FARMLAND)
        {
            return ((Integer)world.getBlockState(pos).getValue(BlockFarmland.MOISTURE)) > 0;
        }

        return false;
    }

    /**
     * Location aware and overrideable version of the lightOpacity array,
     * return the number to subtract from the light value when it passes through this block.
     *
     * This is not guaranteed to have the tile entity in place before this is called, so it is
     * Recommended that you have your tile entity call relight after being placed if you
     * rely on it for light info.
     *
     * @param state The Block state
     * @param world The current world
     * @param pos Block position in world
     * @return The amount of light to block, 0 for air, 255 for fully opaque.
     */
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return state.getLightOpacity();
    }

    /**
     * Determines if this block is can be destroyed by the specified entities normal behavior.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @return True to allow the ender dragon to destroy this block
     */
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity)
    {
        if (entity instanceof net.minecraft.entity.boss.EntityDragon)
        {
            return this != net.minecraft.init.Blocks.BARRIER &&
                   this != net.minecraft.init.Blocks.OBSIDIAN &&
                   this != net.minecraft.init.Blocks.END_STONE &&
                   this != net.minecraft.init.Blocks.BEDROCK &&
                   this != net.minecraft.init.Blocks.END_PORTAL &&
                   this != net.minecraft.init.Blocks.END_PORTAL_FRAME &&
                   this != net.minecraft.init.Blocks.COMMAND_BLOCK &&
                   this != net.minecraft.init.Blocks.REPEATING_COMMAND_BLOCK &&
                   this != net.minecraft.init.Blocks.CHAIN_COMMAND_BLOCK &&
                   this != net.minecraft.init.Blocks.IRON_BARS &&
                   this != net.minecraft.init.Blocks.END_GATEWAY;
        }
        else if ((entity instanceof net.minecraft.entity.boss.EntityWither) ||
                 (entity instanceof net.minecraft.entity.projectile.EntityWitherSkull))
        {
            return net.minecraft.entity.boss.EntityWither.canDestroyBlock(this);
        }

        return true;
    }

    /**
     * Determines if this block can be used as the base of a beacon.
     *
     * @param world The current world
     * @param pos Block position in world
     * @param beacon Beacon position in world
     * @return True, to support the beacon, and make it active with this block.
     */
    public boolean isBeaconBase(IBlockAccess worldObj, BlockPos pos, BlockPos beacon)
    {
        return this == net.minecraft.init.Blocks.EMERALD_BLOCK || this == net.minecraft.init.Blocks.GOLD_BLOCK || this == net.minecraft.init.Blocks.DIAMOND_BLOCK || this == net.minecraft.init.Blocks.IRON_BLOCK;
    }

    /**
     * Rotate the block. For vanilla blocks this rotates around the axis passed in (generally, it should be the "face" that was hit).
     * Note: for mod blocks, this is up to the block and modder to decide. It is not mandated that it be a rotation around the
     * face, but could be a rotation to orient *to* that face, or a visiting of possible rotations.
     * The method should return true if the rotation was successful though.
     *
     * @param world The world
     * @param pos Block position in world
     * @param axis The axis to rotate around
     * @return True if the rotation was successful, False if the rotation failed, or is not possible
     */
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis)
    {
        IBlockState state = world.getBlockState(pos);
        for (IProperty<?> prop : state.getProperties().keySet())
        {
            if ((prop.getName().equals("facing") || prop.getName().equals("rotation")) && prop.getValueClass() == EnumFacing.class)
            {
                Block block = state.getBlock();
                if (!(block instanceof BlockBed) && !(block instanceof BlockPistonExtension))
                {
                    IBlockState newState;
                    //noinspection unchecked
                    IProperty<EnumFacing> facingProperty = (IProperty<EnumFacing>) prop;
                    EnumFacing facing = state.getValue(facingProperty);
                    java.util.Collection<EnumFacing> validFacings = facingProperty.getAllowedValues();

                    // rotate horizontal facings clockwise
                    if (validFacings.size() == 4 && !validFacings.contains(EnumFacing.UP) && !validFacings.contains(EnumFacing.DOWN))
                    {
                        newState = state.withProperty(facingProperty, facing.rotateY());
                    }
                    else
                    {
                        // rotate other facings about the axis
                        EnumFacing rotatedFacing = facing.rotateAround(axis.getAxis());
                        if (validFacings.contains(rotatedFacing))
                        {
                            newState = state.withProperty(facingProperty, rotatedFacing);
                        }
                        else // abnormal facing property, just cycle it
                        {
                            newState = state.cycleProperty(facingProperty);
                        }
                    }

                    world.setBlockState(pos, newState);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the rotations that can apply to the block at the specified coordinates. Null means no rotations are possible.
     * Note, this is up to the block to decide. It may not be accurate or representative.
     * @param world The world
     * @param pos Block position in world
     * @return An array of valid axes to rotate around, or null for none or unknown
     */
    @Nullable
    public EnumFacing[] getValidRotations(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        for (IProperty<?> prop : state.getProperties().keySet())
        {
            if ((prop.getName().equals("facing") || prop.getName().equals("rotation")) && prop.getValueClass() == EnumFacing.class)
            {
                @SuppressWarnings("unchecked")
                java.util.Collection<EnumFacing> values = ((java.util.Collection<EnumFacing>)prop.getAllowedValues());
                return values.toArray(new EnumFacing[values.size()]);
            }
        }
        return null;
    }

    /**
     * Determines the amount of enchanting power this block can provide to an enchanting table.
     * @param world The World
     * @param pos Block position in world
     * @return The amount of enchanting power this block produces.
     */
    public float getEnchantPowerBonus(World world, BlockPos pos)
    {
        return this == net.minecraft.init.Blocks.BOOKSHELF ? 1 : 0;
    }

    /**
     * Common way to recolor a block with an external tool
     * @param world The world
     * @param pos Block position in world
     * @param side The side hit with the coloring tool
     * @param color The color to change to
     * @return If the recoloring was successful
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean recolorBlock(World world, BlockPos pos, EnumFacing side, net.minecraft.item.EnumDyeColor color)
    {
        IBlockState state = world.getBlockState(pos);
        for (IProperty prop : state.getProperties().keySet())
        {
            if (prop.getName().equals("color") && prop.getValueClass() == net.minecraft.item.EnumDyeColor.class)
            {
                net.minecraft.item.EnumDyeColor current = (net.minecraft.item.EnumDyeColor)state.getValue(prop);
                if (current != color && prop.getAllowedValues().contains(color))
                {
                    world.setBlockState(pos, state.withProperty(prop, color));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gathers how much experience this block drops when broken.
     *
     * @param state The current state
     * @param world The world
     * @param pos Block position
     * @param fortune
     * @return Amount of XP from breaking this block.
     */
    public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune)
    {
        return 0;
    }

    /**
     * Called when a tile entity on a side of this block changes is created or is destroyed.
     * @param world The world
     * @param pos Block position in world
     * @param neighbor Block position of neighbor
     */
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor){}

    /**
     * Called on an Observer block whenever an update for an Observer is received.
     *
     * @param observerState The Observer block's state.
     * @param world The current world.
     * @param observerPos The Observer block's position.
     * @param changedBlock The updated block.
     * @param changedBlockPos The updated block's position.
     */
    public void observedNeighborChange(IBlockState observerState, World world, BlockPos observerPos, Block changedBlock, BlockPos changedBlockPos){}

    /**
     * Called to determine whether to allow the a block to handle its own indirect power rather than using the default rules.
     * @param world The world
     * @param pos Block position in world
     * @param side The INPUT side of the block to be powered - ie the opposite of this block's output side
     * @return Whether Block#isProvidingWeakPower should be called when determining indirect power
     */
    public boolean shouldCheckWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
    {
        return state.isNormalCube();
    }

    /**
     * If this block should be notified of weak changes.
     * Weak changes are changes 1 block away through a solid block.
     * Similar to comparators.
     *
     * @param world The current world
     * @param pos Block position in world
     * @return true To be notified of changes
     */
    public boolean getWeakChanges(IBlockAccess world, BlockPos pos)
    {
        return false;
    }

    private String[] harvestTool = new String[16];;
    private int[] harvestLevel = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    /**
     * Sets or removes the tool and level required to harvest this block.
     *
     * @param toolClass Class
     * @param level Harvest level:
     *     Wood:    0
     *     Stone:   1
     *     Iron:    2
     *     Diamond: 3
     *     Gold:    0
     */
    public void setHarvestLevel(String toolClass, int level)
    {
        java.util.Iterator<IBlockState> itr = getBlockState().getValidStates().iterator();
        while (itr.hasNext())
        {
            setHarvestLevel(toolClass, level, itr.next());
        }
    }

    /**
     * Sets or removes the tool and level required to harvest this block.
     *
     * @param toolClass Class
     * @param level Harvest level:
     *     Wood:    0
     *     Stone:   1
     *     Iron:    2
     *     Diamond: 3
     *     Gold:    0
     * @param state The specific state.
     */
    public void setHarvestLevel(String toolClass, int level, IBlockState state)
    {
        int idx = this.getMetaFromState(state);
        this.harvestTool[idx] = toolClass;
        this.harvestLevel[idx] = level;
    }

    /**
     * Queries the class of tool required to harvest this block, if null is returned
     * we assume that anything can harvest this block.
     */
    @Nullable public String getHarvestTool(IBlockState state)
    {
        return harvestTool[getMetaFromState(state)];
    }

    /**
     * Queries the harvest level of this item stack for the specified tool class,
     * Returns -1 if this tool is not of the specified type
     *
     * @return Harvest level, or -1 if not the specified tool type.
     */
    public int getHarvestLevel(IBlockState state)
    {
        return harvestLevel[getMetaFromState(state)];
    }

    /**
     * Checks if the specified tool type is efficient on this block,
     * meaning that it digs at full speed.
     */
    public boolean isToolEffective(String type, IBlockState state)
    {
        if ("pickaxe".equals(type) && (this == net.minecraft.init.Blocks.REDSTONE_ORE || this == net.minecraft.init.Blocks.LIT_REDSTONE_ORE || this == net.minecraft.init.Blocks.OBSIDIAN))
            return false;
        return type != null && type.equals(getHarvestTool(state));
    }

    /**
     * Can return IExtendedBlockState
     */
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return state;
    }

    /**
      * Called when the entity is inside this block, may be used to determined if the entity can breathing,
      * display material overlays, or if the entity can swim inside a block.
      *
      * @param world that is being tested.
      * @param blockpos position thats being tested.
      * @param iblockstate state at world/blockpos
      * @param entity that is being tested.
      * @param yToTest, primarily for testingHead, which sends the the eye level of the entity, other wise it sends a y that can be tested vs liquid height.
      * @param materialIn to test for.
      * @param testingHead when true, its testing the entities head for vision, breathing ect... otherwise its testing the body, for swimming and movement adjustment.
      * @return null for default behavior, true if the entity is within the material, false if it was not.
      */
    @Nullable
    public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos blockpos, IBlockState iblockstate, Entity entity, double yToTest, Material materialIn, boolean testingHead)
    {
        return null;
    }

     /**
      * Called when boats or fishing hooks are inside the block to check if they are inside
      * the material requested.
      *
      * @param world world that is being tested.
      * @param pos block thats being tested.
      * @param boundingBox box to test, generally the bounds of an entity that are besting tested.
      * @param materialIn to check for.
      * @return null for default behavior, true if the box is within the material, false if it was not.
      */
     @Nullable
     public Boolean isAABBInsideMaterial(World world, BlockPos pos, AxisAlignedBB boundingBox, Material materialIn)
     {
         return null;
     }
     
     /**
      * Called when entities are moving to check if they are inside a liquid
      *
      * @param world world that is being tested.
      * @param pos block thats being tested.
      * @param boundingBox box to test, generally the bounds of an entity that are besting tested.
      * @return null for default behavior, true if the box is within the material, false if it was not.
      */
     @Nullable
     public Boolean isAABBInsideLiquid(World world, BlockPos pos, AxisAlignedBB boundingBox)
     {
         return null;
     }
     
     /**
      * Called when entities are swimming in the given liquid and returns the relative height (used by {@link net.minecraft.entity.item.EntityBoat})
      * 
      * @param world world that is being tested.
      * @param pos block thats being tested.
      * @param state state at world/pos
      * @param material liquid thats being tested.
      * @return relative height of the given liquid (material), a value between 0 and 1
      */
     public float getBlockLiquidHeight(World world, BlockPos pos, IBlockState state, Material material)
     {
         return 0;
     }

     /**
     * Queries if this block should render in a given layer.
     * ISmartBlockModel can use {@link net.minecraftforge.client.MinecraftForgeClient#getRenderLayer()} to alter their model based on layer.
     */
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer)
    {
        return getRenderLayer() == layer;
    }
    // For Internal use only to capture droped items inside getDrops
    protected static ThreadLocal<Boolean> captureDrops = ThreadLocal.withInitial(() -> false);
    protected static ThreadLocal<NonNullList<ItemStack>> capturedDrops = ThreadLocal.withInitial(NonNullList::create);
    protected NonNullList<ItemStack> captureDrops(boolean start)
    {
        if (start)
        {
            captureDrops.set(true);
            capturedDrops.get().clear();
            return NonNullList.create();
        }
        else
        {
            captureDrops.set(false);
            return capturedDrops.get();
        }
    }

    /**
     * Sensitive version of getSoundType
     * @param state The state
     * @param world The world
     * @param pos The position. Note that the world may not necessarily have {@code state} here!
     * @param entity The entity that is breaking/stepping on/placing/hitting/falling on this block, or null if no entity is in this context
     * @return A SoundType to use
     */
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity)
    {
        return getSoundType();
    }

    /**
     * @param state The state
     * @param world The world
     * @param pos The position of this state
     * @param beaconPos The position of the beacon
     * @return A float RGB [0.0, 1.0] array to be averaged with a beacon's existing beam color, or null to do nothing to the beam
     */
    @Nullable
    public float[] getBeaconColorMultiplier(IBlockState state, World world, BlockPos pos, BlockPos beaconPos)
    {
        return null;
    }

    /**
     * Use this to change the fog color used when the entity is "inside" a material.
     * Vec3d is used here as "r/g/b" 0 - 1 values.
     *
     * @param world         The world.
     * @param pos           The position at the entity viewport.
     * @param state         The state at the entity viewport.
     * @param entity        the entity
     * @param originalColor The current fog color, You are not expected to use this, Return as the default if applicable.
     * @return The new fog color.
     */
    @SideOnly (Side.CLIENT)
    public Vec3d getFogColor(World world, BlockPos pos, IBlockState state, Entity entity, Vec3d originalColor, float partialTicks)
    {
        if (state.getMaterial() == Material.WATER)
        {
            float f12 = 0.0F;

            if (entity instanceof net.minecraft.entity.EntityLivingBase)
            {
                net.minecraft.entity.EntityLivingBase ent = (net.minecraft.entity.EntityLivingBase)entity;
                f12 = (float)net.minecraft.enchantment.EnchantmentHelper.getRespirationModifier(ent) * 0.2F;

                if (ent.isPotionActive(net.minecraft.init.MobEffects.WATER_BREATHING))
                {
                    f12 = f12 * 0.3F + 0.6F;
                }
            }
            return new Vec3d(0.02F + f12, 0.02F + f12, 0.2F + f12);
        }
        else if (state.getMaterial() == Material.LAVA)
        {
            return new Vec3d(0.6F, 0.1F, 0.0F);
        }
        return originalColor;
    }

    /**
     * Used to determine the state 'viewed' by an entity (see
     * {@link net.minecraft.client.renderer.ActiveRenderInfo#getBlockStateAtEntityViewpoint(World, Entity, float)}).
     * Can be used by fluid blocks to determine if the viewpoint is within the fluid or not.
     *
     * @param state     the state
     * @param world     the world
     * @param pos       the position
     * @param viewpoint the viewpoint
     * @return the block state that should be 'seen'
     */
    public IBlockState getStateAtViewpoint(IBlockState state, IBlockAccess world, BlockPos pos, Vec3d viewpoint)
    {
        return state;
    }

    /**
     * Gets the {@link IBlockState} to place
     * @param world The world the block is being placed in
     * @param pos The position the block is being placed at
     * @param facing The side the block is being placed on
     * @param hitX The X coordinate of the hit vector
     * @param hitY The Y coordinate of the hit vector
     * @param hitZ The Z coordinate of the hit vector
     * @param meta The metadata of {@link ItemStack} as processed by {@link Item#getMetadata(int)}
     * @param placer The entity placing the block
     * @param hand The player hand used to place this block
     * @return The state to be placed in the world
     */
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
    {
        return getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer);
    }

    /**
     * Determines if another block can connect to this block
     *
     * @param world The current world
     * @param pos The position of this block
     * @param facing The side the connecting block is on
     * @return True to allow another block to connect to this block
     */
    public boolean canBeConnectedTo(IBlockAccess world, BlockPos pos, EnumFacing facing)
    {
        return false;
    }

    /** @deprecated use {@link #getAiPathNodeType(IBlockState, IBlockAccess, BlockPos, net.minecraft.entity.EntityLiving)} */
    @Nullable
    @Deprecated // TODO: remove
    public net.minecraft.pathfinding.PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return isBurning(world, pos) ? net.minecraft.pathfinding.PathNodeType.DAMAGE_FIRE : null;
    }

    /**
     * Get the {@code PathNodeType} for this block. Return {@code null} for vanilla behavior.
     *
     * @return the PathNodeType
     */
    @Nullable
    public net.minecraft.pathfinding.PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable net.minecraft.entity.EntityLiving entity)
    {
        return getAiPathNodeType(state, world, pos);
    }

    /**
     * @param blockState The state for this block
     * @param world The world this block is in
     * @param pos The position of this block
     * @param side The side of this block that the chest lid is trying to open into
     * @return true if the chest should be prevented from opening by this block
     */
    public boolean doesSideBlockChestOpening(IBlockState blockState, IBlockAccess world, BlockPos pos, EnumFacing side)
    {
        ResourceLocation registryName = this.getRegistryName();
        if (registryName != null && "minecraft".equals(registryName.getNamespace()))
        {
            // maintain the vanilla behavior of https://bugs.mojang.com/browse/MC-378
            return isNormalCube(blockState, world, pos);
        }
        return isSideSolid(blockState, world, pos, side);
    }

    /**
     * @param state The state
     * @return true if the block is sticky block which used for pull or push adjacent blocks (use by piston)
     */
    public boolean isStickyBlock(IBlockState state)
    {
        return state.getBlock() == Blocks.SLIME_BLOCK;
    }

    /* ========================================= FORGE END ======================================*/

    public static void registerBlocks()
    {
        registerBlock(0, AIR_ID, (new BlockAir()).setTranslationKey("air"));
        registerBlock(1, "stone", (new BlockStone()).setHardness(1.5F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("stone"));
        registerBlock(2, "grass", (new BlockGrass()).setHardness(0.6F).setSoundType(SoundType.PLANT).setTranslationKey("grass"));
        registerBlock(3, "dirt", (new BlockDirt()).setHardness(0.5F).setSoundType(SoundType.GROUND).setTranslationKey("dirt"));
        Block block = (new Block(Material.ROCK)).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("stonebrick").setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        registerBlock(4, "cobblestone", block);
        Block block1 = (new BlockPlanks()).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("wood");
        registerBlock(5, "planks", block1);
        registerBlock(6, "sapling", (new BlockSapling()).setHardness(0.0F).setSoundType(SoundType.PLANT).setTranslationKey("sapling"));
        registerBlock(7, "bedrock", (new BlockEmptyDrops(Material.ROCK)).setBlockUnbreakable().setResistance(6000000.0F).setSoundType(SoundType.STONE).setTranslationKey("bedrock").disableStats().setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(8, "flowing_water", (new BlockDynamicLiquid(Material.WATER)).setHardness(100.0F).setLightOpacity(3).setTranslationKey("water").disableStats());
        registerBlock(9, "water", (new BlockStaticLiquid(Material.WATER)).setHardness(100.0F).setLightOpacity(3).setTranslationKey("water").disableStats());
        registerBlock(10, "flowing_lava", (new BlockDynamicLiquid(Material.LAVA)).setHardness(100.0F).setLightLevel(1.0F).setTranslationKey("lava").disableStats());
        registerBlock(11, "lava", (new BlockStaticLiquid(Material.LAVA)).setHardness(100.0F).setLightLevel(1.0F).setTranslationKey("lava").disableStats());
        registerBlock(12, "sand", (new BlockSand()).setHardness(0.5F).setSoundType(SoundType.SAND).setTranslationKey("sand"));
        registerBlock(13, "gravel", (new BlockGravel()).setHardness(0.6F).setSoundType(SoundType.GROUND).setTranslationKey("gravel"));
        registerBlock(14, "gold_ore", (new BlockOre()).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("oreGold"));
        registerBlock(15, "iron_ore", (new BlockOre()).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("oreIron"));
        registerBlock(16, "coal_ore", (new BlockOre()).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("oreCoal"));
        registerBlock(17, "log", (new BlockOldLog()).setTranslationKey("log"));
        registerBlock(18, "leaves", (new BlockOldLeaf()).setTranslationKey("leaves"));
        registerBlock(19, "sponge", (new BlockSponge()).setHardness(0.6F).setSoundType(SoundType.PLANT).setTranslationKey("sponge"));
        registerBlock(20, "glass", (new BlockGlass(Material.GLASS, false)).setHardness(0.3F).setSoundType(SoundType.GLASS).setTranslationKey("glass"));
        registerBlock(21, "lapis_ore", (new BlockOre()).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("oreLapis"));
        registerBlock(22, "lapis_block", (new Block(Material.IRON, MapColor.LAPIS)).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("blockLapis").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(23, "dispenser", (new BlockDispenser()).setHardness(3.5F).setSoundType(SoundType.STONE).setTranslationKey("dispenser"));
        Block block2 = (new BlockSandStone()).setSoundType(SoundType.STONE).setHardness(0.8F).setTranslationKey("sandStone");
        registerBlock(24, "sandstone", block2);
        registerBlock(25, "noteblock", (new BlockNote()).setSoundType(SoundType.WOOD).setHardness(0.8F).setTranslationKey("musicBlock"));
        registerBlock(26, "bed", (new BlockBed()).setSoundType(SoundType.WOOD).setHardness(0.2F).setTranslationKey("bed").disableStats());
        registerBlock(27, "golden_rail", (new BlockRailPowered()).setHardness(0.7F).setSoundType(SoundType.METAL).setTranslationKey("goldenRail"));
        registerBlock(28, "detector_rail", (new BlockRailDetector()).setHardness(0.7F).setSoundType(SoundType.METAL).setTranslationKey("detectorRail"));
        registerBlock(29, "sticky_piston", (new BlockPistonBase(true)).setTranslationKey("pistonStickyBase"));
        registerBlock(30, "web", (new BlockWeb()).setLightOpacity(1).setHardness(4.0F).setTranslationKey("web"));
        registerBlock(31, "tallgrass", (new BlockTallGrass()).setHardness(0.0F).setSoundType(SoundType.PLANT).setTranslationKey("tallgrass"));
        registerBlock(32, "deadbush", (new BlockDeadBush()).setHardness(0.0F).setSoundType(SoundType.PLANT).setTranslationKey("deadbush"));
        registerBlock(33, "piston", (new BlockPistonBase(false)).setTranslationKey("pistonBase"));
        registerBlock(34, "piston_head", (new BlockPistonExtension()).setTranslationKey("pistonBase"));
        registerBlock(35, "wool", (new BlockColored(Material.CLOTH)).setHardness(0.8F).setSoundType(SoundType.CLOTH).setTranslationKey("cloth"));
        registerBlock(36, "piston_extension", new BlockPistonMoving());
        registerBlock(37, "yellow_flower", (new BlockYellowFlower()).setHardness(0.0F).setSoundType(SoundType.PLANT).setTranslationKey("flower1"));
        registerBlock(38, "red_flower", (new BlockRedFlower()).setHardness(0.0F).setSoundType(SoundType.PLANT).setTranslationKey("flower2"));
        Block block3 = (new BlockMushroom()).setHardness(0.0F).setSoundType(SoundType.PLANT).setLightLevel(0.125F).setTranslationKey("mushroom");
        registerBlock(39, "brown_mushroom", block3);
        Block block4 = (new BlockMushroom()).setHardness(0.0F).setSoundType(SoundType.PLANT).setTranslationKey("mushroom");
        registerBlock(40, "red_mushroom", block4);
        registerBlock(41, "gold_block", (new Block(Material.IRON, MapColor.GOLD)).setHardness(3.0F).setResistance(10.0F).setSoundType(SoundType.METAL).setTranslationKey("blockGold").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(42, "iron_block", (new Block(Material.IRON, MapColor.IRON)).setHardness(5.0F).setResistance(10.0F).setSoundType(SoundType.METAL).setTranslationKey("blockIron").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(43, "double_stone_slab", (new BlockDoubleStoneSlab()).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("stoneSlab"));
        registerBlock(44, "stone_slab", (new BlockHalfStoneSlab()).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("stoneSlab"));
        Block block5 = (new Block(Material.ROCK, MapColor.RED)).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("brick").setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        registerBlock(45, "brick_block", block5);
        registerBlock(46, "tnt", (new BlockTNT()).setHardness(0.0F).setSoundType(SoundType.PLANT).setTranslationKey("tnt"));
        registerBlock(47, "bookshelf", (new BlockBookshelf()).setHardness(1.5F).setSoundType(SoundType.WOOD).setTranslationKey("bookshelf"));
        registerBlock(48, "mossy_cobblestone", (new Block(Material.ROCK)).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("stoneMoss").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(49, "obsidian", (new BlockObsidian()).setHardness(50.0F).setResistance(2000.0F).setSoundType(SoundType.STONE).setTranslationKey("obsidian"));
        registerBlock(50, "torch", (new BlockTorch()).setHardness(0.0F).setLightLevel(0.9375F).setSoundType(SoundType.WOOD).setTranslationKey("torch"));
        registerBlock(51, "fire", (new BlockFire()).setHardness(0.0F).setLightLevel(1.0F).setSoundType(SoundType.CLOTH).setTranslationKey("fire").disableStats());
        registerBlock(52, "mob_spawner", (new BlockMobSpawner()).setHardness(5.0F).setSoundType(SoundType.METAL).setTranslationKey("mobSpawner").disableStats());
        registerBlock(53, "oak_stairs", (new BlockStairs(block1.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.OAK))).setTranslationKey("stairsWood"));
        registerBlock(54, "chest", (new BlockChest(BlockChest.Type.BASIC)).setHardness(2.5F).setSoundType(SoundType.WOOD).setTranslationKey("chest"));
        registerBlock(55, "redstone_wire", (new BlockRedstoneWire()).setHardness(0.0F).setSoundType(SoundType.STONE).setTranslationKey("redstoneDust").disableStats());
        registerBlock(56, "diamond_ore", (new BlockOre()).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("oreDiamond"));
        registerBlock(57, "diamond_block", (new Block(Material.IRON, MapColor.DIAMOND)).setHardness(5.0F).setResistance(10.0F).setSoundType(SoundType.METAL).setTranslationKey("blockDiamond").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(58, "crafting_table", (new BlockWorkbench()).setHardness(2.5F).setSoundType(SoundType.WOOD).setTranslationKey("workbench"));
        registerBlock(59, "wheat", (new BlockCrops()).setTranslationKey("crops"));
        Block block6 = (new BlockFarmland()).setHardness(0.6F).setSoundType(SoundType.GROUND).setTranslationKey("farmland");
        registerBlock(60, "farmland", block6);
        registerBlock(61, "furnace", (new BlockFurnace(false)).setHardness(3.5F).setSoundType(SoundType.STONE).setTranslationKey("furnace").setCreativeTab(CreativeTabs.DECORATIONS));
        registerBlock(62, "lit_furnace", (new BlockFurnace(true)).setHardness(3.5F).setSoundType(SoundType.STONE).setLightLevel(0.875F).setTranslationKey("furnace"));
        registerBlock(63, "standing_sign", (new BlockStandingSign()).setHardness(1.0F).setSoundType(SoundType.WOOD).setTranslationKey("sign").disableStats());
        registerBlock(64, "wooden_door", (new BlockDoor(Material.WOOD)).setHardness(3.0F).setSoundType(SoundType.WOOD).setTranslationKey("doorOak").disableStats());
        registerBlock(65, "ladder", (new BlockLadder()).setHardness(0.4F).setSoundType(SoundType.LADDER).setTranslationKey("ladder"));
        registerBlock(66, "rail", (new BlockRail()).setHardness(0.7F).setSoundType(SoundType.METAL).setTranslationKey("rail"));
        registerBlock(67, "stone_stairs", (new BlockStairs(block.getDefaultState())).setTranslationKey("stairsStone"));
        registerBlock(68, "wall_sign", (new BlockWallSign()).setHardness(1.0F).setSoundType(SoundType.WOOD).setTranslationKey("sign").disableStats());
        registerBlock(69, "lever", (new BlockLever()).setHardness(0.5F).setSoundType(SoundType.WOOD).setTranslationKey("lever"));
        registerBlock(70, "stone_pressure_plate", (new BlockPressurePlate(Material.ROCK, BlockPressurePlate.Sensitivity.MOBS)).setHardness(0.5F).setSoundType(SoundType.STONE).setTranslationKey("pressurePlateStone"));
        registerBlock(71, "iron_door", (new BlockDoor(Material.IRON)).setHardness(5.0F).setSoundType(SoundType.METAL).setTranslationKey("doorIron").disableStats());
        registerBlock(72, "wooden_pressure_plate", (new BlockPressurePlate(Material.WOOD, BlockPressurePlate.Sensitivity.EVERYTHING)).setHardness(0.5F).setSoundType(SoundType.WOOD).setTranslationKey("pressurePlateWood"));
        registerBlock(73, "redstone_ore", (new BlockRedstoneOre(false)).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("oreRedstone").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(74, "lit_redstone_ore", (new BlockRedstoneOre(true)).setLightLevel(0.625F).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("oreRedstone"));
        registerBlock(75, "unlit_redstone_torch", (new BlockRedstoneTorch(false)).setHardness(0.0F).setSoundType(SoundType.WOOD).setTranslationKey("notGate"));
        registerBlock(76, "redstone_torch", (new BlockRedstoneTorch(true)).setHardness(0.0F).setLightLevel(0.5F).setSoundType(SoundType.WOOD).setTranslationKey("notGate").setCreativeTab(CreativeTabs.REDSTONE));
        registerBlock(77, "stone_button", (new BlockButtonStone()).setHardness(0.5F).setSoundType(SoundType.STONE).setTranslationKey("button"));
        registerBlock(78, "snow_layer", (new BlockSnow()).setHardness(0.1F).setSoundType(SoundType.SNOW).setTranslationKey("snow").setLightOpacity(0));
        registerBlock(79, "ice", (new BlockIce()).setHardness(0.5F).setLightOpacity(3).setSoundType(SoundType.GLASS).setTranslationKey("ice"));
        registerBlock(80, "snow", (new BlockSnowBlock()).setHardness(0.2F).setSoundType(SoundType.SNOW).setTranslationKey("snow"));
        registerBlock(81, "cactus", (new BlockCactus()).setHardness(0.4F).setSoundType(SoundType.CLOTH).setTranslationKey("cactus"));
        registerBlock(82, "clay", (new BlockClay()).setHardness(0.6F).setSoundType(SoundType.GROUND).setTranslationKey("clay"));
        registerBlock(83, "reeds", (new BlockReed()).setHardness(0.0F).setSoundType(SoundType.PLANT).setTranslationKey("reeds").disableStats());
        registerBlock(84, "jukebox", (new BlockJukebox()).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("jukebox"));
        registerBlock(85, "fence", (new BlockFence(Material.WOOD, BlockPlanks.EnumType.OAK.getMapColor())).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("fence"));
        Block block7 = (new BlockPumpkin()).setHardness(1.0F).setSoundType(SoundType.WOOD).setTranslationKey("pumpkin");
        registerBlock(86, "pumpkin", block7);
        registerBlock(87, "netherrack", (new BlockNetherrack()).setHardness(0.4F).setSoundType(SoundType.STONE).setTranslationKey("hellrock"));
        registerBlock(88, "soul_sand", (new BlockSoulSand()).setHardness(0.5F).setSoundType(SoundType.SAND).setTranslationKey("hellsand"));
        registerBlock(89, "glowstone", (new BlockGlowstone(Material.GLASS)).setHardness(0.3F).setSoundType(SoundType.GLASS).setLightLevel(1.0F).setTranslationKey("lightgem"));
        registerBlock(90, "portal", (new BlockPortal()).setHardness(-1.0F).setSoundType(SoundType.GLASS).setLightLevel(0.75F).setTranslationKey("portal"));
        registerBlock(91, "lit_pumpkin", (new BlockPumpkin()).setHardness(1.0F).setSoundType(SoundType.WOOD).setLightLevel(1.0F).setTranslationKey("litpumpkin"));
        registerBlock(92, "cake", (new BlockCake()).setHardness(0.5F).setSoundType(SoundType.CLOTH).setTranslationKey("cake").disableStats());
        registerBlock(93, "unpowered_repeater", (new BlockRedstoneRepeater(false)).setHardness(0.0F).setSoundType(SoundType.WOOD).setTranslationKey("diode").disableStats());
        registerBlock(94, "powered_repeater", (new BlockRedstoneRepeater(true)).setHardness(0.0F).setSoundType(SoundType.WOOD).setTranslationKey("diode").disableStats());
        registerBlock(95, "stained_glass", (new BlockStainedGlass(Material.GLASS)).setHardness(0.3F).setSoundType(SoundType.GLASS).setTranslationKey("stainedGlass"));
        registerBlock(96, "trapdoor", (new BlockTrapDoor(Material.WOOD)).setHardness(3.0F).setSoundType(SoundType.WOOD).setTranslationKey("trapdoor").disableStats());
        registerBlock(97, "monster_egg", (new BlockSilverfish()).setHardness(0.75F).setTranslationKey("monsterStoneEgg"));
        Block block8 = (new BlockStoneBrick()).setHardness(1.5F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("stonebricksmooth");
        registerBlock(98, "stonebrick", block8);
        registerBlock(99, "brown_mushroom_block", (new BlockHugeMushroom(Material.WOOD, MapColor.DIRT, block3)).setHardness(0.2F).setSoundType(SoundType.WOOD).setTranslationKey("mushroom"));
        registerBlock(100, "red_mushroom_block", (new BlockHugeMushroom(Material.WOOD, MapColor.RED, block4)).setHardness(0.2F).setSoundType(SoundType.WOOD).setTranslationKey("mushroom"));
        registerBlock(101, "iron_bars", (new BlockPane(Material.IRON, true)).setHardness(5.0F).setResistance(10.0F).setSoundType(SoundType.METAL).setTranslationKey("fenceIron"));
        registerBlock(102, "glass_pane", (new BlockPane(Material.GLASS, false)).setHardness(0.3F).setSoundType(SoundType.GLASS).setTranslationKey("thinGlass"));
        Block block9 = (new BlockMelon()).setHardness(1.0F).setSoundType(SoundType.WOOD).setTranslationKey("melon");
        registerBlock(103, "melon_block", block9);
        registerBlock(104, "pumpkin_stem", (new BlockStem(block7)).setHardness(0.0F).setSoundType(SoundType.WOOD).setTranslationKey("pumpkinStem"));
        registerBlock(105, "melon_stem", (new BlockStem(block9)).setHardness(0.0F).setSoundType(SoundType.WOOD).setTranslationKey("pumpkinStem"));
        registerBlock(106, "vine", (new BlockVine()).setHardness(0.2F).setSoundType(SoundType.PLANT).setTranslationKey("vine"));
        registerBlock(107, "fence_gate", (new BlockFenceGate(BlockPlanks.EnumType.OAK)).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("fenceGate"));
        registerBlock(108, "brick_stairs", (new BlockStairs(block5.getDefaultState())).setTranslationKey("stairsBrick"));
        registerBlock(109, "stone_brick_stairs", (new BlockStairs(block8.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.DEFAULT))).setTranslationKey("stairsStoneBrickSmooth"));
        registerBlock(110, "mycelium", (new BlockMycelium()).setHardness(0.6F).setSoundType(SoundType.PLANT).setTranslationKey("mycel"));
        registerBlock(111, "waterlily", (new BlockLilyPad()).setHardness(0.0F).setSoundType(SoundType.PLANT).setTranslationKey("waterlily"));
        Block block10 = (new BlockNetherBrick()).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("netherBrick").setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        registerBlock(112, "nether_brick", block10);
        registerBlock(113, "nether_brick_fence", (new BlockFence(Material.ROCK, MapColor.NETHERRACK)).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("netherFence"));
        registerBlock(114, "nether_brick_stairs", (new BlockStairs(block10.getDefaultState())).setTranslationKey("stairsNetherBrick"));
        registerBlock(115, "nether_wart", (new BlockNetherWart()).setTranslationKey("netherStalk"));
        registerBlock(116, "enchanting_table", (new BlockEnchantmentTable()).setHardness(5.0F).setResistance(2000.0F).setTranslationKey("enchantmentTable"));
        registerBlock(117, "brewing_stand", (new BlockBrewingStand()).setHardness(0.5F).setLightLevel(0.125F).setTranslationKey("brewingStand"));
        registerBlock(118, "cauldron", (new BlockCauldron()).setHardness(2.0F).setTranslationKey("cauldron"));
        registerBlock(119, "end_portal", (new BlockEndPortal(Material.PORTAL)).setHardness(-1.0F).setResistance(6000000.0F));
        registerBlock(120, "end_portal_frame", (new BlockEndPortalFrame()).setSoundType(SoundType.GLASS).setLightLevel(0.125F).setHardness(-1.0F).setTranslationKey("endPortalFrame").setResistance(6000000.0F).setCreativeTab(CreativeTabs.DECORATIONS));
        registerBlock(121, "end_stone", (new Block(Material.ROCK, MapColor.SAND)).setHardness(3.0F).setResistance(15.0F).setSoundType(SoundType.STONE).setTranslationKey("whiteStone").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(122, "dragon_egg", (new BlockDragonEgg()).setHardness(3.0F).setResistance(15.0F).setSoundType(SoundType.STONE).setLightLevel(0.125F).setTranslationKey("dragonEgg"));
        registerBlock(123, "redstone_lamp", (new BlockRedstoneLight(false)).setHardness(0.3F).setSoundType(SoundType.GLASS).setTranslationKey("redstoneLight").setCreativeTab(CreativeTabs.REDSTONE));
        registerBlock(124, "lit_redstone_lamp", (new BlockRedstoneLight(true)).setHardness(0.3F).setSoundType(SoundType.GLASS).setTranslationKey("redstoneLight"));
        registerBlock(125, "double_wooden_slab", (new BlockDoubleWoodSlab()).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("woodSlab"));
        registerBlock(126, "wooden_slab", (new BlockHalfWoodSlab()).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("woodSlab"));
        registerBlock(127, "cocoa", (new BlockCocoa()).setHardness(0.2F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("cocoa"));
        registerBlock(128, "sandstone_stairs", (new BlockStairs(block2.getDefaultState().withProperty(BlockSandStone.TYPE, BlockSandStone.EnumType.SMOOTH))).setTranslationKey("stairsSandStone"));
        registerBlock(129, "emerald_ore", (new BlockOre()).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("oreEmerald"));
        registerBlock(130, "ender_chest", (new BlockEnderChest()).setHardness(22.5F).setResistance(1000.0F).setSoundType(SoundType.STONE).setTranslationKey("enderChest").setLightLevel(0.5F));
        registerBlock(131, "tripwire_hook", (new BlockTripWireHook()).setTranslationKey("tripWireSource"));
        registerBlock(132, "tripwire", (new BlockTripWire()).setTranslationKey("tripWire"));
        registerBlock(133, "emerald_block", (new Block(Material.IRON, MapColor.EMERALD)).setHardness(5.0F).setResistance(10.0F).setSoundType(SoundType.METAL).setTranslationKey("blockEmerald").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(134, "spruce_stairs", (new BlockStairs(block1.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.SPRUCE))).setTranslationKey("stairsWoodSpruce"));
        registerBlock(135, "birch_stairs", (new BlockStairs(block1.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.BIRCH))).setTranslationKey("stairsWoodBirch"));
        registerBlock(136, "jungle_stairs", (new BlockStairs(block1.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.JUNGLE))).setTranslationKey("stairsWoodJungle"));
        registerBlock(137, "command_block", (new BlockCommandBlock(MapColor.BROWN)).setBlockUnbreakable().setResistance(6000000.0F).setTranslationKey("commandBlock"));
        registerBlock(138, "beacon", (new BlockBeacon()).setTranslationKey("beacon").setLightLevel(1.0F));
        registerBlock(139, "cobblestone_wall", (new BlockWall(block)).setTranslationKey("cobbleWall"));
        registerBlock(140, "flower_pot", (new BlockFlowerPot()).setHardness(0.0F).setSoundType(SoundType.STONE).setTranslationKey("flowerPot"));
        registerBlock(141, "carrots", (new BlockCarrot()).setTranslationKey("carrots"));
        registerBlock(142, "potatoes", (new BlockPotato()).setTranslationKey("potatoes"));
        registerBlock(143, "wooden_button", (new BlockButtonWood()).setHardness(0.5F).setSoundType(SoundType.WOOD).setTranslationKey("button"));
        registerBlock(144, "skull", (new BlockSkull()).setHardness(1.0F).setSoundType(SoundType.STONE).setTranslationKey("skull"));
        registerBlock(145, "anvil", (new BlockAnvil()).setHardness(5.0F).setSoundType(SoundType.ANVIL).setResistance(2000.0F).setTranslationKey("anvil"));
        registerBlock(146, "trapped_chest", (new BlockChest(BlockChest.Type.TRAP)).setHardness(2.5F).setSoundType(SoundType.WOOD).setTranslationKey("chestTrap"));
        registerBlock(147, "light_weighted_pressure_plate", (new BlockPressurePlateWeighted(Material.IRON, 15, MapColor.GOLD)).setHardness(0.5F).setSoundType(SoundType.WOOD).setTranslationKey("weightedPlate_light"));
        registerBlock(148, "heavy_weighted_pressure_plate", (new BlockPressurePlateWeighted(Material.IRON, 150)).setHardness(0.5F).setSoundType(SoundType.WOOD).setTranslationKey("weightedPlate_heavy"));
        registerBlock(149, "unpowered_comparator", (new BlockRedstoneComparator(false)).setHardness(0.0F).setSoundType(SoundType.WOOD).setTranslationKey("comparator").disableStats());
        registerBlock(150, "powered_comparator", (new BlockRedstoneComparator(true)).setHardness(0.0F).setLightLevel(0.625F).setSoundType(SoundType.WOOD).setTranslationKey("comparator").disableStats());
        registerBlock(151, "daylight_detector", new BlockDaylightDetector(false));
        registerBlock(152, "redstone_block", (new BlockCompressedPowered(Material.IRON, MapColor.TNT)).setHardness(5.0F).setResistance(10.0F).setSoundType(SoundType.METAL).setTranslationKey("blockRedstone").setCreativeTab(CreativeTabs.REDSTONE));
        registerBlock(153, "quartz_ore", (new BlockOre(MapColor.NETHERRACK)).setHardness(3.0F).setResistance(5.0F).setSoundType(SoundType.STONE).setTranslationKey("netherquartz"));
        registerBlock(154, "hopper", (new BlockHopper()).setHardness(3.0F).setResistance(8.0F).setSoundType(SoundType.METAL).setTranslationKey("hopper"));
        Block block11 = (new BlockQuartz()).setSoundType(SoundType.STONE).setHardness(0.8F).setTranslationKey("quartzBlock");
        registerBlock(155, "quartz_block", block11);
        registerBlock(156, "quartz_stairs", (new BlockStairs(block11.getDefaultState().withProperty(BlockQuartz.VARIANT, BlockQuartz.EnumType.DEFAULT))).setTranslationKey("stairsQuartz"));
        registerBlock(157, "activator_rail", (new BlockRailPowered(true)).setHardness(0.7F).setSoundType(SoundType.METAL).setTranslationKey("activatorRail"));
        registerBlock(158, "dropper", (new BlockDropper()).setHardness(3.5F).setSoundType(SoundType.STONE).setTranslationKey("dropper"));
        registerBlock(159, "stained_hardened_clay", (new BlockStainedHardenedClay()).setHardness(1.25F).setResistance(7.0F).setSoundType(SoundType.STONE).setTranslationKey("clayHardenedStained"));
        registerBlock(160, "stained_glass_pane", (new BlockStainedGlassPane()).setHardness(0.3F).setSoundType(SoundType.GLASS).setTranslationKey("thinStainedGlass"));
        registerBlock(161, "leaves2", (new BlockNewLeaf()).setTranslationKey("leaves"));
        registerBlock(162, "log2", (new BlockNewLog()).setTranslationKey("log"));
        registerBlock(163, "acacia_stairs", (new BlockStairs(block1.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.ACACIA))).setTranslationKey("stairsWoodAcacia"));
        registerBlock(164, "dark_oak_stairs", (new BlockStairs(block1.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.DARK_OAK))).setTranslationKey("stairsWoodDarkOak"));
        registerBlock(165, "slime", (new BlockSlime()).setTranslationKey("slime").setSoundType(SoundType.SLIME));
        registerBlock(166, "barrier", (new BlockBarrier()).setTranslationKey("barrier"));
        registerBlock(167, "iron_trapdoor", (new BlockTrapDoor(Material.IRON)).setHardness(5.0F).setSoundType(SoundType.METAL).setTranslationKey("ironTrapdoor").disableStats());
        registerBlock(168, "prismarine", (new BlockPrismarine()).setHardness(1.5F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("prismarine"));
        registerBlock(169, "sea_lantern", (new BlockSeaLantern(Material.GLASS)).setHardness(0.3F).setSoundType(SoundType.GLASS).setLightLevel(1.0F).setTranslationKey("seaLantern"));
        registerBlock(170, "hay_block", (new BlockHay()).setHardness(0.5F).setSoundType(SoundType.PLANT).setTranslationKey("hayBlock").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(171, "carpet", (new BlockCarpet()).setHardness(0.1F).setSoundType(SoundType.CLOTH).setTranslationKey("woolCarpet").setLightOpacity(0));
        registerBlock(172, "hardened_clay", (new BlockHardenedClay()).setHardness(1.25F).setResistance(7.0F).setSoundType(SoundType.STONE).setTranslationKey("clayHardened"));
        registerBlock(173, "coal_block", (new Block(Material.ROCK, MapColor.BLACK)).setHardness(5.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("blockCoal").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(174, "packed_ice", (new BlockPackedIce()).setHardness(0.5F).setSoundType(SoundType.GLASS).setTranslationKey("icePacked"));
        registerBlock(175, "double_plant", new BlockDoublePlant());
        registerBlock(176, "standing_banner", (new BlockBanner.BlockBannerStanding()).setHardness(1.0F).setSoundType(SoundType.WOOD).setTranslationKey("banner").disableStats());
        registerBlock(177, "wall_banner", (new BlockBanner.BlockBannerHanging()).setHardness(1.0F).setSoundType(SoundType.WOOD).setTranslationKey("banner").disableStats());
        registerBlock(178, "daylight_detector_inverted", new BlockDaylightDetector(true));
        Block block12 = (new BlockRedSandstone()).setSoundType(SoundType.STONE).setHardness(0.8F).setTranslationKey("redSandStone");
        registerBlock(179, "red_sandstone", block12);
        registerBlock(180, "red_sandstone_stairs", (new BlockStairs(block12.getDefaultState().withProperty(BlockRedSandstone.TYPE, BlockRedSandstone.EnumType.SMOOTH))).setTranslationKey("stairsRedSandStone"));
        registerBlock(181, "double_stone_slab2", (new BlockDoubleStoneSlabNew()).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("stoneSlab2"));
        registerBlock(182, "stone_slab2", (new BlockHalfStoneSlabNew()).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("stoneSlab2"));
        registerBlock(183, "spruce_fence_gate", (new BlockFenceGate(BlockPlanks.EnumType.SPRUCE)).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("spruceFenceGate"));
        registerBlock(184, "birch_fence_gate", (new BlockFenceGate(BlockPlanks.EnumType.BIRCH)).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("birchFenceGate"));
        registerBlock(185, "jungle_fence_gate", (new BlockFenceGate(BlockPlanks.EnumType.JUNGLE)).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("jungleFenceGate"));
        registerBlock(186, "dark_oak_fence_gate", (new BlockFenceGate(BlockPlanks.EnumType.DARK_OAK)).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("darkOakFenceGate"));
        registerBlock(187, "acacia_fence_gate", (new BlockFenceGate(BlockPlanks.EnumType.ACACIA)).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("acaciaFenceGate"));
        registerBlock(188, "spruce_fence", (new BlockFence(Material.WOOD, BlockPlanks.EnumType.SPRUCE.getMapColor())).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("spruceFence"));
        registerBlock(189, "birch_fence", (new BlockFence(Material.WOOD, BlockPlanks.EnumType.BIRCH.getMapColor())).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("birchFence"));
        registerBlock(190, "jungle_fence", (new BlockFence(Material.WOOD, BlockPlanks.EnumType.JUNGLE.getMapColor())).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("jungleFence"));
        registerBlock(191, "dark_oak_fence", (new BlockFence(Material.WOOD, BlockPlanks.EnumType.DARK_OAK.getMapColor())).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("darkOakFence"));
        registerBlock(192, "acacia_fence", (new BlockFence(Material.WOOD, BlockPlanks.EnumType.ACACIA.getMapColor())).setHardness(2.0F).setResistance(5.0F).setSoundType(SoundType.WOOD).setTranslationKey("acaciaFence"));
        registerBlock(193, "spruce_door", (new BlockDoor(Material.WOOD)).setHardness(3.0F).setSoundType(SoundType.WOOD).setTranslationKey("doorSpruce").disableStats());
        registerBlock(194, "birch_door", (new BlockDoor(Material.WOOD)).setHardness(3.0F).setSoundType(SoundType.WOOD).setTranslationKey("doorBirch").disableStats());
        registerBlock(195, "jungle_door", (new BlockDoor(Material.WOOD)).setHardness(3.0F).setSoundType(SoundType.WOOD).setTranslationKey("doorJungle").disableStats());
        registerBlock(196, "acacia_door", (new BlockDoor(Material.WOOD)).setHardness(3.0F).setSoundType(SoundType.WOOD).setTranslationKey("doorAcacia").disableStats());
        registerBlock(197, "dark_oak_door", (new BlockDoor(Material.WOOD)).setHardness(3.0F).setSoundType(SoundType.WOOD).setTranslationKey("doorDarkOak").disableStats());
        registerBlock(198, "end_rod", (new BlockEndRod()).setHardness(0.0F).setLightLevel(0.9375F).setSoundType(SoundType.WOOD).setTranslationKey("endRod"));
        registerBlock(199, "chorus_plant", (new BlockChorusPlant()).setHardness(0.4F).setSoundType(SoundType.WOOD).setTranslationKey("chorusPlant"));
        registerBlock(200, "chorus_flower", (new BlockChorusFlower()).setHardness(0.4F).setSoundType(SoundType.WOOD).setTranslationKey("chorusFlower"));
        Block block13 = (new Block(Material.ROCK, MapColor.MAGENTA)).setHardness(1.5F).setResistance(10.0F).setSoundType(SoundType.STONE).setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setTranslationKey("purpurBlock");
        registerBlock(201, "purpur_block", block13);
        registerBlock(202, "purpur_pillar", (new BlockRotatedPillar(Material.ROCK, MapColor.MAGENTA)).setHardness(1.5F).setResistance(10.0F).setSoundType(SoundType.STONE).setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setTranslationKey("purpurPillar"));
        registerBlock(203, "purpur_stairs", (new BlockStairs(block13.getDefaultState())).setTranslationKey("stairsPurpur"));
        registerBlock(204, "purpur_double_slab", (new BlockPurpurSlab.Double()).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("purpurSlab"));
        registerBlock(205, "purpur_slab", (new BlockPurpurSlab.Half()).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("purpurSlab"));
        registerBlock(206, "end_bricks", (new Block(Material.ROCK, MapColor.SAND)).setSoundType(SoundType.STONE).setHardness(0.8F).setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setTranslationKey("endBricks"));
        registerBlock(207, "beetroots", (new BlockBeetroot()).setTranslationKey("beetroots"));
        Block block14 = (new BlockGrassPath()).setHardness(0.65F).setSoundType(SoundType.PLANT).setTranslationKey("grassPath").disableStats();
        registerBlock(208, "grass_path", block14);
        registerBlock(209, "end_gateway", (new BlockEndGateway(Material.PORTAL)).setHardness(-1.0F).setResistance(6000000.0F));
        registerBlock(210, "repeating_command_block", (new BlockCommandBlock(MapColor.PURPLE)).setBlockUnbreakable().setResistance(6000000.0F).setTranslationKey("repeatingCommandBlock"));
        registerBlock(211, "chain_command_block", (new BlockCommandBlock(MapColor.GREEN)).setBlockUnbreakable().setResistance(6000000.0F).setTranslationKey("chainCommandBlock"));
        registerBlock(212, "frosted_ice", (new BlockFrostedIce()).setHardness(0.5F).setLightOpacity(3).setSoundType(SoundType.GLASS).setTranslationKey("frostedIce"));
        registerBlock(213, "magma", (new BlockMagma()).setHardness(0.5F).setSoundType(SoundType.STONE).setTranslationKey("magma"));
        registerBlock(214, "nether_wart_block", (new Block(Material.GRASS, MapColor.RED)).setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setHardness(1.0F).setSoundType(SoundType.WOOD).setTranslationKey("netherWartBlock"));
        registerBlock(215, "red_nether_brick", (new BlockNetherBrick()).setHardness(2.0F).setResistance(10.0F).setSoundType(SoundType.STONE).setTranslationKey("redNetherBrick").setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
        registerBlock(216, "bone_block", (new BlockBone()).setTranslationKey("boneBlock"));
        registerBlock(217, "structure_void", (new BlockStructureVoid()).setTranslationKey("structureVoid"));
        registerBlock(218, "observer", (new BlockObserver()).setHardness(3.0F).setTranslationKey("observer"));
        registerBlock(219, "white_shulker_box", (new BlockShulkerBox(EnumDyeColor.WHITE)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxWhite"));
        registerBlock(220, "orange_shulker_box", (new BlockShulkerBox(EnumDyeColor.ORANGE)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxOrange"));
        registerBlock(221, "magenta_shulker_box", (new BlockShulkerBox(EnumDyeColor.MAGENTA)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxMagenta"));
        registerBlock(222, "light_blue_shulker_box", (new BlockShulkerBox(EnumDyeColor.LIGHT_BLUE)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxLightBlue"));
        registerBlock(223, "yellow_shulker_box", (new BlockShulkerBox(EnumDyeColor.YELLOW)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxYellow"));
        registerBlock(224, "lime_shulker_box", (new BlockShulkerBox(EnumDyeColor.LIME)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxLime"));
        registerBlock(225, "pink_shulker_box", (new BlockShulkerBox(EnumDyeColor.PINK)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxPink"));
        registerBlock(226, "gray_shulker_box", (new BlockShulkerBox(EnumDyeColor.GRAY)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxGray"));
        registerBlock(227, "silver_shulker_box", (new BlockShulkerBox(EnumDyeColor.SILVER)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxSilver"));
        registerBlock(228, "cyan_shulker_box", (new BlockShulkerBox(EnumDyeColor.CYAN)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxCyan"));
        registerBlock(229, "purple_shulker_box", (new BlockShulkerBox(EnumDyeColor.PURPLE)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxPurple"));
        registerBlock(230, "blue_shulker_box", (new BlockShulkerBox(EnumDyeColor.BLUE)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxBlue"));
        registerBlock(231, "brown_shulker_box", (new BlockShulkerBox(EnumDyeColor.BROWN)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxBrown"));
        registerBlock(232, "green_shulker_box", (new BlockShulkerBox(EnumDyeColor.GREEN)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxGreen"));
        registerBlock(233, "red_shulker_box", (new BlockShulkerBox(EnumDyeColor.RED)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxRed"));
        registerBlock(234, "black_shulker_box", (new BlockShulkerBox(EnumDyeColor.BLACK)).setHardness(2.0F).setSoundType(SoundType.STONE).setTranslationKey("shulkerBoxBlack"));
        registerBlock(235, "white_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.WHITE));
        registerBlock(236, "orange_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.ORANGE));
        registerBlock(237, "magenta_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.MAGENTA));
        registerBlock(238, "light_blue_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.LIGHT_BLUE));
        registerBlock(239, "yellow_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.YELLOW));
        registerBlock(240, "lime_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.LIME));
        registerBlock(241, "pink_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.PINK));
        registerBlock(242, "gray_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.GRAY));
        registerBlock(243, "silver_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.SILVER));
        registerBlock(244, "cyan_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.CYAN));
        registerBlock(245, "purple_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.PURPLE));
        registerBlock(246, "blue_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.BLUE));
        registerBlock(247, "brown_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.BROWN));
        registerBlock(248, "green_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.GREEN));
        registerBlock(249, "red_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.RED));
        registerBlock(250, "black_glazed_terracotta", new BlockGlazedTerracotta(EnumDyeColor.BLACK));
        registerBlock(251, "concrete", (new BlockColored(Material.ROCK)).setHardness(1.8F).setSoundType(SoundType.STONE).setTranslationKey("concrete"));
        registerBlock(252, "concrete_powder", (new BlockConcretePowder()).setHardness(0.5F).setSoundType(SoundType.SAND).setTranslationKey("concretePowder"));
        registerBlock(255, "structure_block", (new BlockStructure()).setBlockUnbreakable().setResistance(6000000.0F).setTranslationKey("structureBlock"));
        REGISTRY.validateKey();

        for (Block block15 : REGISTRY)
        {
            if (block15.material == Material.AIR)
            {
                block15.useNeighborBrightness = false;
            }
            else
            {
                boolean flag = false;
                boolean flag1 = block15 instanceof BlockStairs;
                boolean flag2 = block15 instanceof BlockSlab;
                boolean flag3 = block15 == block6 || block15 == block14;
                boolean flag4 = block15.translucent;
                boolean flag5 = block15.lightOpacity == 0;

                if (flag1 || flag2 || flag3 || flag4 || flag5)
                {
                    flag = true;
                }

                block15.useNeighborBrightness = flag;
            }
        }
    }

    private static void registerBlock(int id, ResourceLocation textualID, Block block_)
    {
        REGISTRY.register(id, textualID, block_);
    }

    private static void registerBlock(int id, String textualID, Block block_)
    {
        registerBlock(id, new ResourceLocation(textualID), block_);
    }

    public static enum EnumOffsetType
    {
        NONE,
        XZ,
        XYZ;
    }
}