package team.minefed.mods.display.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
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
import team.minefed.mods.display.enums.TvPart;

public class TelevisionMonitorBlock extends HorizontalFacingBlock {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<TvPart> PART = EnumProperty.of("part", TvPart.class);

    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0, 0.0, 0, 3.0, 2.0, 0.5);

    public TelevisionMonitorBlock() {
        super(AbstractBlock.Settings.create().sounds(BlockSoundGroup.STONE));
        this.setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(PART, TvPart.LEFT));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        BlockPos origin = ctx.getBlockPos();
        World w = ctx.getWorld();

        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                BlockPos p = origin.offset(facing.rotateYClockwise(), -dx).up(dy);
                BlockState s = getDefaultState()
                        .with(FACING, facing)
                        .with(PART, TvPart.values()[dx])
                        .with(HALF, dy == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                w.setBlockState(p, s, Block.NOTIFY_ALL);
            }
        }

        return getDefaultState().with(FACING, facing);
    }

    @Override
    public BlockState onBreak(World w, BlockPos pos, BlockState state, PlayerEntity player) {
        removeWholeStructure(w, pos, state);

        return super.onBreak(w, pos, state, player);
    }

    private void removeWholeStructure(World w, BlockPos pos, BlockState state) {
        Direction f = state.get(FACING);
        int baseX = state.get(PART) == TvPart.LEFT ? 0 : state.get(PART) == TvPart.CENTER ? 1 : 2;
        int baseY = state.get(HALF) == DoubleBlockHalf.LOWER ? 0 : 1;
        BlockPos anchor = pos.offset(f.rotateYClockwise(), baseX).down(baseY);

        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                BlockPos p = anchor.offset(f.rotateYClockwise(), -dx).up(dy);
                if (w.getBlockState(p).getBlock() == this) w.breakBlock(p, false);
            }
        }
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(FACING, HALF, PART);
    }
    @Override public VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c){ return SHAPE; }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return null;
    }

    @Override public BlockState rotate(BlockState s, BlockRotation r){ return s.with(FACING, r.rotate(s.get(FACING))); }
    @Override public BlockState mirror(BlockState s, BlockMirror m){ return rotate(s, m.getRotation(s.get(FACING))); }
}