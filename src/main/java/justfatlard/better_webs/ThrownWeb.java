package justfatlard.better_webs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A ball of web a spider throws: a snowball wearing a cobweb, so every client draws it without
 * knowing this mod. Where it lands it leaves a web that comes apart again a few seconds later.
 */
public final class ThrownWeb {
	/** Marks the snowballs that are webs; an entity tag is saved with the entity, so it outlives a reload. */
	public static final String TAG = Main.MOD_ID + ":thrown_web";

	/** How long a thrown web holds before it comes apart. */
	public static final int LASTS_TICKS = 100;

	/** What a web that could not be left behind does to whoever it hit instead: three seconds of Slowness III. */
	private static final int SLOW_TICKS = 60;
	private static final int SLOW_AMPLIFIER = 2;

	/** For how many ticks of its flight a ball trails a strand back to the spider that threw it. */
	private static final int STRAND_TICKS = 8;

	private record Flight(Snowball ball, LivingEntity thrower) {}

	/** Balls in the air, for their trail. Kept only while they fly; a reload loses the trail, never the web. */
	private static final List<Flight> FLYING = new ArrayList<>();

	private ThrownWeb() {}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> trail());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> FLYING.clear());
	}

	/** A strand back to the spider while the ball is young, then a wisp of silk behind it. */
	private static void trail() {
		Iterator<Flight> flights = FLYING.iterator();
		while (flights.hasNext()) {
			Flight flight = flights.next();
			Snowball ball = flight.ball();
			if (ball.isRemoved() || !(ball.level() instanceof ServerLevel level)) {
				flights.remove();
				continue;
			}
			if (ball.tickCount <= STRAND_TICKS && flight.thrower().isAlive()) {
				Silk.strand(level, Silk.spinnerets(flight.thrower()), ball.position());
			} else {
				Silk.puff(level, ball.position(), 1, 0.02);
			}
		}
	}

	public static void throwAt(ServerLevel level, LivingEntity thrower, LivingEntity target, float velocity, float inaccuracy) {
		Snowball ball = new Snowball(level, thrower, new ItemStack(Items.COBWEB));
		ball.addTag(TAG);
		Vec3 aim = new Vec3(target.getX(), target.getY(0.3), target.getZ()).subtract(ball.position());
		ball.shoot(aim.x, aim.y + aim.horizontalDistance() * 0.15, aim.z, velocity, inaccuracy);
		level.addFreshEntity(ball);
		FLYING.add(new Flight(ball, thrower));
		level.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 0.6F, 0.6F);
		level.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(), SoundEvents.COBWEB_HIT, SoundSource.HOSTILE, 0.8F, 1.3F);
	}

	public static boolean is(Snowball ball) {
		return ball.entityTags().contains(TAG);
	}

	public static void land(Snowball ball, HitResult hit) {
		if (!(ball.level() instanceof ServerLevel level)) return;
		BlockPos at = switch (hit) {
			case EntityHitResult entity -> entity.getEntity().blockPosition();
			case BlockHitResult block -> block.getBlockPos().relative(block.getDirection());
			default -> ball.blockPosition();
		};
		// Snowball's own burst, in the cobweb it carries, and a splash of silk round where it struck.
		level.broadcastEntityEvent(ball, (byte) 3);
		Silk.puff(level, Vec3.atCenterOf(at), 12, 0.35);
		level.playSound(null, at, SoundEvents.COBWEB_PLACE, SoundSource.HOSTILE, 1.0F, 0.9F);

		boolean left = Webs.mayChangeBlocks(level) && leave(level, at);
		if (!left && hit instanceof EntityHitResult entity && entity.getEntity() instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, SLOW_AMPLIFIER), ball.getOwner());
		}
		ball.discard();
	}

	/** Only into empty air, so it never takes the place of grass, a flower or water. */
	private static boolean leave(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir()) return false;
		level.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
		// A scheduled tick is saved with its chunk, so the web still comes apart after an unload or a
		// restart. Vanilla never schedules one on a cobweb, so a placed or generated web is left alone.
		level.scheduleTick(pos, Blocks.COBWEB, LASTS_TICKS);
		return true;
	}
}
