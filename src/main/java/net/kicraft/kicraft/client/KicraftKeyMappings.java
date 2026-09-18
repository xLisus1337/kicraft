package net.kicraft.kicraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.kicraft.kicraft.Kicraft;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KicraftKeyMappings {
    public static final String CATEGORY = "key.categories.kicraft";

    public static final KeyMapping OPEN_TRANSFORMATIONS = new KeyMapping(
            "key.kicraft.transformation_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    );

    public static final KeyMapping OPEN_TRAINING = new KeyMapping(
            "key.kicraft.training_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    public static final KeyMapping CHARGE_KI = new KeyMapping(
            "key.kicraft.charge_ki",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    public static final KeyMapping DASH = new KeyMapping(
            "key.kicraft.dodge",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY
    );

    public static final KeyMapping BLOCK = new KeyMapping(
            "key.kicraft.block",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );

    public static final KeyMapping LOCK_ON = new KeyMapping(
            "key.kicraft.lock_on",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY
    );

    public static final KeyMapping CYCLE_LOCK_ON = new KeyMapping(
            "key.kicraft.cycle_lock_on",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    public static final KeyMapping SHOULDER_CAMERA = new KeyMapping(
            "key.kicraft.shoulder_camera",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY
    );

    public static final KeyMapping TRANSFORM = new KeyMapping(
            "key.kicraft.transform",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping TRANSFORM_OFF = new KeyMapping(
            "key.kicraft.transform_off",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    private KicraftKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TRANSFORMATIONS);
        event.register(OPEN_TRAINING);
        event.register(CHARGE_KI);
        event.register(DASH);
        event.register(BLOCK);
        event.register(LOCK_ON);
        event.register(CYCLE_LOCK_ON);
        event.register(SHOULDER_CAMERA);
        event.register(TRANSFORM);
        event.register(TRANSFORM_OFF);
    }
}
