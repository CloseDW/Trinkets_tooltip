package closedw.tt.client.mixin;

import closedw.tt.client.theme.CuriosPanelButton;
import closedw.tt.client.theme.ThemeClient;
import closedw.tt.client.theme.TrinketRenderToggle;
import closedw.tt.client.theme.interfaces.TCTPlayerScreenHandlerInterface;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在玩家背包旁边绘制 Curios 风格的面板、显示/隐藏按钮和（可选的）左侧滚动条，
 * 并把滚轮、滑块拖动这些输入转发给 {@link ThemeClient}。
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {

	@Shadow @Final protected T handler;
	@Shadow protected int x;
	@Shadow protected int y;

	@Unique private boolean trinketsTooltip$correct = false;

	/** 正按着左侧滚动条的滑块（或轨道）拖动。 */
	@Unique private boolean trinketsTooltip$scrollerDragging = false;

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

	/**
	 * 鼠标正停在某个饰品槽的渲染开关图标上时返回那个槽位，否则返回 {@code null}。
	 */
	@Unique
	@Nullable
	private Slot trinketsTooltip$getHoveredRenderToggle(double mouseX, double mouseY) {
		return TrinketRenderToggle.getHoveredIcon(this.handler.slots, trinketsTooltip$getTcp(), mouseX, mouseY,
				this.x, this.y);
	}

	// 注入点

	@Inject(at = @At("TAIL"), method = "init")
	private void trinketsTooltip$init(CallbackInfo ci) {
		trinketsTooltip$correct = (Object) this instanceof InventoryScreen;

		if (!trinketsTooltip$correct) {
			return;
		}

		// 按钮贴图跟着面板状态走，见 CuriosPanelButton。
		this.addDrawableChild(new CuriosPanelButton(this.x + 26, this.y + 8, 14, 14, trinketsTooltip$getTcp()));

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

		// Curios 的渲染开关是一枚独立按钮，比槽位本身优先拿到点击；这里也一样，
		// 手上拿着物品时也能点，点了之后这次点击不再交给槽位处理。
		Slot toggle = trinketsTooltip$getHoveredRenderToggle(mouseX, mouseY);
		if (toggle != null) {
			TrinketRenderToggle.toggle(toggle);
			cir.setReturnValue(true);
			return;
		}

		// 左侧滚动条：在轨道任意位置按下都开始拖滑块（1.19 原版就是这样），这一次点击同样不再往下传。
		if (ThemeClient.isMouseOverScrollbar(tcp, mouseX, mouseY, this.x, this.y)) {
			trinketsTooltip$scrollerDragging = true;
			ThemeClient.dragScrollbarTo(this.handler.slots, tcp, mouseY, this.y);
			cir.setReturnValue(true);
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
	 * 画饰品槽右上角那枚「启用饰品渲染」开关，做法照 Curios 的 RenderButton
	 * （具体位置由 {@link TrinketRenderToggle} 里的偏移常量决定，比 Curios 再靠左 2 像素，
	 * 免得被右边相邻的槽位盖住）。
     * 挂在 {@code drawSlot} 的 TAIL 上，是想让图标盖在槽位里的物品图标之上 ——
	 * Curios 也是在画完槽位之后才把这些开关覆盖上去的，而原版画槽位时已经关掉了深度测试
	 * （{@code HandledScreen.render} 里的 {@code RenderSystem.disableDepthTest()}），
	 * 所以后画的图标就在上层。
	 * 注意坐标系：{@code HandledScreen.render} 在开始循环画槽位之前，已经把矩阵平移到了
	 * 界面左上角，所以 {@code drawSlot} 里一律用槽位的 GUI 相对坐标，图标位置也一样
	 * （{@link TrinketRenderToggle#drawIcon} 里不再加 this.x/this.y）。
	 */
	@Inject(method = "drawSlot", at = @At("TAIL"))
	private void trinketsTooltip$drawRenderToggle(DrawContext context, Slot slot, CallbackInfo ci) {
		if (!trinketsTooltip$correct || !(slot.inventory instanceof TrinketInventory)) {
			return;
		}

		TrinketRenderToggle.drawIcon(context, slot);
	}

	/**
	 * 和 Curios 一样：鼠标停在渲染开关上时，这一格不算「悬停在槽位上」。
	 * 这样物品提示框、槽位高亮和空槽位的槽位名提示都会让位给开关自己的提示
	 * （见 {@link #trinketsTooltip$drawSlotName}）。
	 * 原版 {@code HandledScreen.isPointOverSlot} 是私有方法，直接把 HEAD 钩进去即可；
	 * {@code InventoryScreen} 里对 {@code isPointWithinBounds} 的覆写不受影响。
	 */
	@Inject(method = "isPointOverSlot", at = @At("HEAD"), cancellable = true)
	private void trinketsTooltip$isPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
		if (!trinketsTooltip$correct || !slot.isEnabled() || !(slot.inventory instanceof TrinketInventory)) {
			return;
		}

		if (TrinketRenderToggle.isMouseOverIcon(slot, pointX, pointY, this.x, this.y)) {
			cir.setReturnValue(false);
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

		// 开关图标自己也有一条提示，命中它时就不再画槽位名了（物品提示框已经被
		// isPointOverSlot 那边拦掉了，所以这里画什么就是玩家看到的唯一一条）。
		Slot toggle = trinketsTooltip$getHoveredRenderToggle(mouseX, mouseY);
		if (toggle != null) {
			context.drawTooltip(this.textRenderer, TrinketRenderToggle.TOOLTIP, mouseX, mouseY);
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

	/**
	 * 拖动滚动条的滑块。滑块的位置只看鼠标的 Y，所以往两边拖出轨道也照样跟手。
	 * 1.20.1 的 {@code HandledScreen} 也自己声明了 {@code mouseDragged} 和 {@code mouseReleased}，
	 * 不像 {@code mouseScrolled} 那样只有父类实现，所以这里只能注入，不能直接覆写。
	 */
	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void trinketsTooltip$mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY,
			CallbackInfoReturnable<Boolean> cir) {
		if (!trinketsTooltip$scrollerDragging) {
			return;
		}

		if (!trinketsTooltip$correct || !trinketsTooltip$getTcp().getTrinketsShow()) {
			trinketsTooltip$scrollerDragging = false;
			return;
		}

		ThemeClient.dragScrollbarTo(this.handler.slots, trinketsTooltip$getTcp(), mouseY, this.y);
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"))
	private void trinketsTooltip$mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		trinketsTooltip$scrollerDragging = false;
	}

	/**
	 * 滚轮：滚动条模式一格一行，分页模式一格一页，方向都按原版的约定（往上 = 往前看）。
	 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!trinketsTooltip$correct || trinketsTooltip$isRecipeBookOpen()
				|| !trinketsTooltip$getTcp().getTrinketsShow()
				|| !ThemeClient.isScrolledInTrinkets(trinketsTooltip$getTcp(), mouseX, mouseY, this.x, this.y)) {
			return false;
		}

		if (amount != 0) {
			TCTPlayerScreenHandlerInterface tcp = trinketsTooltip$getTcp();
			// 滚轮往上（amount > 0）就是往前看：两种模式都按原版的约定来。
			// 滚动条模式滚一行，分页模式翻一页。
			int step = amount > 0 ? -1 : 1;

			if (ThemeClient.isScrollbarMode()) {
				ThemeClient.scrollRows(this.handler.slots, tcp, step);
			} else {
				ThemeClient.changePage(this.handler.slots, tcp, step);
			}
		}
		return true;
	}
}
