package com.ivanc.smartmovingarmorcompat;

import api.player.model.ModelPlayerAPI;
import api.player.model.ModelPlayerBaseSorting;
import api.player.render.RenderPlayerAPI;
import cpw.mods.fml.common.Loader;
import net.minecraftforge.common.MinecraftForge;

public final class ClientRenderHookRegistrar {
    private static final String SMART_MOVING_BASE = "Smart Moving";
    private static final String SMART_RENDER_BASE = "Smart Render";

    private static boolean registered;

    private ClientRenderHookRegistrar() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        RenderPlayerAPI.register(SmartMovingArmorCompat.MODID, SmartMovingArmorRenderPlayerBase.class);
        registerModelHooks();
        registerOptionalForgeHandlers();
        registered = true;
    }

    private static void registerModelHooks() {
        ModelPlayerBaseSorting sorting = new ModelPlayerBaseSorting();
        sorting.setAfterSetRotationAnglesInferiors(new String[] {SMART_RENDER_BASE, SMART_MOVING_BASE});
        ModelPlayerAPI.register(
            SmartMovingArmorCompat.MODID + ".model",
            SmartMovingArmorModelPlayerBase.class,
            sorting);
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

        if (Loader.isModLoaded("hbm")) {
            MinecraftForge.EVENT_BUS.register(new HbmArmorModTransformHandler());
        }
    }
}
