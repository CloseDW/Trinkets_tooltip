package closedw.tt.client.mixin;

import dev.emi.trinkets.CreativeTrinketSlot;
import dev.emi.trinkets.SurvivalTrinketSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创造模式背包会复制饰品槽。
 */
@Mixin(value = CreativeTrinketSlot.class, remap = false, priority = 1500)
public class CreativeTrinketSlotMixin {

	@Inject(at = @At("TAIL"), method = "<init>")
	private void trinketsTooltip$init(SurvivalTrinketSlot original, int index, int x, int y, CallbackInfo ci) {
		CreativeTrinketSlot self = (CreativeTrinketSlot) (Object) this;
		self.x = original.x;
		self.y = original.y;
	}
}
