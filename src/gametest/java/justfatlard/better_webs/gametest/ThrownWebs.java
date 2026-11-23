package justfatlard.better_webs.gametest;

import justfatlard.better_webs.ThrownWeb;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

/**
 * A web a spider throws flies, leaves a cobweb where it lands, and that cobweb comes apart by
 * itself a few seconds later without leaving string. With mob griefing off it leaves no block,
 * and slows whoever it hit instead.
 */
public final class ThrownWebs implements FabricClientGameTest {
	private static final int RANGE = 8;
	private static final int FLIGHT_LIMIT_TICKS = 60;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			BlockPos origin = server.computeOnServer(s -> world.getConnection().getServerPlayer().blockPosition().above(40));
			BlockPos targetAt = origin.east(RANGE);

			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(RANGE + 2, -1, 2))) {
					level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
				}
				Spider spider = spawn(level, EntityTypes.SPIDER, origin);
				spider.setNoAi(true);
				ArmorStand target = spawn(level, EntityTypes.ARMOR_STAND, targetAt);
				ThrownWeb.throwAt(level, spider, target, 1.2F, 0.0F);
			});

			BlockPos landed = null;
			for (int waited = 0; waited < FLIGHT_LIMIT_TICKS && landed == null; waited++) {
				context.waitTick();
				landed = server.computeOnServer(s -> webNear(s.overworld(), targetAt));
			}
			if (landed == null) throw new AssertionError("No web landed within a block of the target " + RANGE + " blocks off");

			context.waitTicks(ThrownWeb.LASTS_TICKS + 5);
			BlockPos web = landed;
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				if (level.getBlockState(web).is(Blocks.COBWEB)) throw new AssertionError("The thrown web at " + web + " never came apart");
				boolean string = !level.getEntitiesOfClass(ItemEntity.class, new AABB(web).inflate(2),
					item -> item.getItem().is(Items.STRING)).isEmpty();
				if (string) throw new AssertionError("A thrown web coming apart left string to farm");
			});

			server.runCommand("gamerule mob_griefing false");
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				ArmorStand target = level.getEntitiesOfClass(ArmorStand.class, new AABB(targetAt).inflate(2)).getFirst();
				Snowball ball = new Snowball(level, target.getX(), target.getY() + 3, target.getZ(), new ItemStack(Items.COBWEB));
				ball.addTag(ThrownWeb.TAG);
				level.addFreshEntity(ball);
				ThrownWeb.land(ball, new EntityHitResult(target));
				if (level.getBlockState(target.blockPosition()).is(Blocks.COBWEB)) {
					throw new AssertionError("A thrown web left a block with mob griefing off");
				}
				if (!target.hasEffect(MobEffects.SLOWNESS)) {
					throw new AssertionError("A thrown web that could leave no block did not slow what it hit");
				}
				if (!ball.isRemoved()) throw new AssertionError("A thrown web stayed in the world after landing");
			});
		}
	}

	private static <T extends LivingEntity> T spawn(ServerLevel level, EntityType<T> type, BlockPos at) {
		T entity = type.create(level, EntitySpawnReason.COMMAND);
		if (entity == null) throw new AssertionError("Could not make a " + type);
		entity.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		level.addFreshEntity(entity);
		return entity;
	}

	private static BlockPos webNear(ServerLevel level, BlockPos at) {
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 2, 1))) {
			if (level.getBlockState(pos).is(Blocks.COBWEB)) return pos.immutable();
		}
		return null;
	}
}
