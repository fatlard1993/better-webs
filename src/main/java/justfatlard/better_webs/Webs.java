package justfatlard.better_webs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/** Where a web can hang, the same way Pandorical's strands decide what holds one. */
public final class Webs {
	private Webs() {}

	/** Sides of a spot a web there would be strung to: more web, or anything solid. */
	public static int heldSides(BlockGetter level, BlockPos pos) {
		int held = 0;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (Direction side : Direction.values()) {
			at.setWithOffset(pos, side);
			BlockState beyond = level.getBlockState(at);
			if (beyond.is(Blocks.COBWEB) || beyond.isFaceSturdy(level, at, side.getOpposite(), SupportType.CENTER)
					|| !beyond.getCollisionShape(level, at).isEmpty()) held++;
		}
		return held;
	}

	public static boolean touchesWeb(BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			if (level.getBlockState(pos.relative(side)).is(Blocks.COBWEB)) return true;
		}
		return false;
	}

	public static boolean mayChangeBlocks(ServerLevel level) {
		return level.getGameRules().get(GameRules.MOB_GRIEFING);
	}
}
