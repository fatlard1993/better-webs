package justfatlard.better_webs;

import justfatlard.pandorical.api.PandoricalApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What a spider's silk looks like on its way somewhere: strands drawn as a fine line of dust, and
 * puffs of cobweb. All of it is vanilla particles, so every player sees it; the spider's own
 * movements, played through Pandorical, only those who have it.
 */
public final class Silk {
	public static final String THROW_ANIMATION = Main.MOD_ID + ":web_throw";
	public static final String SPIN_ANIMATION = Main.MOD_ID + ":web_spin";

	private static final DustParticleOptions STRAND = new DustParticleOptions(0xF4F7F7, 0.6F);
	private static final ItemParticleOption PUFF = new ItemParticleOption(ParticleTypes.ITEM, Items.COBWEB);

	/** Dust every this many blocks along a strand: close enough to read as a line, not a dotted one. */
	private static final double STRAND_STEP = 0.12;

	private Silk() {}

	/** Where silk comes from: the tip of the abdomen, behind and above the spider's middle. */
	public static Vec3 spinnerets(Entity spider) {
		double size = spider.getBbWidth() / 1.4;
		Vec3 back = Vec3.directionFromRotation(0, spider.getYRot()).scale(-0.75 * size);
		return spider.position().add(back).add(0, spider.getBbHeight() * 0.7, 0);
	}

	public static void strand(ServerLevel level, Vec3 from, Vec3 to) {
		double length = from.distanceTo(to);
		int steps = Math.max(2, (int) (length / STRAND_STEP));
		for (int i = 0; i <= steps; i++) {
			Vec3 at = from.lerp(to, (double) i / steps);
			level.sendParticles(STRAND, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		}
	}

	public static void puff(ServerLevel level, Vec3 at, int count, double spread) {
		level.sendParticles(PUFF, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
	}

	/** A point on the face of a block beside a spot, a little off its middle, for a strand to land on. */
	public static Vec3 anchor(BlockPos spot, Direction side, RandomSource random) {
		Vec3 centre = Vec3.atCenterOf(spot).add(new Vec3(side.getStepX(), side.getStepY(), side.getStepZ()).scale(0.5));
		double u = (random.nextDouble() - 0.5) * 0.7;
		double v = (random.nextDouble() - 0.5) * 0.7;
		return switch (side.getAxis()) {
			case X -> centre.add(0, u, v);
			case Y -> centre.add(u, 0, v);
			case Z -> centre.add(u, v, 0);
		};
	}

	/**
	 * One strand of a web being spun: from the spinnerets to one of the faces the web will hang from,
	 * then on into the middle of the spot, landing with a soft sound.
	 */
	public static void spinStrand(ServerLevel level, Entity spider, BlockPos spot) {
		List<Direction> holds = new ArrayList<>();
		for (Direction side : Direction.values()) {
			BlockPos beside = spot.relative(side);
			BlockState state = level.getBlockState(beside);
			if (state.is(Blocks.COBWEB) || !state.getCollisionShape(level, beside).isEmpty()) holds.add(side);
		}
		if (holds.isEmpty()) return;
		RandomSource random = level.getRandom();
		Vec3 anchor = anchor(spot, holds.get(random.nextInt(holds.size())), random);
		strand(level, spinnerets(spider), anchor);
		strand(level, anchor, Vec3.atCenterOf(spot));
		level.playSound(null, spot, SoundEvents.COBWEB_STEP, SoundSource.HOSTILE, 0.4F, 1.4F + random.nextFloat() * 0.3F);
	}

	public static void animate(Entity spider, String animation, boolean looping) {
		if (PandoricalApi.isAvailable()) PandoricalApi.animations().play(spider, animation, looping);
	}

	public static void stopAnimating(Entity spider) {
		if (PandoricalApi.isAvailable()) PandoricalApi.animations().stop(spider);
	}
}
