package justfatlard.better_webs.mixin;

import justfatlard.better_webs.WebSpinGoal;
import justfatlard.better_webs.WebThrowGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cave spiders inherit registerGoals, so they throw and spin too. */
@Mixin(Spider.class)
public abstract class SpiderMixin extends Monster {
	protected SpiderMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void betterwebs$addGoals(CallbackInfo ci) {
		Spider self = (Spider) (Object) this;
		// Level with the leap, above the bite, so it can stop a chase to throw but never cuts a leap short.
		this.goalSelector.addGoal(3, new WebThrowGoal(self));
		// Level with wandering, so an idle spider does one or the other.
		this.goalSelector.addGoal(5, new WebSpinGoal(self));
	}
}
