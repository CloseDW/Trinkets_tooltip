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
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

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

	public static int max_height = 7;
	public static int min_width = 1;
	public static boolean jei_slot_lookup = true;

	/**
	 * 用面板左侧的滚动条代替翻页箭头，两者互斥。
	 * 关着（默认）时就是原来的分页：一页 {@code min_width} 列 × {@code max_height} 行，
	 * 靠箭头或滚轮整页翻。打开之后不再分页，面板高度固定为 {@code max_height} 行，
	 * 内容按行滚动，滚轮的每一格走一行。
	 */
	public static boolean scrollbar = false;

	/**
	 * Curios 风格「启用饰品渲染」开关里被关掉的槽位，键是
	 * {@code closedw.tt.client.theme.TrinketRenderToggle#key}，例如 {@code chest/necklace#0}。
	 * Curios 把渲染状态存在玩家身上并由服务端同步，本模组是纯客户端模组，
	 * 所以状态只能存在本机的属性文件里；这个选项刻意不出现在 Configured 界面上，
	 * 它由面板上的开关图标维护（逗号分隔的一串键）。
	 */
	public static final Set<String> render_disabled_slots = new LinkedHashSet<>();

	/** 属性文件里的键名。 */
	public static final String RENDER_DISABLED_SLOTS_KEY = "render_disabled_slots";

	public static Path getPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	/** 供 Configured 适配层使用的按键读取。 */
	public static boolean getBoolean(String key) {
		return switch (key) {
			case "jei_slot_lookup" -> jei_slot_lookup;
			case "scrollbar" -> scrollbar;
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

		// 分页不再是唯一的排版方式，而是由 scrollbar 在「翻页箭头」和「左侧滚动条」之间二选一；
		// 下一次 save() 会把它们自然清掉（旧文件里的值不再被读取）。
		jei_slot_lookup = readBoolean(props, "jei_slot_lookup", jei_slot_lookup);
		scrollbar = readBoolean(props, "scrollbar", scrollbar);
		max_height = clamp(readInt(props, "max_height", max_height), MAX_HEIGHT_MIN, MAX_HEIGHT_MAX);
		min_width = clamp(readInt(props, "min_width", min_width), MIN_WIDTH_MIN, MIN_WIDTH_MAX);

		render_disabled_slots.clear();

		for (String key : props.getProperty(RENDER_DISABLED_SLOTS_KEY, "").split(",")) {
			String trimmed = key.trim();

			if (!trimmed.isEmpty()) {
				render_disabled_slots.add(trimmed);
			}
		}
	}

	/**
	 * 记下或恢复某个饰品槽的渲染开关，并立刻落盘，这样重登之后仍然是玩家上次的选择。
	 */
	public static void setRenderDisabled(String slotKey, boolean disabled) {
		boolean changed = disabled ? render_disabled_slots.add(slotKey) : render_disabled_slots.remove(slotKey);

		if (changed) {
			save();
			LOGGER.info("Trinket rendering for slot {} turned {}", slotKey, disabled ? "off" : "on");
		}
	}

	/** 把当前的值写回属性文件。 */
	public static void save() {
		Properties props = new Properties();
		props.setProperty("jei_slot_lookup", String.valueOf(jei_slot_lookup));
		props.setProperty("scrollbar", String.valueOf(scrollbar));
		props.setProperty("max_height", String.valueOf(max_height));
		props.setProperty("min_width", String.valueOf(min_width));
		props.setProperty(RENDER_DISABLED_SLOTS_KEY, String.join(",", render_disabled_slots));

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
	public static void refresh(int maxHeightValue, int minWidthValue, boolean jeiSlotLookupValue,
			boolean scrollbarValue) {
		jei_slot_lookup = jeiSlotLookupValue;
		scrollbar = scrollbarValue;
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
