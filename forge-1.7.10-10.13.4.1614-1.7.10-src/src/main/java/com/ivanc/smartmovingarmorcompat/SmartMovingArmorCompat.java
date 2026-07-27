package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(
    modid = SmartMovingArmorCompat.MODID,
    name = "Smart Moving Armor Compat",
    version = SmartMovingArmorCompat.VERSION,
    acceptableRemoteVersions = "*",
    dependencies = "required-after:PlayerAPI;required-after:RenderPlayerAPI;required-after:SmartRender;required-after:SmartMoving;after:Thaumcraft;after:Botania;after:TravellersGear;after:armoredarms;after:adventurebackpack;after:Backpack;after:EMT;after:TConstruct;after:etfuturum;after:GalaxySpace;after:hbm"
)
public class SmartMovingArmorCompat {
    public static final String MODID = "smartmovingarmorcompat";
    public static final String VERSION = "1.0.26";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        SmartMovingSneakPlayerBase.register();

        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientRenderHookRegistrar.register();
        }
    }
}
