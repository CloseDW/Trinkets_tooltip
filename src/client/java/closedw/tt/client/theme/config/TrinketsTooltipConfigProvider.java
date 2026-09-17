package closedw.tt.client.theme.config;

import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import closedw.tt.TrinketsTooltip;

import java.util.Set;

/**
 * 通过 fabric.mod.json 里的 {@code custom.configured.providers} 交给 Configured。
 */
public class TrinketsTooltipConfigProvider implements IModConfigProvider {

	@Override
	public Set<IModConfig> getConfigurationsForMod(ModContext context) {
		if (!TrinketsTooltip.MOD_ID.equals(context.modId())) {
			return Set.of();
		}

		return Set.of(TrinketsTooltipConfig.get());
	}
}
