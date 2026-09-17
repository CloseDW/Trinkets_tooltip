package closedw.tt.client;

import closedw.tt.client.theme.ThemeClient;
import net.fabricmc.api.ClientModInitializer;

public class TrinketsTooltipClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ThemeClient.init();
	}
}
