package com.ivanc.smartmovingarmorcompat;

import com.artur114.armoredarms.api.events.InitModelManagersEvent;
import com.artur114.armoredarms.client.layers.ArmRenderLayerArmor;
import com.artur114.armoredarms.core.util.ShapelessLocation;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ArmoredArmsSmartMovingGuard {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void registerArmorModelManager(InitModelManagersEvent event) {
        if (event.managersSL() != null && ArmRenderLayerArmor.class.isAssignableFrom(event.layer())) {
            event.registerManager(new ArmoredArmsArmorModelManager(), ShapelessLocation.ABSOLUTE);
        }
    }
}
