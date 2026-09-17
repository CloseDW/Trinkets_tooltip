package closedw.tt.client.mixin;

import closedw.tt.client.theme.ThemeClient;
import closedw.tt.client.theme.interfaces.TCTPlayerScreenHandlerInterface;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 主题面板位于原版背包矩形之外，所以面板内的点击不能被当成“点在界面外”，
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

	@Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
	private void trinketsTooltip$isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
		if (!(((InventoryScreen) (Object) this).getScreenHandler() instanceof TCTPlayerScreenHandlerInterface tcp)) {
			return;
		}

		if (tcp.getTrinketsShow() && ThemeClient.isMouseOverPanel(tcp, mouseX, mouseY, left, top)) {
			cir.setReturnValue(false);
		}
	}
}
