package justfatlard.better_webs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** The only cobweb ever given a scheduled tick is a thrown one, so its tick is when it comes apart. */
@Mixin(WebBlock.class)
public abstract class WebBlockMixin extends Block {
	protected WebBlockMixin(Properties properties) {
		super(properties);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.destroyBlock(pos, false);
	}
}
