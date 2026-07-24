package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(
    modid = SmartMovingArmorCompat.MODID,
    name = "Smart Moving Armor Compat",
    version = SmartMovingArmorCompat.VERSION,
    acceptableRemoteVersions = "*",
    dependencies = "required-after:RenderPlayerAPI;required-after:SmartRender;required-after:SmartMoving;after:Botania;after:TravellersGear;after:armoredarms;after:adventurebackpack;after:EMT;after:TConstruct;after:etfuturum;after:GalaxySpace"
)
public class SmartMovingArmorCompat {
    public static final String MODID = "smartmovingarmorcompat";
    public static final String VERSION = "1.0.14";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientRenderHookRegistrar.register();
        }
    }
}
