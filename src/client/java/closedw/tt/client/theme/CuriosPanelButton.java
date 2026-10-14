package closedw.tt.client.theme;

import closedw.tt.client.theme.interfaces.TCTPlayerScreenHandlerInterface;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TexturedButtonWidget;

/**
 * 面板显示/隐藏按钮（玩家预览左上角那枚）。三张贴图各有分工：
 * {@code inventory.png} 上半张 —— 面板关着；
 * {@code curios_button_open.png} —— 面板开着；
 * {@code inventory.png} 下半张 —— 鼠标悬停时的反馈。
 * 结构与 Curios 的 {@code CosmeticButton} 相同 —— 先用状态挑图，再挑悬停那张
 * （1.20 的 Curios 正是用 {@code xTex} 表示状态、{@code yTex} 表示 hover/focus）。
 * 悬停判定特意只用 {@link #isHovered()}，不用原版的 {@code isSelected()}：后者是
 * 「悬停或键盘焦点」，而点一下之后焦点会留在按钮上，高亮就会"卡"住不动，
 * 直到界面重建 —— Curios 那边点一下会换掉整个界面、按钮跟着重建，所以看不出来；
 * 本模组只切面板、界面不动，才会暴露成"移开鼠标也不变回去"。
 */
public class CuriosPanelButton extends TexturedButtonWidget {

	/** 双帧贴图 inventory.png 是 14x28：上半张普通、下半张悬停。 */
	private static final int TEXTURE_WIDTH = 14;
	private static final int TEXTURE_HEIGHT = 28;
	private static final int HIGHLIGHTED_V = 14;
	/** curios_button_open.png 是单张 14x14。 */
	private static final int OPEN_TEXTURE_SIZE = 14;

	private final TCTPlayerScreenHandlerInterface tcp;

	public CuriosPanelButton(int x, int y, int width, int height, TCTPlayerScreenHandlerInterface tcp) {
		super(x, y, width, height, 0, 0, HIGHLIGHTED_V, ThemeClient.CURIOS_BUTTON_TEXTURE, TEXTURE_WIDTH,
				TEXTURE_HEIGHT, button -> ThemeClient.toggleCuriosView());
		this.tcp = tcp;
	}

	@Override
	public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
		// 悬停优先，其次看面板状态。
		if (!this.isHovered() && this.tcp.getTrinketsShow()) {
			context.drawTexture(ThemeClient.CURIOS_BUTTON_OPEN, this.getX(), this.getY(), 0, 0,
					this.getWidth(), this.getHeight(), OPEN_TEXTURE_SIZE, OPEN_TEXTURE_SIZE);
			return;
		}

		context.drawTexture(ThemeClient.CURIOS_BUTTON_TEXTURE, this.getX(), this.getY(), 0,
				this.isHovered() ? HIGHLIGHTED_V : 0, this.getWidth(), this.getHeight(),
				TEXTURE_WIDTH, TEXTURE_HEIGHT);
	}
}
