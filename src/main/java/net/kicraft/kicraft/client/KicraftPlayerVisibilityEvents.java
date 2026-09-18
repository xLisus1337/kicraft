package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;

/** Temporarily hides vanilla skin cubes while keeping its renderer, name tag and compatible layers alive. */
@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class KicraftPlayerVisibilityEvents {
    private static final ThreadLocal<Deque<Visibility>> VISIBILITY = ThreadLocal.withInitial(ArrayDeque::new);
    private KicraftPlayerVisibilityEvents() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void before(RenderPlayerEvent.Pre event) {
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        VISIBILITY.get().push(new Visibility(model));
        model.setAllVisible(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void after(RenderPlayerEvent.Post event) {
        Deque<Visibility> stack = VISIBILITY.get();
        if (!stack.isEmpty()) stack.pop().restore();
        if (stack.isEmpty()) VISIBILITY.remove();
    }

    private record Visibility(PlayerModel<AbstractClientPlayer> model,
                              boolean head, boolean hat, boolean body, boolean rightArm, boolean leftArm,
                              boolean rightLeg, boolean leftLeg, boolean jacket, boolean rightSleeve,
                              boolean leftSleeve, boolean rightPants, boolean leftPants) {
        Visibility(PlayerModel<AbstractClientPlayer> model) {
            this(model, model.head.visible, model.hat.visible, model.body.visible,
                    model.rightArm.visible, model.leftArm.visible, model.rightLeg.visible, model.leftLeg.visible,
                    model.jacket.visible, model.rightSleeve.visible, model.leftSleeve.visible,
                    model.rightPants.visible, model.leftPants.visible);
        }

        void restore() {
            model.head.visible = head;
            model.hat.visible = hat;
            model.body.visible = body;
            model.rightArm.visible = rightArm;
            model.leftArm.visible = leftArm;
            model.rightLeg.visible = rightLeg;
            model.leftLeg.visible = leftLeg;
            model.jacket.visible = jacket;
            model.rightSleeve.visible = rightSleeve;
            model.leftSleeve.visible = leftSleeve;
            model.rightPants.visible = rightPants;
            model.leftPants.visible = leftPants;
        }
    }
}
