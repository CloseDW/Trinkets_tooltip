package closedw.tt;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrinketsTooltip implements ModInitializer {

	public static final String MOD_ID = "trinkets-tooltip";

	// 这个 logger 负责把文本写进控制台和日志文件。
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Trinkets Tooltip loaded");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
