package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.KiTechnique;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AttackLoadoutScreen extends Screen {
    private final Screen returnScreen;
    private final Button[] slots = new Button[TechniqueHotbar.SLOT_COUNT];
    private int editingSlot = TechniqueHotbar.selectedSlot();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public AttackLoadoutScreen(Screen returnScreen) {
        super(Component.translatable("screen.kicraft.attacks.title"));
        this.returnScreen = returnScreen;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 24);
        panelHeight = Math.min(244, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int contentX = panelX + 16;
        int contentWidth = panelWidth - 32;
        int slotWidth = (contentWidth - 3 * (slots.length - 1)) / slots.length;
        int barX = panelX + (panelWidth - (slotWidth * slots.length + 3 * (slots.length - 1))) / 2;
        for (int slot = 0; slot < slots.length; slot++) {
            final int index = slot;
            slots[slot] = addRenderableWidget(new Button(barX + slot * (slotWidth + 3), panelY + 63,
                    slotWidth, 34, Component.empty(), button -> {
                        editingSlot = index;
                        refreshSlots();
                    }, narration -> narration.get()) {
                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    TechniqueHotbarRenderer.renderSlot(graphics, font, getX(), getY(), getWidth(), getHeight(),
                            index, ClientSettings.techniqueSlot(index), editingSlot == index, isHoveredOrFocused());
                }
            });
        }
        refreshSlots();

        int optionWidth = (contentWidth - 8) / 2;
        int index = 0;
        for (KiTechnique technique : KiTechnique.values()) {
            if (!ClientSettings.isHotbarTechnique(technique)) {
                continue;
            }
            Button option = addRenderableWidget(new KicraftMenuButton(contentX + (index % 2) * (optionWidth + 8),
                    panelY + 126 + (index / 2) * 26, optionWidth, 22,
                    Component.translatable(technique.translationKey()), button -> {
                        TechniqueHotbar.assign(editingSlot, technique);
                        refreshSlots();
                    }));
            option.active = ClientKiData.data().hasTechnique(technique);
            option.setTooltip(Tooltip.create(Component.translatable(option.active
                    ? "screen.kicraft.hotbar.assign" : "screen.kicraft.hotbar.locked")));
            index++;
        }

        addRenderableWidget(new KicraftMenuButton(contentX, panelY + panelHeight - 32, optionWidth, 22,
                Component.translatable("screen.kicraft.hotbar.clear"), button -> {
                    TechniqueHotbar.assign(editingSlot, null);
                    refreshSlots();
                }));
        addRenderableWidget(new KicraftMenuButton(contentX + optionWidth + 8, panelY + panelHeight - 32, optionWidth, 22,
                Component.translatable("gui.done"), button -> onClose()));
    }

    private void refreshSlots() {
        for (int slot = 0; slot < slots.length; slot++) {
            if (slots[slot] == null) {
                continue;
            }
            Component label = Component.translatable("screen.kicraft.hotbar.slot", slot + 1,
                    TechniqueHotbarRenderer.slotName(ClientSettings.techniqueSlot(slot)));
            slots[slot].setMessage(label);
            slots[slot].setTooltip(Tooltip.create(label));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xA0000000);
        graphics.fill(panelX + 3, panelY + 4, panelX + panelWidth + 3, panelY + panelHeight + 4, 0x88000000);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF75401A);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, 0xFF140A05);
        int centerX = panelX + panelWidth / 2;
        TechniqueHotbarRenderer.drawFitted(graphics, font, title, centerX, panelY + 12, panelWidth - 32, 0xFFFFE3A1);
        TechniqueHotbarRenderer.drawFitted(graphics, font, Component.translatable("screen.kicraft.hotbar.switch_hint"),
                centerX, panelY + 31, panelWidth - 32, 0xFFCFB89A);
        TechniqueHotbarRenderer.drawFitted(graphics, font, Component.translatable("screen.kicraft.hotbar.use_hint",
                        minecraft.options.keyUse.getTranslatedKeyMessage()),
                centerX, panelY + 44, panelWidth - 32, 0xFFCFB89A);
        TechniqueHotbarRenderer.drawFitted(graphics, font, Component.translatable("screen.kicraft.hotbar.editing", editingSlot + 1),
                centerX, panelY + 107, panelWidth - 32, 0xFFFFD25A);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (panelHeight >= 224) {
            TechniqueHotbarRenderer.drawFitted(graphics, font, Component.translatable("screen.kicraft.hotbar.melee_hint",
                            minecraft.options.keyAttack.getTranslatedKeyMessage(), KicraftKeyMappings.BLOCK.getTranslatedKeyMessage()),
                    centerX, panelY + 183, panelWidth - 32, 0xFFCFB89A);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return returnScreen instanceof KicraftPauseScreen;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(returnScreen);
    }
}
