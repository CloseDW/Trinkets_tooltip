package closedw.tt.client.mixin;

import dev.emi.trinkets.TrinketScreenManager;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trinkets 会在背包旁边画它自己的槽位分组框。主题用 Curios 面板取代了那套界面，
 * 所以这里取消它的分组绘制和选中逻辑。
 */
@Pseudo
@Mixin(value = TrinketScreenManager.class, remap = false, priority = 1500)
public class TrinketScreenManagerMixin {

	@Inject(at = @At("HEAD"), method = "update(FF)V", cancellable = true)
	private static void trinketsTooltip$update(float mouseX, float mouseY, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(at = @At("HEAD"), method = "drawActiveGroup", cancellable = true)
	private static void trinketsTooltip$drawActiveGroup(DrawContext context, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(at = @At("HEAD"), method = "drawExtraGroups", cancellable = true)
	private static void trinketsTooltip$drawExtraGroups(DrawContext context, CallbackInfo ci) {
		ci.cancel();
	}
}
