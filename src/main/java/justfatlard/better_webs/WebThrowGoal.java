package justfatlard.better_webs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

/**
 * Now and then a spider hunting something out of reach stops, rears back, and throws a ball of web
 * at it. The rear is the warning: half a second to get out of the way.
 */
public final class WebThrowGoal extends Goal {
	private static final double MIN_RANGE = 5;
	private static final double MAX_RANGE = 14;

	/** One evaluation in this many, once in range and in sight, so it is a surprise rather than a habit. */
	private static final int CHANCE = 60;

	private static final int WIND_UP_TICKS = 10;

	/** The settle after the flick, so the throw animation plays out before the spider moves on. */
	private static final int FOLLOW_THROUGH_TICKS = 6;
	private static final int REST_MIN_TICKS = 200;
	private static final int REST_SPREAD_TICKS = 200;

	private static final float VELOCITY = 1.2F;
	private static final float INACCURACY = 3.0F;

	private final Spider spider;
	private LivingEntity target;
	private int windUp;
	private int followThrough;
	private long restUntil;

	public WebThrowGoal(Spider spider) {
		this.spider = spider;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (spider.level().getGameTime() < restUntil) return false;
		LivingEntity candidate = spider.getTarget();
		if (!throwable(candidate)) return false;
		if (spider.getRandom().nextInt(reducedTickDelay(CHANCE)) != 0) return false;
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (windUp <= 0) return followThrough > 0;
		return target.isAlive() && spider.getTarget() == target;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		windUp = WIND_UP_TICKS;
		followThrough = FOLLOW_THROUGH_TICKS;
		spider.getNavigation().stop();
		spider.playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 1.6F);
		Silk.animate(spider, Silk.THROW_ANIMATION, false);
	}

	@Override
	public void tick() {
		if (windUp <= 0) {
			followThrough--;
			return;
		}
		spider.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (!(spider.level() instanceof ServerLevel level)) return;
		// Silk gathering at the spinnerets while it rears, the tell that something is coming.
		if (windUp % 3 == 0) Silk.puff(level, Silk.spinnerets(spider), 2, 0.08);
		if (--windUp > 0) return;
		if (spider.getSensing().hasLineOfSight(target)) {
			ThrownWeb.throwAt(level, spider, target, VELOCITY, INACCURACY);
		}
	}

	@Override
	public void stop() {
		target = null;
		windUp = 0;
		Silk.stopAnimating(spider);
		restUntil = spider.level().getGameTime() + REST_MIN_TICKS + spider.getRandom().nextInt(REST_SPREAD_TICKS);
	}

	private boolean throwable(LivingEntity candidate) {
		if (candidate == null || !candidate.isAlive()) return false;
		double distance = spider.distanceTo(candidate);
		if (distance < MIN_RANGE || distance > MAX_RANGE) return false;
		// Already caught; another would only be spite.
		if (spider.level().getBlockState(candidate.blockPosition()).is(Blocks.COBWEB)) return false;
		return spider.getSensing().hasLineOfSight(candidate);
	}
}
