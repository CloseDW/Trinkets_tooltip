package closedw.tt.client.theme.interfaces;

/**
 * 由 {@link dev.emi.trinkets.SurvivalTrinketSlot} 实现，让主题能够判断某个饰品槽
 * 是否落在当前这一页可见的范围内。
 */
public interface TCTSurvivalTrinketSlot {

	void setEnabled(boolean b);
}
