package justfatlard.better_webs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;

/**
 * Spiders come in sizes: most a good deal smaller than vanilla's, no two quite alike, cave spiders
 * still the smaller kind, and now and then a big one, slow and hard to kill.
 *
 * <p>Rolled once, the first time a spider is loaded into a world, and kept: the size and the rest
 * live in its attributes, which are saved with it, and a tag says it has been rolled.
 */
public final class SpiderSizes {
	public static final String TAG = Main.MOD_ID + ":sized";

	/** One spider in this many is a big one. */
	public static final int LARGE_ONE_IN = 25;

	public static final double LARGE_SPEED = 0.75;
	public static final double LARGE_HEALTH = 2.5;

	private SpiderSizes() {}

	static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Spider spider && !spider.entityTags().contains(TAG)) roll(spider);
		});
	}

	public static void roll(Spider spider) {
		spider.addTag(TAG);
		RandomSource random = spider.getRandom();
		boolean cave = spider instanceof CaveSpider;
		if (random.nextInt(LARGE_ONE_IN) == 0) {
			scale(spider, Attributes.SCALE, cave ? between(random, 1.2, 1.4) : between(random, 1.25, 1.6));
			scale(spider, Attributes.MOVEMENT_SPEED, LARGE_SPEED);
			scale(spider, Attributes.MAX_HEALTH, LARGE_HEALTH);
			spider.setHealth(spider.getMaxHealth());
			return;
		}
		// Two rolls averaged, so most sit near the middle of the range and few at its ends.
		double middle = (random.nextDouble() + random.nextDouble()) / 2;
		scale(spider, Attributes.SCALE, cave ? 0.75 + 0.25 * middle : 0.6 + 0.3 * middle);
	}

	private static double between(RandomSource random, double low, double high) {
		return low + (high - low) * random.nextDouble();
	}

	private static void scale(Spider spider, Holder<Attribute> attribute, double by) {
		AttributeInstance instance = spider.getAttribute(attribute);
		if (instance != null) instance.setBaseValue(instance.getBaseValue() * by);
	}

	/** No ordinary spider is rolled bigger than vanilla's own size, and no big one smaller. */
	public static boolean isLarge(Spider spider) {
		return spider.getAttributeBaseValue(Attributes.SCALE) > 1.1;
	}
}
