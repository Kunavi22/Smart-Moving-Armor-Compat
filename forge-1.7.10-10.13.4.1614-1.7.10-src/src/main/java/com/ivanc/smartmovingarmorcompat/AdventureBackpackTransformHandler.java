package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;

public class AdventureBackpackTransformHandler {
    private static final String ORIGINAL_HANDLER = "com.darkona.adventurebackpack.handlers.RenderHandler";
    private static final String BACKPACK_MODEL = "com.darkona.adventurebackpack.client.models.ModelBackpackArmor";
    private static final String COPTER_MODEL = "com.darkona.adventurebackpack.client.models.ModelCopterPack";
    private static final String COAL_JETPACK_MODEL = "com.darkona.adventurebackpack.client.models.ModelCoalJetpack";
    private static boolean originalHandlersRemoved;

    public static void unregisterOriginalHandlers() {
        if (originalHandlersRemoved) {
            return;
        }

        originalHandlersRemoved = ForgeEventBusUtil.unregisterHandlersByClassName(ORIGINAL_HANDLER);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void playerSpecialsRendering(RenderPlayerEvent.Specials.Pre event) {
        unregisterOriginalHandlers();

        if (!SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            return;
        }

        if (!isBackRenderingEnabled() || event.entityPlayer.isInvisible()) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        if (source == null) {
            return;
        }

        ItemStack wearable = getWearingWearable(event.entityPlayer);
        if (wearable == null || getTranslucencyLevel(wearable) == 2) {
            return;
        }

        ItemStack wearableCopy = wearable.copy();
        ModelBiped model = getWearableModel(wearableCopy);
        ResourceLocation texture = getWearableTexture(wearableCopy);
        if (model == null || texture == null) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART);
        if (transform == null) {
            renderFallback(model, event.entityPlayer);
            return;
        }

        GL11.glPushAttrib(4096);
        try {
            GL11.glEnable(32826);
            if (!renderKnownWearable(model, event.entityPlayer, wearableCopy)) {
                renderFallback(model, event.entityPlayer);
            }
        } finally {
            GL11.glPopAttrib();
            transform.end();
        }
    }

    private static boolean renderKnownWearable(ModelBiped model, EntityPlayer player, ItemStack stack) {
        String className = model.getClass().getName();
        if (BACKPACK_MODEL.equals(className)) {
            return invokeBodyOffsetMethod(model, "renderBackpack", new Class<?>[] {Float.class},
                new Object[] {Float.valueOf(SmartRenderTransformHelper.MODEL_SCALE * 0.9F)});
        }

        if (COPTER_MODEL.equals(className)) {
            return invokeBodyOffsetMethod(model, "renderCopterPack", new Class<?>[] {Entity.class, Float.TYPE},
                new Object[] {player, Float.valueOf(SmartRenderTransformHelper.MODEL_SCALE)});
        }

        if (COAL_JETPACK_MODEL.equals(className)) {
            return invokeBodyOffsetMethod(model, "renderCoalJetpack", new Class<?>[] {Float.TYPE},
                new Object[] {Float.valueOf(SmartRenderTransformHelper.MODEL_SCALE)});
        }

        return false;
    }

    private static boolean invokeBodyOffsetMethod(ModelBiped model, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method method = findMethod(model.getClass(), methodName, parameterTypes);
            if (method == null) {
                return false;
            }

            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(model.bipedBody.offsetX, model.bipedBody.offsetY, model.bipedBody.offsetZ);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                method.invoke(model, args);
            } finally {
                GL11.glPopMatrix();
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void renderFallback(ModelBiped model, EntityPlayer player) {
        try {
            model.render(player, 0.0F, 0.0F, (float) player.ticksExisted, 0.0F, 0.0F, SmartRenderTransformHelper.MODEL_SCALE);
        } catch (Throwable ignored) {
        }
    }

    private static ItemStack getWearingWearable(EntityPlayer player) {
        try {
            Class<?> type = Class.forName("com.darkona.adventurebackpack.util.Wearing");
            Method method = type.getMethod("getWearingWearable", EntityPlayer.class);
            Object value = method.invoke(null, player);
            return value instanceof ItemStack ? (ItemStack) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ModelBiped getWearableModel(ItemStack wearable) {
        try {
            Method method = wearable.getItem().getClass().getMethod("getWearableModel", ItemStack.class);
            Object value = method.invoke(wearable.getItem(), wearable);
            return value instanceof ModelBiped ? (ModelBiped) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ResourceLocation getWearableTexture(ItemStack wearable) {
        try {
            Method method = wearable.getItem().getClass().getMethod("getWearableTexture", ItemStack.class);
            Object value = method.invoke(wearable.getItem(), wearable);
            return value instanceof ResourceLocation ? (ResourceLocation) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isBackRenderingEnabled() {
        try {
            Class<?> type = Class.forName("com.darkona.adventurebackpack.config.ConfigHandler");
            Field field = type.getField("enableBackRendering");
            return field.getBoolean(null);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static int getTranslucencyLevel(ItemStack wearable) {
        try {
            Class<?> type = Class.forName("com.darkona.adventurebackpack.util.EnchUtils");
            Method method = type.getMethod("getTranslucencyLevel", ItemStack.class);
            Object value = method.invoke(null, wearable);
            return value instanceof Number ? ((Number) value).intValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static Method findMethod(Class<?> type, String methodName, Class<?>[] parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }
}
