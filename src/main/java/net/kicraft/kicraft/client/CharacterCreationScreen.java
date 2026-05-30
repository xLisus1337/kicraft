package net.kicraft.kicraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.kicraft.kicraft.network.PacketHandler;
import net.kicraft.kicraft.network.RaceSyncC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CharacterCreationScreen extends Screen {
    // Fazy kreatora: 0 = Rasy, 1 = Style, 2 = Kolory RGB
    private int currentPage = 0;

    // --- ZMIENNE DO ZAPISU WYGLĄDU ---
    private String selectedRace = "Saiyan";
    private int currentForm = 0; // 0 = Baza, 1-7 = Transformacje

    // Tablice dla stylów (żeby widzieć zmiany na żywo w menu)
    private int[] hairStyles = new int[8];
    private int[] eyeTypes = new int[8];
    private int colorCategory = 0; // 0=Skin, 1=Hair, 2=Eye, 3=Ki

    // --- DOPASOWANIE DO MONITORA Z TŁA ---
    // Zwiększyliśmy szerokość i wysokość
    private final int menuWidth = 500;
    private final int menuHeight = 260;

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation("kicraft", "textures/gui/lab_bg.png");

    public CharacterCreationScreen() {
        super(Component.literal("Incubator - Character Creation"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int x = (this.width - menuWidth) / 2;
        // Przesuwamy o 20 pikseli w górę, żeby wstrzelić się w monitor!
        int y = (this.height - menuHeight) / 2 - 20;

        if (currentPage == 0) {
            // ==========================================
            // STRONA 1: WYBÓR RASY
            // ==========================================
            String[] races = {"Saiyan", "Human", "Namekian", "Frost Demon", "Half-Saiyan", "Android", "Bio-Android", "Majin"};

            for (int i = 0; i < races.length; i++) {
                String r = races[i];
                int col = i % 2;
                int row = i / 2;

                this.addRenderableWidget(Button.builder(Component.literal(r), (b) -> {
                    selectedRace = r;
                    // Aktualizacja rasy w capability po stronie klienta (tylko dla podglądu!)
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.getCapability(net.kicraft.kicraft.capability.PlayerDataProvider.PLAYER_DATA).ifPresent(d -> {
                            d.setRace(r);
                        });
                    }
                }).bounds(x + 40 + (col * 140), y + 60 + (row * 30), 120, 20).build());
            }
        }
        else if (currentPage == 1) {
            // ==========================================
            // STRONA 2: STYLE (Włosy, Oczy, Formy)
            // ==========================================
            this.addRenderableWidget(Button.builder(Component.literal("< BACK"), (b) -> {
                currentPage--; this.init();
            }).bounds(x + 20, y + menuHeight - 35, 60, 20).build());

            // Zakładki Form (Base, Form 1 - 7)
            for (int i = 0; i < 8; i++) {
                final int formIndex = i;
                String formName = (i == 0) ? "Base" : "F" + i;
                this.addRenderableWidget(Button.builder(Component.literal(formName), (b) -> {
                    currentForm = formIndex;
                }).bounds(x + 20 + (i * 40), y + 60, 35, 20).build());
            }

            // Styl Włosów < >
            this.addRenderableWidget(Button.builder(Component.literal("<"), (b) -> hairStyles[currentForm]--).bounds(x + 100, y + 110, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), (b) -> hairStyles[currentForm]++).bounds(x + 160, y + 110, 20, 20).build());

            // Styl Oczu < >
            this.addRenderableWidget(Button.builder(Component.literal("<"), (b) -> eyeTypes[currentForm]--).bounds(x + 100, y + 150, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), (b) -> eyeTypes[currentForm]++).bounds(x + 160, y + 150, 20, 20).build());
        }
        else if (currentPage == 2) {
            // ==========================================
            // STRONA 3: KOLORY RGB
            // ==========================================
            this.addRenderableWidget(Button.builder(Component.literal("< BACK"), (b) -> {
                currentPage--; this.init();
            }).bounds(x + 20, y + menuHeight - 35, 60, 20).build());

            // Zakładki Kategorii Kolorów
            String[] colors = {"Skin", "Hair", "Eyes", "Ki"};
            for (int i = 0; i < colors.length; i++) {
                final int catIndex = i;
                this.addRenderableWidget(Button.builder(Component.literal(colors[i]), (b) -> {
                    colorCategory = catIndex;
                }).bounds(x + 20 + (i * 75), y + 60, 70, 20).build());
            }

            // Prowizoryczne przyciski RGB (Później zrobimy z tego ładne suwaki)
            this.addRenderableWidget(Button.builder(Component.literal("R -"), (b) -> {}).bounds(x + 50, y + 100, 30, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("R +"), (b) -> {}).bounds(x + 90, y + 100, 30, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("G -"), (b) -> {}).bounds(x + 50, y + 130, 30, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("G +"), (b) -> {}).bounds(x + 90, y + 130, 30, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("B -"), (b) -> {}).bounds(x + 50, y + 160, 30, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("B +"), (b) -> {}).bounds(x + 90, y + 160, 30, 20).build());
        }

        // --- PRZYCISK NEXT / FINISH ---
        if (currentPage < 2) {
            this.addRenderableWidget(Button.builder(Component.literal("NEXT >"), (b) -> {
                currentPage++; this.init();
            }).bounds(x + menuWidth - 230, y + menuHeight - 35, 60, 20).build());
        } else {
            this.addRenderableWidget(Button.builder(Component.literal("FINISH"), (b) -> {
                // Na razie wysyła samą rasę. Potem dodamy wysyłanie kolorów!
                PacketHandler.sendToServer(new RaceSyncC2SPacket(selectedRace));
                this.onClose();
            }).bounds(x + menuWidth - 230, y + menuHeight - 35, 60, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(BG_TEXTURE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        int x = (this.width - menuWidth) / 2;
        int y = (this.height - menuHeight) / 2 - 20; // Pamiętamy o przesunięciu w dół!

        RenderSystem.enableBlend();

        // Rysujemy wtopiony w tło Tech Box
        drawTechBox(guiGraphics, x, y, menuWidth, menuHeight);

        guiGraphics.drawCenteredString(this.font, "§b§lPROJECT: NEW WARRIOR", x + (menuWidth / 2) - 75, y + 12, 0xFFFFFF);

        if (currentPage == 0) {
            guiGraphics.drawString(this.font, "§7Selected Origin: §f" + selectedRace, x + 20, y + 40, 0xFFFFFF);
        } else if (currentPage == 1) {
            guiGraphics.drawString(this.font, "§7Edit Styles for: §f" + selectedRace + " §8| Form: §e" + (currentForm == 0 ? "Base" : "F" + currentForm), x + 20, y + 40, 0xFFFFFF);

            guiGraphics.drawString(this.font, "Hair Type: " + hairStyles[currentForm], x + 20, y + 115, 0xFFFFFF);
            guiGraphics.drawString(this.font, "Eye Type: " + eyeTypes[currentForm], x + 20, y + 155, 0xFFFFFF);
        } else if (currentPage == 2) {
            String[] cats = {"Skin", "Hair", "Eyes", "Ki"};
            guiGraphics.drawString(this.font, "§7Edit Colors for: §f" + cats[colorCategory] + " §8| Form: §e" + (currentForm == 0 ? "Base" : "F" + currentForm), x + 20, y + 40, 0xFFFFFF);
        }

        // ==================================================
        // PODGLĄD MODELU POSTACI
        // ==================================================
        int modelX = x + menuWidth - 75;
        int modelY = y + menuHeight - 40;

        guiGraphics.fill(modelX - 45, modelY - 5, modelX + 45, modelY + 5, 0x4400FFFF);

        if (this.minecraft != null && this.minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, modelX, modelY, 65, (float)modelX - mouseX, (float)modelY - 70 - mouseY, this.minecraft.player);
        }

        RenderSystem.disableBlend();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // --- ZMIENIONA METODA: Wtapiamy w monitor ---
    private void drawTechBox(GuiGraphics g, int x, int y, int width, int height) {
        // Zamiast czarnej smoły dajemy tylko LEKKO przyciemnioną szybę (0x88000000)
        g.fill(x, y, x + width, y + height, 0x88000000);

        // Zostawiamy tylko delikatną linię oddzielającą model od przycisków
        g.fill(x + width - 150, y + 31, x + width - 149, y + height, 0x5500FFFF);
    }

    @Override
    public boolean isPauseScreen() { return true; }
}