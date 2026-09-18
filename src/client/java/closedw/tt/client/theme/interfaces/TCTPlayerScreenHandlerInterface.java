package closedw.tt.client.theme.interfaces;

/**
 * 由 {@code ScreenHandlerMixin} 挂到每个 {@link net.minecraft.screen.ScreenHandler} 上的主题状态。
 * 饰品槽的位置和数量都从 Trinkets 自己的 {@link dev.emi.trinkets.TrinketPlayerScreenHandler}读取。
 */
public interface TCTPlayerScreenHandlerInterface {

	boolean getTrinketsShow();

	void setTrinketsShow(boolean val);

	int getScrollIndex();

	void setScrollIndex(int index);
}
