package team.minefed.mods.display.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CustomSizeDisplayBlock extends HorizontalFacingBlock implements BlockEntityProvider {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty IS_MAIN = BooleanProperty.of("is_main");

    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 32;

    private static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(0, 0, 0.5, 1, 1, 1);
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(0, 0, 0, 1, 1, 0.5);
    private static final VoxelShape WEST_SHAPE = VoxelShapes.cuboid(0.5, 0, 0, 1, 1, 1);
    private static final VoxelShape EAST_SHAPE = VoxelShapes.cuboid(0, 0, 0, 0.5, 1, 1);

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, NORTH_SHAPE,
            Direction.SOUTH, SOUTH_SHAPE,
            Direction.WEST, WEST_SHAPE,
            Direction.EAST, EAST_SHAPE);

    public CustomSizeDisplayBlock() {
        super(AbstractBlock.Settings.create()
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque()
                .strength(2.0f));
        this.setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(IS_MAIN, true));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        // Only main block has a block entity
        if (state.get(IS_MAIN)) {
            return new CustomSizeDisplayBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();

        if (!world.getBlockState(pos).canReplace(ctx)) {
            return null;
        }

        // Place single block initially, size can be adjusted via GUI
        return getDefaultState()
                .with(FACING, facing)
                .with(IS_MAIN, true);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (state.get(IS_MAIN)) {
            // Main block is being broken, remove entire structure
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CustomSizeDisplayBlockEntity displayEntity) {
                removeStructure(world, pos, state.get(FACING), displayEntity.getDisplayWidth(),
                        displayEntity.getDisplayHeight());
            }
        } else {
            // Secondary block is being broken, find and remove from main
            findAndRemoveStructure(world, pos, state);
        }
        return super.onBreak(world, pos, state, player);
    }

    private void removeStructure(World world, BlockPos mainPos, Direction facing, int width, int height) {
        Direction right = facing.rotateYCounterclockwise();

        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                BlockPos p = mainPos.offset(right, dx).down(dy);
                if (world.getBlockState(p).isOf(this)) {
                    world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.FORCE_STATE);
                }
            }
        }
    }

    private void findAndRemoveStructure(World world, BlockPos pos, BlockState state) {
        Direction facing = state.get(FACING);
        Direction left = facing.rotateYClockwise();

        // Search for main block within MAX_SIZE range
        for (int dx = 0; dx < MAX_SIZE; dx++) {
            for (int dy = 0; dy < MAX_SIZE; dy++) {
                BlockPos checkPos = pos.offset(left, dx).up(dy);
                BlockState checkState = world.getBlockState(checkPos);

                if (checkState.isOf(this) && checkState.get(IS_MAIN)) {
                    BlockEntity be = world.getBlockEntity(checkPos);
                    if (be instanceof CustomSizeDisplayBlockEntity displayEntity) {
                        removeStructure(world, checkPos, facing, displayEntity.getDisplayWidth(),
                                displayEntity.getDisplayHeight());
                        return;
                    }
                }
            }
        }
    }

    /**
     * Expand or shrink the display to the specified size.
     * Called when user changes size via GUI.
     */
    public boolean resizeDisplay(World world, BlockPos mainPos, int newWidth, int newHeight) {
        BlockState mainState = world.getBlockState(mainPos);
        if (!mainState.isOf(this) || !mainState.get(IS_MAIN)) {
            return false;
        }

        BlockEntity be = world.getBlockEntity(mainPos);
        if (!(be instanceof CustomSizeDisplayBlockEntity displayEntity)) {
            return false;
        }

        Direction facing = mainState.get(FACING);
        Direction right = facing.rotateYCounterclockwise();

        int oldWidth = displayEntity.getDisplayWidth();
        int oldHeight = displayEntity.getDisplayHeight();

        // Check if new space is available
        for (int dx = 0; dx < newWidth; dx++) {
            for (int dy = 0; dy < newHeight; dy++) {
                BlockPos p = mainPos.offset(right, dx).down(dy);
                BlockState existing = world.getBlockState(p);

                // Allow if it's air, replaceable, or already our block
                if (!existing.isAir() && !existing.isOf(this)) {
                    return false;
                }
            }
        }

        // Remove old structure first
        for (int dx = 0; dx < oldWidth; dx++) {
            for (int dy = 0; dy < oldHeight; dy++) {
                BlockPos p = mainPos.offset(right, dx).down(dy);
                if (world.getBlockState(p).isOf(this)) {
                    world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.FORCE_STATE);
                }
            }
        }

        // Place new structure
        for (int dx = 0; dx < newWidth; dx++) {
            for (int dy = 0; dy < newHeight; dy++) {
                BlockPos p = mainPos.offset(right, dx).down(dy);
                boolean isMain = (dx == 0 && dy == 0);
                BlockState newState = getDefaultState()
                        .with(FACING, facing)
                        .with(IS_MAIN, isMain);
                world.setBlockState(p, newState, Block.NOTIFY_ALL);
            }
        }

        // Update entity with new size
        BlockEntity newBe = world.getBlockEntity(mainPos);
        if (newBe instanceof CustomSizeDisplayBlockEntity newDisplayEntity) {
            newDisplayEntity.setDisplaySize(newWidth, newHeight);
            newDisplayEntity.setUrl(displayEntity.getUrl());
        }

        return true;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, IS_MAIN);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return getOutlineShape(state, world, pos, ctx);
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return null;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return rotate(state, mirror.getRotation(state.get(FACING)));
    }
}
