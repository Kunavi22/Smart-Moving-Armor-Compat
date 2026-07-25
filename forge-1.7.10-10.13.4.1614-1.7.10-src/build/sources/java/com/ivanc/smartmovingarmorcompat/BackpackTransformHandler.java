package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;

public class BackpackTransformHandler {
    private static final String ORIGINAL_HANDLER = "de.eydamos.backpack.handler.EventHandlerClientOnly";
    private static final String WORKER_CLASS = "de.eydamos.backpack.model.BackpackModelWorker";
    private static final String CONFIG_CLASS = "de.eydamos.backpack.misc.ConfigurationBackpack";
    private static final String CONSTANTS_CLASS = "de.eydamos.backpack.misc.Constants";
    private static final String BACKPACK_META = "pBackpackMeta";
    private static final float VANILLA_SNEAK_BODY_ROTATION_DEGREES = -28.64789F;

    private static boolean originalHandlersRemoved;
    private static boolean lookupAttempted;
    private static Object modelWorker;
    private static Method getModelMethod;
    private static Method getColorMethod;
    private static Field textureField;

    public static void unregisterOriginalHandlers() {
        if (originalHandlersRemoved) {
            return;
        }

        originalHandlersRemoved = ForgeEventBusUtil.unregisterHandlersByClassName(ORIGINAL_HANDLER);
    }

    public static boolean isRenderingEnabled() {
        try {
            Class<?> type = Class.forName(CONFIG_CLASS);
            Field field = type.getField("RENDER_BACKPACK_MODEL");
            return field.getBoolean(null);
        } catch (Throwable ignored) {
            return true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void render(RenderPlayerEvent.Specials.Pre event) {
        unregisterOriginalHandlers();

        EntityPlayer player = event.entityPlayer;
        if (!SmartRenderTransformHelper.isRealClientPlayer(player) || player.isInvisible() || !isRenderingEnabled()) {
            return;
        }

        NBTTagCompound playerData = player.getEntityData();
        if (playerData == null || !playerData.hasKey(BACKPACK_META)) {
            return;
        }

        int meta = playerData.getInteger(BACKPACK_META);
        if (meta == -1) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        ModelBiped model = getModel(meta);
        ResourceLocation texture = getTexture();
        float[] color = getColor(meta);
        if (source == null || model == null || texture == null || color == null) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART);
        if (transform == null) {
            renderModel(model, player, color, false);
            return;
        }

        try {
            renderModel(model, player, color, true);
        } finally {
            transform.end();
        }
    }

    private static void renderModel(ModelBiped model, EntityPlayer player, float[] color, boolean cancelVanillaSneakTilt) {
        GL11.glPushMatrix();
        try {
            GL11.glColor4f(color[0], color[1], color[2], 1.0F);
            if (cancelVanillaSneakTilt && player.isSneaking()) {
                GL11.glRotatef(VANILLA_SNEAK_BODY_ROTATION_DEGREES, 1.0F, 0.0F, 0.0F);
            }
            model.render(player, 0.0F, 0.0F, (float) player.ticksExisted, 0.0F, 0.0F,
                SmartRenderTransformHelper.MODEL_SCALE);
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
    }

    private static ModelBiped getModel(int meta) {
        if (!loadBackpackHooks()) {
            return null;
        }

        try {
            Object value = getModelMethod.invoke(modelWorker, Integer.valueOf(meta));
            return value instanceof ModelBiped ? (ModelBiped) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float[] getColor(int meta) {
        if (!loadBackpackHooks()) {
            return null;
        }

        try {
            Object value = getColorMethod.invoke(modelWorker, Integer.valueOf(meta));
            return value instanceof float[] ? (float[]) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ResourceLocation getTexture() {
        if (!loadBackpackHooks()) {
            return null;
        }

        try {
            Object value = textureField.get(null);
            return value instanceof ResourceLocation ? (ResourceLocation) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean loadBackpackHooks() {
        if (lookupAttempted) {
            return modelWorker != null && getModelMethod != null && getColorMethod != null && textureField != null;
        }

        lookupAttempted = true;
        try {
            Class<?> workerType = Class.forName(WORKER_CLASS);
            modelWorker = workerType.newInstance();
            getModelMethod = workerType.getMethod("getModel", Integer.TYPE);
            getColorMethod = workerType.getMethod("getColor", Integer.TYPE);

            Class<?> constantsType = Class.forName(CONSTANTS_CLASS);
            textureField = constantsType.getField("modelTexture");
            return true;
        } catch (Throwable ignored) {
            modelWorker = null;
            getModelMethod = null;
            getColorMethod = null;
            textureField = null;
            return false;
        }
    }
}
