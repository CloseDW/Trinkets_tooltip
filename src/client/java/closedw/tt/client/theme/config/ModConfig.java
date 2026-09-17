package closedw.tt.client.theme.config;

import closedw.tt.TrinketsTooltip;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 游戏代码读取的选项值，以及它们背后的属性文件。
 * 这个类刻意不引用任何 Configured 的类型：Configured 是可选依赖，
 * 如果在一个实现了它接口的类里做这件事，没装 Configured 时就会抛
 * {@link NoClassDefFoundError}。{@link TrinketsTooltipConfig} 是面向 Configured 的
 * 适配层，所有文件读写都委托到这里。
 */
public class ModConfig {

	public static final String FILE_NAME = "trinkets-tooltip.properties";
	private static final Logger LOGGER = LoggerFactory.getLogger(TrinketsTooltip.MOD_ID);

	public static final int MAX_HEIGHT_MIN = 2;
	public static final int MAX_HEIGHT_MAX = 7;
	public static final int MIN_WIDTH_MIN = 1;
	public static final int MIN_WIDTH_MAX = 4;

	public static boolean pagination = true;
	public static boolean scrolling_outside_boundary = false;
	public static int max_height = 7;
	public static int min_width = 1;
	public static boolean jei_slot_lookup = true;

	public static Path getPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	/** 供 Configured 适配层使用的按键读取。 */
	public static boolean getBoolean(String key) {
		return switch (key) {
			case "pagination" -> pagination;
			case "scrolling_outside_boundary" -> scrolling_outside_boundary;
			case "jei_slot_lookup" -> jei_slot_lookup;
			default -> false;
		};
	}

	public static int getInt(String key) {
		return switch (key) {
			case "max_height" -> max_height;
			case "min_width" -> min_width;
			default -> 0;
		};
	}

	/** 读取属性文件（如果存在）。没装 Configured 时调用也是安全的。 */
	public static void load() {
		Properties props = new Properties();
		Path path = getPath();

		if (Files.exists(path)) {
			try (InputStream in = Files.newInputStream(path)) {
				props.load(in);
			} catch (IOException e) {
				LOGGER.warn("Failed to load config from {}", path, e);
			}
		}

		pagination = readBoolean(props, "pagination", pagination);
		scrolling_outside_boundary = readBoolean(props, "scrolling_outside_boundary", scrolling_outside_boundary);
		jei_slot_lookup = readBoolean(props, "jei_slot_lookup", jei_slot_lookup);
		max_height = clamp(readInt(props, "max_height", max_height), MAX_HEIGHT_MIN, MAX_HEIGHT_MAX);
		min_width = clamp(readInt(props, "min_width", min_width), MIN_WIDTH_MIN, MIN_WIDTH_MAX);
	}

	/** 把当前的值写回属性文件。 */
	public static void save() {
		Properties props = new Properties();
		props.setProperty("pagination", String.valueOf(pagination));
		props.setProperty("scrolling_outside_boundary", String.valueOf(scrolling_outside_boundary));
		props.setProperty("jei_slot_lookup", String.valueOf(jei_slot_lookup));
		props.setProperty("max_height", String.valueOf(max_height));
		props.setProperty("min_width", String.valueOf(min_width));

		Path path = getPath();

		try {
			Files.createDirectories(path.getParent());
			try (OutputStream out = Files.newOutputStream(path)) {
				props.store(out, "Trinkets Tooltip options (edited via Configured)");
			}
		} catch (IOException e) {
			LOGGER.error("Failed to save config to {}", path, e);
		}
	}

	/** 只收原始类型，这样这个类永远不需要解析 Configured 的类型。 */
	public static void refresh(boolean paginationValue, boolean scrollingOutsideBoundaryValue,
			int maxHeightValue, int minWidthValue, boolean jeiSlotLookupValue) {
		pagination = paginationValue;
		scrolling_outside_boundary = scrollingOutsideBoundaryValue;
		jei_slot_lookup = jeiSlotLookupValue;
		max_height = clamp(maxHeightValue, MAX_HEIGHT_MIN, MAX_HEIGHT_MAX);
		min_width = clamp(minWidthValue, MIN_WIDTH_MIN, MIN_WIDTH_MAX);
	}

	private static boolean readBoolean(Properties props, String key, boolean fallback) {
		String raw = props.getProperty(key);
		return raw == null ? fallback : Boolean.parseBoolean(raw);
	}

	private static int readInt(Properties props, String key, int fallback) {
		String raw = props.getProperty(key);

		if (raw == null) {
			return fallback;
		}

		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException e) {
			LOGGER.warn("Invalid integer config value for {}, using default {}", key, fallback);
			return fallback;
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
