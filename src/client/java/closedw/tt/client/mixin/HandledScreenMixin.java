package closedw.tt.client.mixin;

import closedw.tt.client.theme.ThemeClient;
import closedw.tt.client.theme.config.ModConfig;
import closedw.tt.client.theme.interfaces.TCTPlayerScreenHandlerInterface;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.SlotType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在玩家背包旁边绘制 Curios 风格的面板、显示/隐藏按钮，并把滚轮输入转发给 {@link ThemeClient}。
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {

	@Shadow @Final protected T handler;
	@Shadow protected int x;
	@Shadow protected int y;

	@Unique private boolean trinketsTooltip$correct = false;

	/**
	 * 占位构造函数：Mixin 会丢弃私有的 mixin 构造函数，
	 * 它存在只是为了让 javac 能满足 {@link Screen} 的构造函数要求。
	 */
	private HandledScreenMixin(Text title) {
		super(title);
	}

	// 辅助方法

	@Unique
	private boolean trinketsTooltip$isRecipeBookOpen() {
		return trinketsTooltip$correct && ((InventoryScreen) (Object) this).getRecipeBookWidget().isOpen();
	}

	@Unique
	private TCTPlayerScreenHandlerInterface trinketsTooltip$getTcp() {
		return (TCTPlayerScreenHandlerInterface) this.handler;
	}

	// 注入点

	@Inject(at = @At("TAIL"), method = "init")
	private void trinketsTooltip$init(CallbackInfo ci) {
		trinketsTooltip$correct = (Object) this instanceof InventoryScreen;

		if (!trinketsTooltip$correct) {
			return;
		}

		this.addDrawableChild(new TexturedButtonWidget(this.x + 26, this.y + 8, 14, 14, 0, 0, 14,
				ThemeClient.CURIOS_BUTTON_TEXTURE, 14, 28, button -> ThemeClient.toggleCuriosView()));

		ThemeClient.updateSlots(this.handler.slots, trinketsTooltip$getTcp());
	}

	/**
	 * 每当饰品数据同步时 Trinkets 都会重新添加它的槽位，这会重置槽位坐标，所以在画任何东西之前，每帧都重新应用一次布局。
	 */
	@Inject(at = @At("HEAD"), method = "render")
	private void trinketsTooltip$updateSlots(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!trinketsTooltip$correct) {
			return;
		}

		ThemeClient.updateSlots(this.handler.slots, trinketsTooltip$getTcp(), !trinketsTooltip$isRecipeBookOpen());
	}

	@Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V"))
	private void trinketsTooltip$renderPanel(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!trinketsTooltip$correct) {
			return;
		}

		if (!trinketsTooltip$isRecipeBookOpen() && trinketsTooltip$getTcp().getTrinketsShow()) {
			ThemeClient.drawBackground(context, this.x, this.y, trinketsTooltip$getTcp());
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void trinketsTooltip$mouseClickedHead(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (!trinketsTooltip$correct) {
			return;
		}

		TCTPlayerScreenHandlerInterface tcp = trinketsTooltip$getTcp();
		if (button != 0 || !tcp.getTrinketsShow() || trinketsTooltip$isRecipeBookOpen()) {
			return;
		}

		if (ThemeClient.isClickInPageButtons(tcp, mouseX, mouseY, this.x, this.y)) {
			boolean next = ThemeClient.isClickInNextPage(mouseX, this.x);
			ThemeClient.changePage(this.handler.slots, tcp, next ? 1 : -1);
			cir.setReturnValue(true);
			return;
		}

		// 左键点击一个空的饰品槽、且光标上没有物品时，原版什么都不会做
		if (!this.handler.getCursorStack().isEmpty()) {
			return;
		}

		Slot slot = ThemeClient.getHoveredTrinketSlot(this.handler.slots, tcp, mouseX, mouseY, this.x, this.y);
		if (!(slot instanceof TrinketSlot trinketSlot) || slot.hasStack()) {
			return;
		}

		SlotType type = trinketSlot.getType();
		if (type != null && ThemeClient.openSlotLookup(type)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
	private void trinketsTooltip$drawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
		if (!trinketsTooltip$correct) {
			return;
		}

		if (trinketsTooltip$isRecipeBookOpen() && trinketsTooltip$getTcp().getTrinketsShow() && slot instanceof TrinketSlot) {
			ci.cancel();
		}
	}

	/**
	 * 当光标下的饰品槽为空时显示它的名字，这也是 Curios 的做法：
	 * 告诉玩家这个位置该放什么。槽里有物品时则完全保留原本的物品提示。
	 * 这里钩在 {@code drawMouseoverTooltip} 上，而不是依赖 {@code focusedSlot}，
	 * 因为 {@code HandledScreen.render} 只会给 {@code isEnabled()} 为 true 的槽位
	 * 赋值 {@code focusedSlot}，而本模组的槽位位于原版背包矩形之外。已对照 1.20.1
	 * 字节码确认：{@code InventoryScreen.render} 的两个分支汇合之后会无条件调用
	 * {@code drawMouseoverTooltip}，所以对玩家背包来说这个钩子一定会被执行到。
	 */
	@Inject(method = "drawMouseoverTooltip", at = @At("HEAD"))
	private void trinketsTooltip$drawSlotName(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
		if (!trinketsTooltip$correct || trinketsTooltip$isRecipeBookOpen()) {
			return;
		}

		if (!trinketsTooltip$getTcp().getTrinketsShow() || !this.handler.getCursorStack().isEmpty()) {
			return;
		}

		Slot slot = ThemeClient.getHoveredTrinketSlot(this.handler.slots, trinketsTooltip$getTcp(),
				mouseX, mouseY, this.x, this.y);
		if (!(slot instanceof TrinketSlot trinketSlot) || slot.hasStack()) {
			return;
		}

		SlotType type = trinketSlot.getType();
		if (type == null) {
			return;
		}

		Text slotName = ThemeClient.getSlotName(type);
		ThemeClient.reportSlotNameDrawn(type, slotName);
		context.drawTooltip(this.textRenderer, slotName, mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!trinketsTooltip$correct || !ModConfig.pagination || trinketsTooltip$isRecipeBookOpen()
				|| !trinketsTooltip$getTcp().getTrinketsShow()
				|| !ThemeClient.isScrolledInTrinkets(trinketsTooltip$getTcp(), mouseX, mouseY, this.x, this.y)) {
			return false;
		}

		if (amount != 0) {
			ThemeClient.changePage(this.handler.slots, trinketsTooltip$getTcp(), amount > 0 ? 1 : -1);
		}
		return true;
	}
}
