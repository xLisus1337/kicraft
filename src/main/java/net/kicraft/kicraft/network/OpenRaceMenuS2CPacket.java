package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.CharacterCreationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenRaceMenuS2CPacket {

    public OpenRaceMenuS2CPacket() {
        // Pusty konstruktor, nie przesyłamy żadnych danych, dajemy tylko sygnał
    }

    public OpenRaceMenuS2CPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Kod wykonuje się na KLIENCIE
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // Odpalamy nasze technologiczne menu!
                mc.setScreen(new CharacterCreationScreen());
            }
        });
        return true;
    }
}