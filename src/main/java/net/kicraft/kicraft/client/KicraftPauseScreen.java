package net.kicraft.kicraft.client;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.ModListScreen;

/** Replaces the vanilla pause screen while retaining each of its gameplay actions. */
public final class KicraftPauseScreen extends Screen {
    private static final String DISCORD_INVITE_URL = "https://discord.gg/GggQ9QvU89";
    private static final int BACKDROP = 0xB8000000;
    private static final int SHADOW = 0x90000000;
    private static final int FRAME_DARK = 0xFF130803;
    private static final int FRAME = 0xFF25130A;
    private static final int FRAME_EDGE = 0xFF75401A;
    private static final int PANEL = 0xFF140A05;
    private static final int ACCENT = 0xFFFFB13B;
    private static final int ACCENT_BRIGHT = 0xFFFFE3A1;
    private static final int TEXT = 0xFFFFF0D3;
    private static final int TEXT_DIM = 0xFFCFB89A;
    private static final int PANEL_HEIGHT = 338;
    private static final int HEADER_HEIGHT = 72;
    private static final int ICON_SIZE = 22;
    private static final int ICON_GAP = 5;

    public KicraftPauseScreen() {
        super(Component.translatable("screen.kicraft.pause.title"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(560, width - 32);
        int panelHeight = PANEL_HEIGHT;
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int contentX = panelX + 20;
        int contentWidth = panelWidth - 40;
        int cardHeight = 58;
        int firstRowWidth = (contentWidth - 16) / 3;
        int secondRowWidth = (contentWidth - 8) / 2;
        int firstRowY = panelY + 108;
        int secondRowY = firstRowY + cardHeight + 8;

        addRenderableWidget(category(contentX, firstRowY, firstRowWidth, cardHeight, KicraftCategoryButton.Category.STATS,
                "screen.kicraft.pause.stats", button -> minecraft.setScreen(new StatsScreen(this))));
        addRenderableWidget(category(contentX + firstRowWidth + 8, firstRowY, firstRowWidth, cardHeight, KicraftCategoryButton.Category.FORMS,
                "screen.kicraft.pause.forms", button -> minecraft.setScreen(new TransformationScreen(this))));
        addRenderableWidget(category(contentX + (firstRowWidth + 8) * 2, firstRowY, firstRowWidth, cardHeight, KicraftCategoryButton.Category.TECHNIQUES,
                "screen.kicraft.pause.techniques", button -> minecraft.setScreen(new TechniqueLibraryScreen(this))));
        addRenderableWidget(category(contentX, secondRowY, secondRowWidth, cardHeight, KicraftCategoryButton.Category.KI_ATTACKS,
                "screen.kicraft.pause.attacks", button -> minecraft.setScreen(new AttackLoadoutScreen(this))));
        addRenderableWidget(category(contentX + secondRowWidth + 8, secondRowY, secondRowWidth, cardHeight, KicraftCategoryButton.Category.STORY,
                "screen.kicraft.pause.story", button -> minecraft.setScreen(new StoryScreen(this))));

        int iconX = iconDockX(panelX, panelWidth, 4);
        int iconY = panelY + 8;
        addIcon(iconX, iconY, ICON_SIZE, KicraftIconButton.Icon.ADVANCEMENTS, Component.translatable("gui.advancements"), button -> openAdvancements());
        addIcon(iconX += ICON_SIZE + ICON_GAP, iconY, ICON_SIZE, KicraftIconButton.Icon.STATISTICS, Component.translatable("gui.stats"), button -> openVanillaStatistics());
        addIcon(iconX += ICON_SIZE + ICON_GAP, iconY, ICON_SIZE, KicraftIconButton.Icon.OPTIONS, Component.translatable("menu.options"), button -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
        addIcon(iconX += ICON_SIZE + ICON_GAP, iconY, ICON_SIZE, KicraftIconButton.Icon.MODS, Component.translatable("fml.menu.mods"), button -> minecraft.setScreen(new ModListScreen(this)));
        iconX = iconDockX(panelX, panelWidth, 3);
        iconY += ICON_SIZE + 4;
        addIcon(iconX, iconY, ICON_SIZE, KicraftIconButton.Icon.FEEDBACK, Component.translatable("menu.sendFeedback"), button -> openFeedback());
        addIcon(iconX += ICON_SIZE + ICON_GAP, iconY, ICON_SIZE, KicraftIconButton.Icon.BUG_REPORT,
                Component.translatable("menu.reportBugs"), button -> openLink(DISCORD_INVITE_URL));

        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished()) {
            addIcon(iconX += ICON_SIZE + ICON_GAP, iconY, ICON_SIZE, KicraftIconButton.Icon.SHARE_LAN,
                    Component.translatable("menu.shareToLan"), button -> minecraft.setScreen(new ShareToLanScreen(this)));
        } else {
            addIcon(iconX += ICON_SIZE + ICON_GAP, iconY, ICON_SIZE, KicraftIconButton.Icon.PLAYER_REPORTING,
                    Component.translatable("menu.playerReporting"), button -> minecraft.setScreen(new SocialInteractionsScreen()));
        }

        int footerWidth = (panelWidth - 48) / 2;
        int footerY = panelY + panelHeight - 56;
        addRenderableWidget(new KicraftMenuButton(contentX, footerY, footerWidth, 22,
                Component.translatable("menu.returnToGame"), button -> resumeGame()));
        Component exitLabel = Component.translatable(minecraft.isLocalServer() ? "menu.returnToMenu" : "menu.disconnect");
        addRenderableWidget(new KicraftMenuButton(contentX + footerWidth + 8, footerY, footerWidth, 22,
                exitLabel, button -> disconnect()));
        addRenderableWidget(new KicraftMenuButton(contentX, panelY + panelHeight - 29, contentWidth, 20,
                Component.translatable("screen.kicraft.pause.exit_game"), button -> exitGame()));
    }

    private static int iconDockX(int panelX, int panelWidth, int iconCount) {
        return panelX + panelWidth - 20 - ICON_SIZE * iconCount - ICON_GAP * (iconCount - 1);
    }

    private Button button(int x, int y, int width, int height, String key, Button.OnPress onPress) {
        return new KicraftMenuButton(x, y, width, height, Component.translatable(key), onPress);
    }

    private Button category(int x, int y, int width, int height, KicraftCategoryButton.Category category,
                            String key, Button.OnPress onPress) {
        return new KicraftCategoryButton(x, y, width, height, category, Component.translatable(key), onPress);
    }

    private void addIcon(int x, int y, int size, KicraftIconButton.Icon icon, Component label, Button.OnPress onPress) {
        KicraftIconButton button = new KicraftIconButton(x, y, size, icon, label, onPress);
        button.setTooltip(Tooltip.create(label));
        addRenderableWidget(button);
    }

    private void resumeGame() {
        minecraft.setScreen(null);
        minecraft.mouseHandler.grabMouse();
    }

    private void openAdvancements() {
        if (minecraft.player != null) {
            minecraft.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements()));
        }
    }

    private void openVanillaStatistics() {
        if (minecraft.player != null) {
            minecraft.setScreen(new net.minecraft.client.gui.screens.achievement.StatsScreen(this, minecraft.player.getStats()));
        }
    }

    private void openFeedback() {
        String url = SharedConstants.getCurrentVersion().isStable()
                ? "https://aka.ms/javafeedback?ref=game"
                : "https://aka.ms/snapshotfeedback?ref=game";
        openLink(url);
    }

    private void openLink(String url) {
        minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(url);
            }
            minecraft.setScreen(this);
        }, url, true));
    }

    private void disconnect() {
        boolean localServer = minecraft.isLocalServer();
        boolean realms = minecraft.isConnectedToRealms();
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }

        if (localServer) {
            minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            minecraft.clearLevel();
        }

        TitleScreen titleScreen = new TitleScreen();
        if (localServer) {
            minecraft.setScreen(titleScreen);
        } else if (realms) {
            minecraft.setScreen(new RealmsMainScreen(titleScreen));
        } else {
            minecraft.setScreen(new JoinMultiplayerScreen(titleScreen));
        }
    }

    private void exitGame() {
        minecraft.stop();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, BACKDROP);

        int panelWidth = Math.min(560, width - 32);
        int panelHeight = PANEL_HEIGHT;
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        graphics.fill(panelX + 4, panelY + 5, panelX + panelWidth + 5, panelY + panelHeight + 5, SHADOW);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, FRAME_DARK);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, FRAME_EDGE);
        graphics.fill(panelX + 5, panelY + 5, panelX + panelWidth - 5, panelY + panelHeight - 5, FRAME);

        graphics.fill(panelX + 13, panelY + HEADER_HEIGHT + 26, panelX + panelWidth - 13, panelY + panelHeight - 65, PANEL);

        graphics.drawCenteredString(font, title, width / 2, panelY + 17, ACCENT_BRIGHT);
        graphics.drawString(font, Component.translatable("screen.kicraft.pause.character"), panelX + 20, panelY + HEADER_HEIGHT + 10, TEXT);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        resumeGame();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
