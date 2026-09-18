package net.kicraft.kicraft.client;

import net.minecraftforge.common.ForgeConfigSpec;
import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiTechnique;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

/** Client-only accessibility and presentation settings. */
@Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSettings {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.DoubleValue HUD_SCALE;
    private static final ForgeConfigSpec.IntValue HUD_OFFSET_X;
    private static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;
    private static final ForgeConfigSpec.BooleanValue SHOW_DRAGON_RADAR;
    private static final ForgeConfigSpec.BooleanValue SHOW_RESOURCE_NUMBERS;
    private static final ForgeConfigSpec.BooleanValue LOCK_ON_CAMERA_TRACKING;
    private static final ForgeConfigSpec.BooleanValue SMOOTH_SHOULDER_CAMERA;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TECHNIQUE_HOTBAR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("hud");
        HUD_SCALE = builder.comment("Scale applied to the Ki Craft HUD and transformation charge bar.")
                .defineInRange("scale", 1.0D, 0.60D, 2.00D);
        HUD_OFFSET_X = builder.comment("Horizontal HUD offset in pixels.")
                .defineInRange("offsetX", 0, -500, 500);
        HUD_OFFSET_Y = builder.comment("Vertical HUD offset in pixels.")
                .defineInRange("offsetY", 0, -500, 500);
        SHOW_DRAGON_RADAR = builder.comment("Shows the decorative Dragon Radar after it is unlocked.")
                .define("showDragonRadar", true);
        SHOW_RESOURCE_NUMBERS = builder.comment("Shows current and maximum resource values beside percentages.")
                .define("showResourceNumbers", false);
        builder.pop();

        builder.push("camera");
        LOCK_ON_CAMERA_TRACKING = builder.comment("Lets lock-on guide the regular first-person camera toward its target.")
                .define("lockOnCameraTracking", true);
        SMOOTH_SHOULDER_CAMERA = builder.comment("Smooths the transition between shoulder-camera positions.")
                .define("smoothShoulderCamera", true);
        builder.pop();

        builder.push("techniqueHotbar");
        TECHNIQUE_HOTBAR = builder.comment("Four Ki-attack slots, in hotbar order. An empty string leaves a slot empty.")
                .defineListAllowEmpty(List.of("slots"),
                        () -> List.of("ki_blast", "", "", ""),
                        value -> value instanceof String);
        builder.pop();

        SPEC = builder.build();
    }

    private ClientSettings() {
    }

    public static float hudScale() {
        return HUD_SCALE.get().floatValue();
    }

    public static int hudOffsetX() {
        return HUD_OFFSET_X.get();
    }

    public static int hudOffsetY() {
        return HUD_OFFSET_Y.get();
    }

    public static boolean showDragonRadar() {
        return SHOW_DRAGON_RADAR.get();
    }

    public static boolean showResourceNumbers() {
        return SHOW_RESOURCE_NUMBERS.get();
    }

    public static boolean lockOnCameraTracking() {
        return LOCK_ON_CAMERA_TRACKING.get();
    }

    public static boolean smoothShoulderCamera() {
        return SMOOTH_SHOULDER_CAMERA.get();
    }

    public static KiTechnique techniqueSlot(int slot) {
        List<? extends String> slots = TECHNIQUE_HOTBAR.get();
        KiTechnique technique = slot >= 0 && slot < TechniqueHotbar.SLOT_COUNT && slot < slots.size()
                ? KiTechnique.byName(slots.get(slot)) : null;
        return isHotbarTechnique(technique) ? technique : null;
    }

    public static boolean isHotbarTechnique(KiTechnique technique) {
        return technique != null && technique.isKiAttack();
    }

    @SubscribeEvent
    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC && !(event instanceof ModConfigEvent.Unloading)) {
            removeNonAttackTechniquesFromHotbar();
        }
    }

    static void removeNonAttackTechniquesFromHotbar() {
        List<? extends String> slots = TECHNIQUE_HOTBAR.get();
        boolean containsUtilityTechnique = slots.stream().anyMatch(value -> {
            KiTechnique technique = KiTechnique.byName(value);
            return technique != null && !technique.isKiAttack();
        });
        if (slots.size() == TechniqueHotbar.SLOT_COUNT && !containsUtilityTechnique) {
            return;
        }
        // Remove old utility assignments and resize the legacy nine-slot layout.
        java.util.ArrayList<String> cleaned = new java.util.ArrayList<>();
        for (int index = 0; index < TechniqueHotbar.SLOT_COUNT; index++) {
            String value = index < slots.size() ? slots.get(index) : "";
            KiTechnique technique = KiTechnique.byName(value);
            cleaned.add(technique != null && technique.isKiAttack() ? technique.commandName() : "");
        }
        TECHNIQUE_HOTBAR.set(cleaned);
        TECHNIQUE_HOTBAR.save();
    }

    public static void setTechniqueSlot(int slot, KiTechnique technique) {
        if (slot < 0 || slot >= TechniqueHotbar.SLOT_COUNT
                || (technique != null && !isHotbarTechnique(technique))) {
            return;
        }
        java.util.ArrayList<String> slots = new java.util.ArrayList<>();
        for (int index = 0; index < TechniqueHotbar.SLOT_COUNT; index++) {
            KiTechnique assigned = index == slot ? technique : techniqueSlot(index);
            slots.add(assigned == null ? "" : assigned.commandName());
        }
        TECHNIQUE_HOTBAR.set(slots);
        TECHNIQUE_HOTBAR.save();
    }
}
