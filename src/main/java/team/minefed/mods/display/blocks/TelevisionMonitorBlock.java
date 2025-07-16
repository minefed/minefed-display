package team.minefed.mods.display.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
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
import team.minefed.mods.display.enums.TvPart;

import java.util.Map;

public class TelevisionMonitorBlock extends HorizontalFacingBlock implements BlockEntityProvider {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<TvPart> PART = EnumProperty.of("part", TvPart.class);

    private static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(0, 0, 0,   1, 1, 0.5);
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(0, 0, 0.5, 1, 1, 1);
    private static final VoxelShape WEST_SHAPE  = VoxelShapes.cuboid(0, 0, 0,   0.5, 1, 1);
    private static final VoxelShape EAST_SHAPE  = VoxelShapes.cuboid(0.5,0, 0,  1, 1, 1);

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, NORTH_SHAPE,
            Direction.SOUTH, SOUTH_SHAPE,
            Direction.WEST,  WEST_SHAPE,
            Direction.EAST,  EAST_SHAPE
    );

    public TelevisionMonitorBlock() {
        super(AbstractBlock.Settings.create().sounds(BlockSoundGroup.STONE));
        this.setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(PART, TvPart.LEFT));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TelevisionMonitorBlockEntity(pos, state);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        Direction right  = facing.rotateYCounterclockwise();
        BlockPos anchor  = ctx.getBlockPos();
        World w = ctx.getWorld();

        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                BlockPos p = anchor.offset(right, dx).down(dy);

                if (!w.getBlockState(p).canReplace(ctx)) {
                    return null;
                }
            }
        }

        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                BlockPos p = anchor.offset(right, dx).down(dy);
                BlockState s = getDefaultState()
                        .with(FACING, facing)
                        .with(PART, TvPart.values()[dx])
                        .with(HALF, dy == 0 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);

                w.setBlockState(p, s, Block.NOTIFY_ALL);
            }
        }

        return getDefaultState()
                .with(FACING, facing)
                .with(PART, TvPart.LEFT)
                .with(HALF, DoubleBlockHalf.UPPER);
    }

    @Override
    public BlockState onBreak(World w, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockPos anchorPos = findAnchor(pos, state);
        BlockState anchorState = w.getBlockState(anchorPos);

        if (anchorState.isOf(this)) {
            removeWholeStructure(w, anchorPos, anchorState);
        }

        return super.onBreak(w, pos, state, player);
    }

    private BlockPos findAnchor(BlockPos pos, BlockState state) {
        Direction tvRight = state.get(FACING).rotateYCounterclockwise();
        Direction tvLeft = tvRight.getOpposite();

        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            pos = pos.up();
        }

        switch (state.get(PART)) {
            case CENTER:
                return pos.offset(tvLeft);
            case RIGHT:
                return pos.offset(tvLeft, 2);
            default:
                return pos;
        }
    }

    private void removeWholeStructure(World w, BlockPos anchor, BlockState anchorState) {
        Direction right = anchorState.get(FACING).rotateYCounterclockwise();
        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                BlockPos p = anchor.offset(right, dx).down(dy);
                if (w.getBlockState(p).isOf(this)) {
                    w.setBlockState(p, Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL | Block.FORCE_STATE);
                }
            }
        }
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(FACING, HALF, PART);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world,
                                      BlockPos pos, ShapeContext ctx) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world,
                                        BlockPos pos, ShapeContext ctx) {
        return getOutlineShape(state, world, pos, ctx);
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return null;
    }

    @Override public BlockState rotate(BlockState s, BlockRotation r){ return s.with(FACING, r.rotate(s.get(FACING))); }
    @Override public BlockState mirror(BlockState s, BlockMirror m){ return rotate(s, m.getRotation(s.get(FACING))); }
}