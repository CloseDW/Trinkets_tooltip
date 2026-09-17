package closedw.tt.client.mixin;

import closedw.tt.client.theme.interfaces.TCTPlayerScreenHandlerInterface;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin implements TCTPlayerScreenHandlerInterface {

	@Unique
	private boolean trinketsTooltip$trinketsShow = true;

	@Unique
	private int trinketsTooltip$scrollIndex = 0;

	@Override
	public boolean getTrinketsShow() {
		return this.trinketsTooltip$trinketsShow;
	}

	@Override
	public void setTrinketsShow(boolean val) {
		this.trinketsTooltip$trinketsShow = val;
	}

	@Override
	public int getScrollIndex() {
		return this.trinketsTooltip$scrollIndex;
	}

	@Override
	public void setScrollIndex(int index) {
		this.trinketsTooltip$scrollIndex = index;
	}

	/**
	 * 在饰品上按丢弃键时，把物品拿到光标上，而不是丢到世界里。
	 */
	@Redirect(method = "onSlotClick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/screen/ScreenHandler;internalOnSlotClick(IILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V"))
	private void trinketsTooltip$onSlotClick(ScreenHandler instance, int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		SlotActionType type = actionType;
		if (type == SlotActionType.THROW && trinketsTooltip$isTrinketSlot(slotIndex)) {
			type = SlotActionType.PICKUP;
		}
		((ScreenHandlerInvoker) instance).trinketsTooltip$internalOnSlotClick(slotIndex, button, type, player);
	}

	@Unique
	private boolean trinketsTooltip$isTrinketSlot(int slotIndex) {
		if ((Object) this instanceof TrinketPlayerScreenHandler handler) {
			return slotIndex >= handler.trinkets$getTrinketSlotStart() && slotIndex < handler.trinkets$getTrinketSlotEnd();
		}
		return false;
	}
}
