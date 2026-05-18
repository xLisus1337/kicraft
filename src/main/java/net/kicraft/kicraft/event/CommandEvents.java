package net.kicraft.kicraft.event;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.command.KicraftCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Rejestrujemy na głównym FORGE EVENT BUS
@Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandEvents {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        // Podłączamy naszą komendę
        KicraftCommand.register(event.getDispatcher());
    }
}