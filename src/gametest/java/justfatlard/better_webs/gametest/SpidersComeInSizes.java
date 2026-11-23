package justfatlard.better_webs.gametest;

import justfatlard.better_webs.SpiderSizes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;

import java.util.ArrayList;
import java.util.List;

/**
 * Spiders are rolled a size when they first come into a world: ordinary ones smaller than vanilla
 * and varied, cave spiders the smaller kind still, and now and then a big one that is slower and
 * has more health. Each carries the tag that keeps a reloaded spider from being rolled again.
 */
public final class SpidersComeInSizes implements FabricClientGameTest {
	private static final int EACH = 400;

	/** Vanilla's own: a spider walks at 0.3 with 16 health, a cave spider at 0.3 with 12. */
	private static final double SPEED = 0.3;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamerule spawn_mobs false");
			BlockPos at = world.getServer().computeOnServer(s -> world.getConnection().getServerPlayer().blockPosition().above(60));
			world.getServer().runOnServer(s -> {
				ServerLevel level = s.overworld();
				check(level, at, EntityTypes.SPIDER, 0.6, 0.9, 1.25, 1.6, 16);
				check(level, at, EntityTypes.CAVE_SPIDER, 0.75, 1.0, 1.2, 1.4, 12);
			});
		}
	}

	private static <T extends Spider> void check(ServerLevel level, BlockPos at, EntityType<T> type,
			double low, double high, double largeLow, double largeHigh, double health) {
		List<T> spiders = new ArrayList<>();
		for (int i = 0; i < EACH; i++) {
			T spider = type.create(level, EntitySpawnReason.COMMAND);
			spider.setNoAi(true);
			spider.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			level.addFreshEntity(spider);
			spiders.add(spider);
		}
		double total = 0;
		int ordinary = 0;
		int large = 0;
		for (T spider : spiders) {
			if (!spider.entityTags().contains(SpiderSizes.TAG)) throw new AssertionError(type + " came into the world without a size");
			double scale = spider.getAttributeBaseValue(Attributes.SCALE);
			if (SpiderSizes.isLarge(spider)) {
				large++;
				if (scale < largeLow || scale > largeHigh) throw new AssertionError("A big " + type + " was scaled " + scale);
				double speed = spider.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
				if (Math.abs(speed - SPEED * SpiderSizes.LARGE_SPEED) > 1e-6) throw new AssertionError("A big " + type + " walks at " + speed);
				if (Math.abs(spider.getMaxHealth() - health * SpiderSizes.LARGE_HEALTH) > 1e-3 || spider.getHealth() < spider.getMaxHealth()) {
					throw new AssertionError("A big " + type + " has " + spider.getHealth() + " of " + spider.getMaxHealth() + " health");
				}
			} else {
				ordinary++;
				total += scale;
				if (scale < low || scale > high) throw new AssertionError("An ordinary " + type + " was scaled " + scale);
				if (spider.getMaxHealth() != health) throw new AssertionError("An ordinary " + type + " has " + spider.getMaxHealth() + " health");
			}
		}
		double expected = (double) EACH / SpiderSizes.LARGE_ONE_IN;
		if (large < expected / 4 || large > expected * 3) throw new AssertionError(large + " of " + EACH + " " + type + " were big, not about " + expected);
		double mean = total / ordinary;
		if (Math.abs(mean - (low + high) / 2) > 0.03) throw new AssertionError("Ordinary " + type + " average " + mean + " in size");

		spiders.forEach(Spider::discard);
	}
}
