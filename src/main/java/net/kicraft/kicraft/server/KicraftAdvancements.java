package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class KicraftAdvancements {
    private static final ResourceLocation BERSERK = new ResourceLocation(Kicraft.MODID, "berserk");
    private static final ResourceLocation GOD_KI = new ResourceLocation(Kicraft.MODID, "god_ki");
    private static final ResourceLocation DRAGON_RADAR = new ResourceLocation(Kicraft.MODID, "dragon_radar");

    private KicraftAdvancements() {
    }

    public static void awardBerserk(ServerPlayer player) {
        award(player, BERSERK, "born_lssj");
    }

    public static void awardGodKi(ServerPlayer player) {
        award(player, GOD_KI, "produce_god_ki");
    }

    public static void awardDragonRadar(ServerPlayer player) {
        award(player, DRAGON_RADAR, "obtain_dragon_radar");
    }

    private static void award(ServerPlayer player, ResourceLocation id, String criterion) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
