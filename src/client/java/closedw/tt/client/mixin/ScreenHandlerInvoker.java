package closedw.tt.client.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.20.1 里 {@code ScreenHandler.internalOnSlotClick} 是私有的，
 * 所以 {@link ScreenHandlerMixin} 中的重定向要通过这个 invoker 才能调用到它。
 */
@Mixin(ScreenHandler.class)
public interface ScreenHandlerInvoker {

	@Invoker("internalOnSlotClick")
	void trinketsTooltip$internalOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player);
}
