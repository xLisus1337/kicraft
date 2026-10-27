package net.kicraft.kicraft.client;

import net.kicraft.kicraft.content.TrainingDummyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ClientLockOnData {
    private static int targetId = -1;
    private static String targetName = "";
    private static double targetPower;

    private ClientLockOnData() {
    }

    public static void set(int id, String name, double power) {
        if (id < 0 || ClientOozaruFocus.isLocked()) {
            clear();
            return;
        }

        if (targetId != id) {
            LockOnCameraEvents.reset();
        }
        targetId = id;
        targetName = name == null ? "" : name;
        targetPower = Math.max(0.0, power);
    }

    public static void clear() {
        targetId = -1;
        targetName = "";
        targetPower = 0.0;
        LockOnCameraEvents.reset();
    }

    public static boolean isLocked() {
        return targetId >= 0;
    }

    public static int targetId() {
        return targetId;
    }

    public static String targetName() {
        return targetName;
    }

    public static double targetPower() {
        return targetPower;
    }

    public static Entity targetEntity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || targetId < 0) {
            return null;
        }
        return minecraft.level.getEntity(targetId);
    }

    public static Vec3 targetAimPoint(Entity entity, float partialTick) {
        if (entity == null) {
            return Vec3.ZERO;
        }

        AABB box = entity.getBoundingBox();
        if (entity instanceof TrainingDummyEntity) {
            // A player-sized dummy should be targeted at the upper torso, not its eye height.
            return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.56, 0.0);
        }
        double width = Math.max(box.getXsize(), box.getZsize());
        double height = box.getYsize();
        if (width >= 3.0 || height >= 3.0) {
            Vec3 position = entity.getPosition(partialTick);
            Vec3 currentPosition = entity.position();
            double centerXOffset = box.getCenter().x - currentPosition.x;
            double centerZOffset = box.getCenter().z - currentPosition.z;
            double heightRatio = width >= 8.0 || height >= 6.0 ? 0.72 : 0.64;
            double yOffset = box.minY + height * heightRatio - currentPosition.y;
            return position.add(centerXOffset, yOffset, centerZOffset);
        }

        if (entity instanceof LivingEntity living) {
            return living.getEyePosition(partialTick);
        }
        return box.getCenter();
    }
}
