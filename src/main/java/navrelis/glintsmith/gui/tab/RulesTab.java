package navrelis.glintsmith.gui.tab;

import java.util.ArrayList;
import java.util.List;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.RuleEditScreen;
import navrelis.glintsmith.gui.Ui;
import navrelis.glintsmith.gui.widget.OptionList;
import navrelis.glintsmith.style.OutlineSettings;
import navrelis.glintsmith.style.Rule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The ordered list of rules with add / edit / move / delete.
 */
public final class RulesTab extends ConfigTab {
	public RulesTab(GlintsmithConfig config) {
		super(config, Ui.t("glintsmith.tab.rules"));
	}

	@Override
	protected void fill() {
		list.addHeading(Ui.t("glintsmith.rules.heading"));

		Button add = Ui.button(Ui.HALF, Ui.t("glintsmith.rules.add"), Ui.tip("glintsmith.rules.add"), button -> {
			Rule rule = newRule();
			rule.name = Ui.t("glintsmith.rules.new_name").getString();
			addAndEdit(rule);
		});
		Button addHeld = Ui.button(Ui.HALF, Ui.t("glintsmith.rules.add_held"), Ui.tip("glintsmith.rules.add_held"), button -> {
			ItemStack held = heldItem();
			if (held.isEmpty()) {
				return;
			}
			Rule rule = newRule();
			rule.name = held.getHoverName().getString();
			rule.items.add(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
			addAndEdit(rule);
		});
		addHeld.active = !heldItem().isEmpty();
		list.addPair(add, addHeld);
		list.addHelp(Ui.t("glintsmith.rules.help"));

		if (config.rules.isEmpty()) {
			list.addHelp(Ui.t("glintsmith.rules.empty"));
		}
		for (int i = 0; i < config.rules.size(); i++) {
			list.add(new RuleRow(config.rules.get(i), i == 0, i == config.rules.size() - 1));
		}
	}

	private static Rule newRule() {
		Rule rule = new Rule();
		rule.outline = OutlineSettings.enabled(true);
		return rule;
	}

	private static ItemStack heldItem() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
	}

	private void addAndEdit(Rule rule) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.setScreen(new RuleEditScreen(minecraft.screen, rule, edited -> {
			config.rules.add(edited);
			rebuild();
			list.setScrollAmount(list.getMaxScroll());
		}));
	}

	private void move(Rule rule, int direction) {
		int index = config.rules.indexOf(rule);
		int target = index + direction;
		if (index < 0 || target < 0 || target >= config.rules.size()) {
			return;
		}
		config.rules.remove(index);
		config.rules.add(target, rule);
		rebuild();
	}

	private void edit(Rule rule) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.setScreen(new RuleEditScreen(minecraft.screen, rule, edited -> {
			int index = config.rules.indexOf(rule);
			if (index >= 0) {
				config.rules.set(index, edited);
			}
			rebuild();
		}));
	}

	private void delete(Rule rule) {
		config.rules.remove(rule);
		rebuild();
	}

	@Override
	public void reset(GlintsmithConfig defaults) {
		config.rules = defaults.rules;
	}

	/** "3 items - outline - glint" */
	private static String summary(Rule rule) {
		List<String> parts = new ArrayList<>();
		int count = rule.items == null ? 0 : rule.items.size();
		if (count == 0) {
			parts.add(Ui.t("glintsmith.rules.summary.any_item").getString());
		} else if (count == 1) {
			parts.add(Ui.t("glintsmith.rules.summary.one_item").getString());
		} else {
			parts.add(Ui.t("glintsmith.rules.summary.items", count).getString());
		}
		if (rule.outline != null) {
			parts.add(Ui.t("glintsmith.rules.summary.outline").getString());
		}
		if (rule.glint != null) {
			parts.add(Ui.t("glintsmith.rules.summary.glint").getString());
		}
		return String.join(" · ", parts);
	}

	/** One rule: checkbox, name and summary, error marker, move / edit / delete buttons. */
	private final class RuleRow extends OptionList.Row {
		private static final int CHECKBOX_X = 0;
		private static final int TEXT_X = 26;
		private static final int MARKER_WIDTH = 10;
		private static final int BUTTON = Ui.HEIGHT;
		private static final int EDIT_WIDTH = 36;

		private final Rule rule;
		private final Checkbox enabled;
		private final Button up;
		private final Button down;
		private final Button edit;
		private final Button delete;

		RuleRow(Rule rule, boolean first, boolean last) {
			super(Ui.GAP, List.<AbstractWidget>of());
			this.rule = rule;
			Font font = Minecraft.getInstance().font;
			this.enabled = Checkbox.builder(Component.empty(), font)
				.selected(rule.enabled)
				.tooltip(Ui.tip("glintsmith.rules.enabled"))
				.onValueChange((box, value) -> rule.enabled = value)
				.build();
			this.up = Ui.button(BUTTON, Component.literal("▲"), Ui.tip("glintsmith.rules.up"), b -> move(rule, -1));
			this.down = Ui.button(BUTTON, Component.literal("▼"), Ui.tip("glintsmith.rules.down"), b -> move(rule, 1));
			this.edit = Ui.button(EDIT_WIDTH, Ui.t("glintsmith.rules.edit"), Ui.tip("glintsmith.rules.edit"), b -> edit(rule));
			this.delete = Ui.button(BUTTON, Component.literal("X").withStyle(ChatFormatting.RED),
				Ui.tip("glintsmith.rules.delete"), b -> delete(rule));
			up.active = !first;
			down.active = !last;
			this.widgets = List.of(enabled, up, down, edit, delete);
		}

		@Override
		public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
			int mouseY, boolean hovered, float partialTick) {
			int x = left - 2;
			Font font = Minecraft.getInstance().font;
			delete.setPosition(x + Ui.WIDTH - BUTTON, top);
			edit.setPosition(delete.getX() - Ui.GAP - EDIT_WIDTH, top);
			down.setPosition(edit.getX() - Ui.GAP - BUTTON, top);
			up.setPosition(down.getX() - Ui.GAP - BUTTON, top);
			enabled.setPosition(x + CHECKBOX_X, top + (Ui.HEIGHT - enabled.getHeight()) / 2);

			int markerX = up.getX() - Ui.GAP - MARKER_WIDTH;
			int textX = x + TEXT_X;
			int textWidth = markerX - Ui.GAP - textX;
			String name = rule.name == null || rule.name.isBlank() ? Ui.t("glintsmith.rules.unnamed").getString() : rule.name;
			graphics.drawString(font, fit(font, name, textWidth), textX, top + 1, rule.enabled ? 0xFFFFFFFF : 0xFF808080);
			graphics.drawString(font, fit(font, summary(rule), textWidth), textX, top + 11, 0xFF808080);

			if (rule.error != null) {
				graphics.drawString(font, Component.literal("!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
					markerX + 3, top + 6, 0xFFFF5555);
				boolean over = mouseX >= markerX && mouseX < markerX + MARKER_WIDTH && mouseY >= top && mouseY < top + Ui.HEIGHT
					&& graphics.containsPointInScissor(mouseX, mouseY);
				if (over && Minecraft.getInstance().screen != null) {
					Minecraft.getInstance().screen.setTooltipForNextRenderPass(
						Ui.t("glintsmith.rules.invalid", rule.error).withStyle(ChatFormatting.RED));
				}
			}

			for (AbstractWidget widget : widgets) {
				widget.render(graphics, mouseX, mouseY, partialTick);
			}
		}

		private static String fit(Font font, String text, int maxWidth) {
			if (font.width(text) <= maxWidth) {
				return text;
			}
			return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("…"))) + "…";
		}
	}
}
