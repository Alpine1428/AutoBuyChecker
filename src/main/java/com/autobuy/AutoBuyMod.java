package com.autobuy;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class AutoBuyMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("autob")
                    .then(ClientCommandManager.literal("on").executes(ctx -> { ChatHandler.enable(); return 1; }))
                    .then(ClientCommandManager.literal("off").executes(ctx -> { ChatHandler.disable(); return 1; }))
                    .then(ClientCommandManager.literal("status").executes(ctx -> { ChatHandler.sendStatus(); return 1; }))
            );
        });
    }
}
