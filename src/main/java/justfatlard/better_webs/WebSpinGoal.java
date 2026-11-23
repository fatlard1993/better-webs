package justfatlard.better_webs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A spider with nothing to hunt now and then goes and spins a web somewhere dark: beside a web
 * already there if it can, so webs spread into a den, otherwise into a corner it can string one
 * across. It stops once the place around it is webbed enough.
 */
public final class WebSpinGoal extends Goal {
	/** One evaluation in this many, so an idle spider spins about once a minute. */
	private static final int CHANCE = 1200;

	private static final int SEARCH_TRIES = 24;
	private static final int SEARCH_RADIUS = 4;

	/** Webs already within {@link #CROWD_RADIUS} that make a spot webbed enough. */
	private static final int CROWDED = 8;
	private static final int CROWD_RADIUS = 4;

	/** Darker than this, as mobs count it: where spiders come from. */
	private static final int DARK_BELOW = 8;

	private static final double REACH = 2.0;
	private static final int GIVE_UP_TICKS = 200;

	/** How long the spinning itself takes, once the spider is there: long enough to watch. */
	private static final int SPIN_TICKS = 40;

	/** A strand shot every this many ticks while it spins, each to one of the faces the web will hang from. */
	private static final int STRAND_EVERY = 5;

	private final Spider spider;
	private BlockPos spot;
	private int ticks;
	private int spinning;

	public WebSpinGoal(Spider spider) {
		this.spider = spider;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (spider.getTarget() != null || spider.isVehicle() || spider.isPassenger()) return false;
		if (!(spider.level() instanceof ServerLevel level) || !Webs.mayChangeBlocks(level)) return false;
		if (spider.getRandom().nextInt(reducedTickDelay(CHANCE)) != 0) return false;
		spot = findSpot(level);
		return spot != null;
	}

	@Override
	public boolean canContinueToUse() {
		return spot != null && (ticks < GIVE_UP_TICKS || spinning > 0) && spider.getTarget() == null && spider.level().getBlockState(spot).isAir();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		ticks = 0;
		spinning = 0;
		spider.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.0);
	}

	@Override
	public void tick() {
		ticks++;
		if (!(spider.level() instanceof ServerLevel level)) return;
		if (spinning > 0) {
			spin(level);
			return;
		}
		spider.getLookControl().setLookAt(spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5);
		if (spider.getBoundingBox().inflate(REACH).contains(Vec3.atCenterOf(spot))) {
			if (!fits(level, spot)) {
				spot = null;
				return;
			}
			spinning = SPIN_TICKS;
			spider.getNavigation().stop();
			turnBackTo(spot);
			Silk.animate(spider, Silk.SPIN_ANIMATION, true);
		} else if (spider.getNavigation().isDone()) {
			spot = null;
		}
	}

	/** Its back to the spot, the way a spider spins: silk comes from the tip of the abdomen. */
	private void turnBackTo(BlockPos spot) {
		double dx = spot.getX() + 0.5 - spider.getX();
		double dz = spot.getZ() + 0.5 - spider.getZ();
		float away = (float) Math.toDegrees(Math.atan2(dz, dx)) + 90.0F;
		spider.setYRot(away);
		spider.setYBodyRot(away);
		spider.setYHeadRot(away);
	}

	/**
	 * Strands shot one at a time from the spinnerets to the faces the web will hang from, each
	 * landing with a soft sound, and when the last is down, the web.
	 */
	private void spin(ServerLevel level) {
		spinning--;
		if (spinning % STRAND_EVERY == 0 && spinning > 0) Silk.spinStrand(level, spider, spot);
		if (spinning > 0) return;
		if (fits(level, spot)) {
			level.setBlockAndUpdate(spot, Blocks.COBWEB.defaultBlockState());
			Silk.puff(level, Vec3.atCenterOf(spot), 8, 0.3);
			level.playSound(null, spot, SoundEvents.COBWEB_PLACE, SoundSource.HOSTILE, 0.8F, 1.0F);
		}
		spot = null;
	}

	@Override
	public void stop() {
		spot = null;
		spinning = 0;
		Silk.stopAnimating(spider);
		spider.getNavigation().stop();
	}

	/** The best of a handful of tries: spreading a web outranks starting one, and more held sides outrank fewer. */
	private BlockPos findSpot(ServerLevel level) {
		BlockPos home = spider.blockPosition();
		BlockPos best = null;
		int bestScore = 0;
		for (int i = 0; i < SEARCH_TRIES; i++) {
			BlockPos pos = home.offset(
				spider.getRandom().nextIntBetweenInclusive(-SEARCH_RADIUS, SEARCH_RADIUS),
				spider.getRandom().nextIntBetweenInclusive(-1, 3),
				spider.getRandom().nextIntBetweenInclusive(-SEARCH_RADIUS, SEARCH_RADIUS));
			if (!fits(level, pos)) continue;
			int score = Webs.heldSides(level, pos) + (Webs.touchesWeb(level, pos) ? 4 : 0);
			if (score > bestScore) {
				best = pos;
				bestScore = score;
			}
		}
		return best != null && !crowded(level, best) ? best : null;
	}

	private static boolean fits(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).isAir()
			&& level.getMaxLocalRawBrightness(pos) < DARK_BELOW
			&& Webs.heldSides(level, pos) >= 2
			// Never round anyone, the spider included.
			&& level.getEntities(null, new AABB(pos)).isEmpty();
	}

	private static boolean crowded(ServerLevel level, BlockPos pos) {
		int webs = 0;
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-CROWD_RADIUS, -CROWD_RADIUS, -CROWD_RADIUS), pos.offset(CROWD_RADIUS, CROWD_RADIUS, CROWD_RADIUS))) {
			if (level.getBlockState(near).is(Blocks.COBWEB) && ++webs >= CROWDED) return true;
		}
		return false;
	}
}
