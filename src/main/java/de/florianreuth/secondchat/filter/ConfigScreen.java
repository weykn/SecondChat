/*
 * This file is part of SecondChat - https://github.com/florianreuth/SecondChat
 * Copyright (C) 2025-2026 Florian Reuth <git@florianreuth.de> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianreuth.secondchat.filter;

import de.florianreuth.secondchat.SecondChat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

public final class ConfigScreen extends Screen {
    private static final int RED_TRANSPARENT = 0x80FF0000;
    private static final int PADDING = 3;

    private static final int TEXT_FIELD_WIDTH = 120;
    private static final int SERVER_FIELD_WIDTH = 100;
    private static final int FILTER_BUTTON_WIDTH = 60;
    private static final int ADD_BUTTON_WIDTH = 20;

    private static final float COL_FILTER_PCT = 0.55f;
    private static final float COL_SERVER_PCT = 0.30f;

    private final Screen parent;

    private EditBox editBox;
    private EditBox serverBox;
    private Button addButton;
    private FilterType filterType = FilterType.CONTAINS;

    private FilterRule alreadyAdded;

    public ConfigScreen(final Screen parent) {
        super(Component.translatable("secondchat.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new SlotList(
            this.minecraft,
            width,
            height,
            PADDING + PADDING + (font.lineHeight + 2) * PADDING,
            30,
            font.lineHeight + ListEntry.INNER_PADDING * 4
        ));

        final int y = height - Button.DEFAULT_HEIGHT - PADDING - 1;
        int x = width / 2 - (TEXT_FIELD_WIDTH + PADDING + SERVER_FIELD_WIDTH + PADDING + FILTER_BUTTON_WIDTH + PADDING + ADD_BUTTON_WIDTH) / 2;

        editBox = addRenderableWidget(new EditBox(this.font, x, y, TEXT_FIELD_WIDTH, Button.DEFAULT_HEIGHT, Component.empty()));
        editBox.setMaxLength(Integer.MAX_VALUE);

        x += TEXT_FIELD_WIDTH + PADDING;
        serverBox = addRenderableWidget(new EditBox(this.font, x, y, SERVER_FIELD_WIDTH, Button.DEFAULT_HEIGHT, Component.empty()));
        serverBox.setMaxLength(253);
        serverBox.setHint(Component.literal("* (all servers)").withStyle(ChatFormatting.DARK_GRAY));

        x += SERVER_FIELD_WIDTH + PADDING;
        addRenderableWidget(Button
            .builder(getFilterTypeText(filterType), button -> {
                filterType = FilterType.values()[(filterType.ordinal() + 1) % FilterType.values().length];
                button.setMessage(getFilterTypeText(filterType));
            })
            .pos(x, y)
            .size(FILTER_BUTTON_WIDTH, Button.DEFAULT_HEIGHT)
            .build());

        x += FILTER_BUTTON_WIDTH + PADDING;
        addButton = addRenderableWidget(Button
            .builder(Component.literal("+"), button -> {
                final String server = serverBox.getValue().isBlank() ? "*" : serverBox.getValue();
                SecondChat.instance().add(new FilterRule(editBox.getValue(), server, filterType));
                minecraft.setScreen(new ConfigScreen(parent));
            })
            .pos(x, y)
            .size(ADD_BUTTON_WIDTH, Button.DEFAULT_HEIGHT)
            .build());
        addButton.active = false;

        addRenderableWidget(Button
            .builder(Component.literal("<-"), button -> minecraft.setScreen(parent))
            .pos(PADDING, y)
            .size(Button.DEFAULT_HEIGHT, Button.DEFAULT_HEIGHT)
            .build());
    }

    private Component getFilterTypeText(final FilterType filterType) {
        return Component.translatable("secondchat.config.filter." + filterType.name().toLowerCase()).withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void tick() {
        super.tick();
        if (addButton == null) {
            return;
        }

        addButton.active = !editBox.getValue().isEmpty();
        if (!addButton.active) {
            return;
        }

        final String server = serverBox.getValue().isBlank() ? "*" : serverBox.getValue();
        SecondChat.instance().rules().stream()
            .filter(rule -> rule.value().equals(editBox.getValue()) && rule.type() == filterType && rule.server().equals(server))
            .findAny()
            .ifPresentOrElse(filterRule -> this.alreadyAdded = filterRule, () -> this.alreadyAdded = null);
        addButton.active = alreadyAdded == null;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        final Matrix3x2fStack pose = graphics.pose();

        pose.pushMatrix();
        pose.scale(2.0F, 2.0F);
        graphics.text(font, title, this.width / 4 - font.width(title) / 2, 5, -1, true);
        pose.popMatrix();

        final int headerY = PADDING + PADDING + (font.lineHeight + 2) * PADDING - font.lineHeight - 2;
        final int contentWidth = new SlotList(minecraft, width, height,
            PADDING + PADDING + (font.lineHeight + 2) * PADDING, 30,
            font.lineHeight + ListEntry.INNER_PADDING * 4).getRowWidth();
        final int listX = width / 2 - contentWidth / 2;

        final int filterColWidth = (int) (contentWidth * COL_FILTER_PCT);
        final int serverColWidth = (int) (contentWidth * COL_SERVER_PCT);

        final Component filterHeader = Component.literal("FILTER").withStyle(ChatFormatting.GOLD);
        final Component serverHeader = Component.literal("SERVER").withStyle(ChatFormatting.GOLD);
        final Component typeHeader = Component.literal("TYPE").withStyle(ChatFormatting.GOLD);

        graphics.text(font, filterHeader, listX + ListEntry.INNER_PADDING, headerY, -1, false);
        graphics.text(font, serverHeader, listX + filterColWidth + serverColWidth / 2 - font.width(serverHeader) / 2, headerY, -1, false);
        graphics.text(font, typeHeader, listX + contentWidth - font.width(typeHeader) - ListEntry.INNER_PADDING * 2, headerY, -1, false);
    }

    public class SlotList extends ObjectSelectionList<ListEntry> {

        public SlotList(Minecraft minecraft, int width, int height, int top, int bottom, int entryHeight) {
            super(minecraft, width, height - top - bottom, top, entryHeight);

            SecondChat.instance().rules().forEach(rule -> addEntry(new ListEntry(rule)));
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 150;
        }

        @Override
        protected void extractSelection(final GuiGraphicsExtractor graphics, final ListEntry entry, final int outlineColor) {
        }
    }

    public class ListEntry extends ObjectSelectionList.Entry<ListEntry> {
        public static final int INNER_PADDING = 2;

        private final FilterRule rule;

        public ListEntry(FilterRule rule) {
            this.rule = rule;
        }

        @Override
        public @NotNull Component getNarration() {
            return getFilterTypeText(rule.type());
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent mouseButtonEvent, final boolean bl) {
            SecondChat.instance().remove(rule);
            minecraft.setScreen(new ConfigScreen(parent));
            return super.mouseClicked(mouseButtonEvent, bl);
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            final Matrix3x2fStack pose = graphics.pose();

            final int width = getContentWidth();
            final int height = getContentHeight();

            final int filterColWidth = (int) (width * COL_FILTER_PCT);
            final int serverColWidth = (int) (width * COL_SERVER_PCT);
            final int serverColX = filterColWidth;
            final int typeColX = filterColWidth + serverColWidth;

            final int color = ConfigScreen.this.alreadyAdded == rule ? RED_TRANSPARENT : Integer.MIN_VALUE;
            pose.pushMatrix();
            pose.translate(getContentX(), getContentY());
            graphics.fill(0, 0, width - INNER_PADDING * 2, height, color);

            String filterValue = rule.value();
            if (font.width(filterValue) > filterColWidth - INNER_PADDING * 2) {
                while (font.width(filterValue + "...") > filterColWidth - INNER_PADDING * 2 && !filterValue.isEmpty()) {
                    filterValue = filterValue.substring(0, filterValue.length() - 1);
                }
                filterValue += "...";
            }
            final MutableComponent base = Component.literal(filterValue);
            final Component filterText = hovered ? base.withStyle(ChatFormatting.ITALIC, ChatFormatting.RED) : base;
            graphics.text(font, filterText, INNER_PADDING, INNER_PADDING, -1);

            final String serverDisplay = rule.server() == null || rule.server().isBlank() ? "*" : rule.server();
            final Component serverText = Component.literal(serverDisplay)
                .withStyle(serverDisplay.equals("*") ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE);
            graphics.text(font, serverText, serverColX + serverColWidth / 2 - font.width(serverText) / 2, INNER_PADDING, -1);

            final Component narration = Component.literal("").append(getNarration()).withStyle(ChatFormatting.GOLD);
            graphics.text(font, narration, width - font.width(narration) - INNER_PADDING * 2, INNER_PADDING, -1);

            pose.popMatrix();
        }
    }
}
