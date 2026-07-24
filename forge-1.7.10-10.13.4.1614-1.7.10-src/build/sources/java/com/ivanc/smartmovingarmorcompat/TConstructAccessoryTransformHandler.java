package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import net.minecraft.client.model.ModelBiped;
import net.minecraftforge.client.event.RenderPlayerEvent;

public class TConstructAccessoryTransformHandler {
    private static final String ARMOR_PROXY_CLIENT = "tconstruct.armor.ArmorProxyClient";
    private static final String[] STATIC_MODELS = new String[] {"wings", "glove", "belt", "vest"};

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void prepareAccessories(RenderPlayerEvent.SetArmorModel event) {
        if (event.slot != 1 && event.slot != 2) {
            return;
        }

        if (!SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        if (source == null) {
            return;
        }

        for (int i = 0; i < STATIC_MODELS.length; i++) {
            prepareStaticModel(STATIC_MODELS[i], source);
        }
    }

    private static void prepareStaticModel(String fieldName, ModelBiped source) {
        try {
            Class<?> type = Class.forName(ARMOR_PROXY_CLIENT);
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof ModelBiped) {
                ArmorModelSynchronizer.prepare((ModelBiped) value, source);
            }
        } catch (Throwable ignored) {
        }
    }
}
