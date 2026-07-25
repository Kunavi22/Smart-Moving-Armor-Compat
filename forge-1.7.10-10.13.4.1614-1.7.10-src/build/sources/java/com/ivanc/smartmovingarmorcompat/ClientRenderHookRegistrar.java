package com.ivanc.smartmovingarmorcompat;

import api.player.render.RenderPlayerAPI;
import cpw.mods.fml.common.Loader;
import net.minecraftforge.common.MinecraftForge;

public final class ClientRenderHookRegistrar {
    private static boolean registered;

    private ClientRenderHookRegistrar() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        RenderPlayerAPI.register(SmartMovingArmorCompat.MODID, SmartMovingArmorRenderPlayerBase.class);
        registerOptionalForgeHandlers();
        registered = true;
    }

    private static void registerOptionalForgeHandlers() {
        if (Loader.isModLoaded("Botania")) {
            BotaniaBaubleTransformHandler.unregisterOriginalHandlers();
            MinecraftForge.EVENT_BUS.register(new BotaniaBaubleTransformHandler());
        }

        if (Loader.isModLoaded("TravellersGear")) {
            MinecraftForge.EVENT_BUS.register(new TravellersGearRenderHandler());
        }

        if (Loader.isModLoaded("TConstruct")) {
            MinecraftForge.EVENT_BUS.register(new TConstructAccessoryTransformHandler());
        }

        if (Loader.isModLoaded("armoredarms")) {
            MinecraftForge.EVENT_BUS.register(new ArmoredArmsSmartMovingGuard());
        }

        if (Loader.isModLoaded("adventurebackpack")) {
            AdventureBackpackTransformHandler.unregisterOriginalHandlers();
            MinecraftForge.EVENT_BUS.register(new AdventureBackpackTransformHandler());
        }

        if (Loader.isModLoaded("Backpack") && BackpackTransformHandler.isRenderingEnabled()) {
            BackpackTransformHandler.unregisterOriginalHandlers();
            MinecraftForge.EVENT_BUS.register(new BackpackTransformHandler());
        }

        if (Loader.isModLoaded("etfuturum")) {
            EtFuturumElytraTransformHandler.unregisterOriginalHandlers();
            MinecraftForge.EVENT_BUS.register(new EtFuturumElytraTransformHandler());
        }
    }
}
