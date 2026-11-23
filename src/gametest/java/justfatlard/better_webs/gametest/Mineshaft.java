package justfatlard.better_webs.gametest;

import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Pictures of the webs a mineshaft generates with, the thickest of them round a cave spider
 * spawner, as this mod draws them.
 *
 * <p>Its own world, and a real one: a mineshaft sinks itself well under sea level, which in the
 * flat world the other tests use is under the bottom of the world. The seed is fixed so the same
 * mineshaft is found every run.
 */
public final class Mineshaft implements FabricClientGameTest {
	private static final String SEED = "better-webs";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed(SEED))
				.create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.waitFor(s -> PandoricalApi.isAvailable(connection.getServerPlayer()));
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamemode spectator @a");
			server.runCommand("effect give @a minecraft:night_vision infinite 0 true");

			List<BlockPos[]> views = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockPos found = level.findNearestMapStructure(StructureTags.MINESHAFT, connection.getServerPlayer().blockPosition(), 100, false);
				if (found == null) throw new AssertionError("No mineshaft within reach of seed " + SEED);
				return views(level, found);
			});
			if (views.isEmpty()) throw new AssertionError("The mineshaft found has no webs to show");
			for (int i = 0; i < views.size(); i++) {
				BlockPos camera = views.get(i)[0];
				BlockPos at = views.get(i)[1];
				look(server, camera.getX() + 0.5, camera.getY(), camera.getZ() + 0.5, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
				context.waitTicks(80);
				connection.waitForChunksRender();
				context.takeScreenshot(TestScreenshotOptions.of("webs-mineshaft-" + (i + 1)).withSize(1920, 1080).disableCounterPrefix());
			}
		}
	}

	/** Where to stand and what to look at: the spawner if there is one, and the thickest knots of webs, well apart. */
	private static List<BlockPos[]> views(ServerLevel level, BlockPos around) {
		BlockPos spawner = null;
		List<BlockPos> webs = new ArrayList<>();
		List<Integer> counts = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(around.offset(-48, 0, -48).atY(-50), around.offset(48, 0, 48).atY(50))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.SPAWNER)) spawner = pos.immutable();
			if (!state.is(Blocks.COBWEB)) continue;
			int count = 0;
			for (BlockPos near : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
				if (level.getBlockState(near).is(Blocks.COBWEB)) count++;
			}
			webs.add(pos.immutable());
			counts.add(count);
		}
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < webs.size(); i++) order.add(i);
		order.sort((a, b) -> counts.get(b) - counts.get(a));
		List<BlockPos> targets = new ArrayList<>();
		if (spawner != null) targets.add(spawner);
		for (int i : order) {
			BlockPos web = webs.get(i);
			if (targets.size() >= 3) break;
			if (targets.stream().allMatch(taken -> taken.distSqr(web) > 144)) targets.add(web);
		}

		List<BlockPos[]> views = new ArrayList<>();
		for (BlockPos target : targets) {
			BlockPos camera = stand(level, target);
			if (camera != null) views.add(new BlockPos[] {camera, target});
		}
		return views;
	}

	/** Somewhere open a few blocks off, with nothing solid between there and the target, to look from. */
	private static BlockPos stand(ServerLevel level, BlockPos target) {
		Vec3 aim = Vec3.atCenterOf(target);
		for (int distance : new int[] {5, 4, 6, 3, 7}) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) continue;
					for (int dy = -1; dy <= 1; dy++) {
						BlockPos feet = target.offset(dx * distance, dy - 1, dz * distance);
						if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
						Vec3 eye = Vec3.atBottomCenterOf(feet).add(0, 1.62, 0);
						BlockHitResult hit = level.clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
						if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target)) return feet;
					}
				}
			}
		}
		return null;
	}

	private static void look(TestServerContext server, double x, double y, double z, double atX, double atY, double atZ) {
		double dx = atX - x;
		double dy = atY - (y + 1.62);
		double dz = atZ - z;
		double yaw = -Math.toDegrees(Math.atan2(dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand("tp @a %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
	}
}
