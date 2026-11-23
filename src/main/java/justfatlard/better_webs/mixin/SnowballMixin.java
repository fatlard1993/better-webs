package justfatlard.better_webs.mixin;

import justfatlard.better_webs.ThrownWeb;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A thrown web lands as a web, not as a snowball: no knock back, nothing a snowball would set off. */
@Mixin(Snowball.class)
public abstract class SnowballMixin {
	@Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
	private void betterwebs$landAsWeb(HitResult hit, CallbackInfo ci) {
		Snowball self = (Snowball) (Object) this;
		if (!ThrownWeb.is(self)) return;
		ThrownWeb.land(self, hit);
		ci.cancel();
	}
}
