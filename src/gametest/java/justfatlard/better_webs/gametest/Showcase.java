package justfatlard.better_webs.gametest;

import justfatlard.better_webs.Silk;
import justfatlard.better_webs.ThrownWeb;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.AABB;

/**
 * The pictures for the readme and the mod page: a stone room webbed the way spiders leave one,
 * with corners, a run along the floor, a curtain between two pillars, a web between fence posts,
 * and one hanging alone that keeps vanilla's cross; a doorway and a window full of web; and a dark
 * cave lit by one lantern, since that is where webs mostly are.
 *
 * <p>Run it under xvfb-run; the frames land in build/run/clientGameTest/screenshots.
 */
public final class Showcase implements FabricClientGameTest {
	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.waitFor(s -> PandoricalApi.isAvailable(connection.getServerPlayer()));

			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule mob_griefing false");
			server.runCommand("time set noon");
			server.runCommand("gamemode spectator @a");

			BlockPos origin = server.computeOnServer(s -> connection.getServerPlayer().blockPosition());
			int x = origin.getX();
			int y = origin.getY() + 20;
			int z = origin.getZ();

			// A room open toward the camera: floor, ceiling, back wall and two side walls.
			fill(server, x - 8, y - 1, z - 10, x + 8, y - 1, z, "minecraft:cobblestone");
			fill(server, x - 8, y + 5, z - 10, x + 8, y + 5, z - 4, "minecraft:stone_bricks");
			fill(server, x - 8, y, z - 10, x + 8, y + 4, z - 10, "minecraft:mossy_stone_bricks");
			fill(server, x - 8, y, z - 10, x - 8, y + 4, z, "minecraft:stone_bricks");
			fill(server, x + 8, y, z - 10, x + 8, y + 4, z, "minecraft:stone_bricks");

			// Upper left corner: a wedge of webs filling where the ceiling meets two walls.
			fill(server, x - 7, y + 4, z - 9, x - 5, y + 4, z - 9, "minecraft:cobweb");
			fill(server, x - 7, y + 3, z - 9, x - 6, y + 3, z - 9, "minecraft:cobweb");
			fill(server, x - 7, y + 2, z - 9, x - 7, y + 2, z - 9, "minecraft:cobweb");
			fill(server, x - 7, y + 4, z - 8, x - 7, y + 4, z - 7, "minecraft:cobweb");

			// A run along the floor where it meets the back wall.
			fill(server, x - 3, y, z - 9, x + 2, y, z - 9, "minecraft:cobweb");

			// A curtain between two pillars, floor to ceiling.
			fill(server, x + 3, y, z - 6, x + 3, y + 4, z - 6, "minecraft:spruce_log");
			fill(server, x + 7, y, z - 6, x + 7, y + 4, z - 6, "minecraft:spruce_log");
			fill(server, x + 4, y + 2, z - 6, x + 6, y + 4, z - 6, "minecraft:cobweb");

			// A doorway in a wall of its own, filled with web, and a window in the right-hand wall.
			fill(server, x - 8, y, z - 1, x - 4, y + 4, z - 1, "minecraft:stone_bricks");
			fill(server, x - 7, y, z - 1, x - 5, y + 2, z - 1, "minecraft:cobweb");
			fill(server, x + 8, y + 1, z - 4, x + 8, y + 2, z - 3, "minecraft:cobweb");

			// Between two fence posts, and one floating free that keeps vanilla's cross.
			fill(server, x - 4, y, z - 4, x - 4, y + 1, z - 4, "minecraft:oak_fence");
			fill(server, x - 2, y, z - 4, x - 2, y + 1, z - 4, "minecraft:oak_fence");
			fill(server, x - 3, y + 1, z - 4, x - 3, y + 1, z - 4, "minecraft:cobweb");
			fill(server, x, y + 2, z - 6, x, y + 2, z - 6, "minecraft:cobweb");

			// Held on one side only: lying on the floor, on the right-hand wall, and hanging from the ceiling.
			fill(server, x - 1, y, z - 2, x - 1, y, z - 2, "minecraft:cobweb");
			fill(server, x + 7, y + 2, z - 2, x + 7, y + 2, z - 2, "minecraft:cobweb");
			fill(server, x + 1, y + 4, z - 5, x + 1, y + 4, z - 5, "minecraft:cobweb");

			// The back right corner of the room, floor to ceiling, and a run along the ceiling's edge.
			fill(server, x + 7, y, z - 9, x + 7, y + 4, z - 9, "minecraft:cobweb");
			fill(server, x - 2, y + 4, z - 9, x + 2, y + 4, z - 9, "minecraft:cobweb");

			// A curtain too wide for one spider's orb, hanging from the ceiling with a frayed edge.
			fill(server, x - 3, y + 3, z - 7, x + 1, y + 4, z - 7, "minecraft:cobweb");

			// A den: a clump of webs in the open, three by two by two.
			fill(server, x - 6, y, z - 6, x - 4, y + 1, z - 5, "minecraft:cobweb");

			look(server, x + 0.5, y + 1.0, z + 3.5, x + 0.5, y + 2.0, z - 8.0);
			context.waitTicks(100);
			connection.waitForChunksRender();
			shoot(context, "webs-room");

			look(server, x + 6.5, y + 3.5, z - 1.5, x - 4.0, y + 2.0, z - 8.0);
			context.waitTicks(40);
			connection.waitForChunksRender();
			shoot(context, "webs-corner");

			look(server, x + 1.0, y + 1.5, z - 1.5, x + 5.5, y + 2.5, z - 6.0);
			context.waitTicks(40);
			connection.waitForChunksRender();
			shoot(context, "webs-curtain");

			look(server, x + 4.5, y + 0.0, z - 7.5, x + 7.5, y + 2.0, z - 9.5);
			context.waitTicks(40);
			connection.waitForChunksRender();
			shoot(context, "webs-room-corner");

			look(server, x - 6.0, y + 0.0, z + 4.0, x - 6.0, y + 1.5, z - 1.0);
			context.waitTicks(40);
			connection.waitForChunksRender();
			shoot(context, "webs-doorway");

			look(server, x + 4.0, y + 0.5, z - 3.0, x + 8.0, y + 2.0, z - 3.0);
			context.waitTicks(40);
			connection.waitForChunksRender();
			shoot(context, "webs-window");

			spiders(context, server, connection, x - 30, y, z);

			cave(server, x + 30, y, z);
			server.runCommand("time set midnight");
			look(server, x + 30.5, y + 0.0, z + 0.5, x + 30.5, y + 1.5, z - 8.0);
			context.waitTicks(60);
			connection.waitForChunksRender();
			shoot(context, "webs-cave");
		}
	}

	/**
	 * A spider throwing a web at an armour stand and another spinning one into the corner of a wall,
	 * caught part way through each. Both are set going by hand, since a spider only does either now
	 * and then, through the same calls its goals make.
	 */
	private static void spiders(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, int x, int y, int z) {
		// A thrown web only leaves a block where mobs may change blocks.
		server.runCommand("gamerule mob_griefing true");
		fill(server, x - 8, y - 1, z - 8, x + 10, y - 1, z + 4, "minecraft:moss_block");
		fill(server, x - 8, y, z - 8, x + 10, y + 3, z - 8, "minecraft:mossy_cobblestone");
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			Spider thrower = EntityTypes.SPIDER.create(level, EntitySpawnReason.COMMAND);
			thrower.setNoAi(true);
			thrower.snapTo(x + 0.5, y, z + 0.5, -90, 0);
			thrower.setYHeadRot(-90);
			level.addFreshEntity(thrower);
			ArmorStand target = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
			target.snapTo(x + 8.5, y, z + 0.5, 90, 0);
			level.addFreshEntity(target);
			Spider spinner = EntityTypes.SPIDER.create(level, EntitySpawnReason.COMMAND);
			spinner.setNoAi(true);
			// Its back to the wall it is spinning against, as the goal turns it.
			spinner.snapTo(x - 4.5, y, z - 5.5, 0, 0);
			spinner.setYHeadRot(0);
			level.addFreshEntity(spinner);
		});
		look(server, x + 4.5, y + 1.2, z + 7.5, x + 4.5, y + 0.4, z);
		// A real spider is in view long before it throws; give the clients a moment to know of these.
		context.waitTicks(20);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			for (Spider spider : level.getEntitiesOfClass(Spider.class, new AABB(x - 8, y - 1, z - 8, x + 2, y + 3, z + 2))) {
				Silk.animate(spider, spider.getX() > x - 2 ? Silk.THROW_ANIMATION : Silk.SPIN_ANIMATION, spider.getX() <= x - 2);
			}
		});
		context.waitTicks(8);
		shoot(context, "spider-rear");

		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			Spider thrower = level.getEntitiesOfClass(Spider.class, new AABB(x - 1, y - 1, z - 1, x + 2, y + 2, z + 2)).getFirst();
			ArmorStand target = level.getEntitiesOfClass(ArmorStand.class, new AABB(x + 7, y - 1, z - 1, x + 10, y + 3, z + 2)).getFirst();
			ThrownWeb.throwAt(level, thrower, target, 1.2F, 0.0F);
		});
		context.waitTicks(4);
		shoot(context, "spider-throw");
		context.waitTicks(30);
		shoot(context, "spider-hit");

		BlockPos spot = new BlockPos(x - 5, y, z - 7);
		look(server, x - 1.0, y + 0.3, z - 5.0, x - 4.5, y + 0.4, z - 6.3);
		for (int i = 0; i < 4; i++) {
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				Spider spinner = level.getEntitiesOfClass(Spider.class, new AABB(spot).inflate(3)).getFirst();
				Silk.spinStrand(level, spinner, spot);
			});
			context.waitTicks(3);
		}
		shoot(context, "spider-spin");

		// A row of them as they come: six spiders, three cave spiders, and one big one made so.
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			for (int i = 0; i < 10; i++) {
				Spider spider = (i < 6 ? EntityTypes.SPIDER : i < 9 ? EntityTypes.CAVE_SPIDER : EntityTypes.SPIDER).create(level, EntitySpawnReason.COMMAND);
				spider.setNoAi(true);
				spider.snapTo(x - 6.5 + i * 1.7, y, z + 2.5, 180, 0);
				spider.setYHeadRot(180);
				level.addFreshEntity(spider);
				if (i == 9) spider.getAttribute(Attributes.SCALE).setBaseValue(1.5);
			}
		});
		look(server, x + 1.5, y + 1.5, z + 9.5, x + 1.5, y + 0.3, z + 2.5);
		context.waitTicks(20);
		shoot(context, "spider-sizes");
	}

	/**
	 * A sealed stone chamber lit by one lantern, webbed the way a spider's cave is: a passage across
	 * the back with an orb in it, its corners and the foot of its walls webbed, a curtain hanging
	 * from the ceiling, and a clump on the floor.
	 */
	private static void cave(TestServerContext server, int x, int y, int z) {
		fill(server, x - 6, y - 1, z - 12, x + 6, y + 6, z + 2, "minecraft:stone");
		fill(server, x - 5, y, z - 10, x + 5, y + 4, z + 1, "minecraft:air");
		// The passage out of the back: a gap two wide and two high in the far wall, webbed across.
		fill(server, x - 1, y, z - 11, x, y + 1, z - 11, "minecraft:cobweb");
		fill(server, x - 1, y, z - 12, x, y + 1, z - 12, "minecraft:air");
		// The back corners floor to ceiling, and the foot of the left wall.
		fill(server, x - 5, y, z - 10, x - 5, y + 4, z - 10, "minecraft:cobweb");
		fill(server, x + 5, y, z - 10, x + 5, y + 4, z - 10, "minecraft:cobweb");
		fill(server, x - 5, y, z - 8, x - 5, y, z - 3, "minecraft:cobweb");
		// Where the ceiling meets the right wall, and a curtain hanging in the middle of the room.
		fill(server, x + 5, y + 4, z - 8, x + 5, y + 4, z - 3, "minecraft:cobweb");
		fill(server, x - 3, y + 3, z - 6, x + 2, y + 4, z - 6, "minecraft:cobweb");
		// A clump on the floor, one on the ceiling, and the lantern.
		fill(server, x + 3, y, z - 3, x + 3, y, z - 3, "minecraft:cobweb");
		fill(server, x - 2, y + 4, z - 2, x - 2, y + 4, z - 2, "minecraft:cobweb");
		fill(server, x + 2, y, z - 8, x + 2, y, z - 8, "minecraft:lantern");
	}

	private static void fill(TestServerContext server, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
		server.runCommand("fill %d %d %d %d %d %d %s".formatted(x1, y1, z1, x2, y2, z2, block));
	}

	/**
	 * Stand the camera at one place and point it at another. The camera's y is the feet, so it
	 * looks from 1.62 above where it stands.
	 */
	private static void look(TestServerContext server, double x, double y, double z,
			double atX, double atY, double atZ) {
		double dx = atX - x;
		double dy = atY - (y + 1.62);
		double dz = atZ - z;
		double yaw = -Math.toDegrees(Math.atan2(dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand("tp @a %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
	}

	private static void shoot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name)
			.withSize(WIDTH, HEIGHT)
			.disableCounterPrefix());
	}
}
