package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;

public class EtFuturumElytraTransformHandler {
    private static final String ORIGINAL_HANDLER = "ganymedes01.etfuturum.core.handlers.ClientEventHandler";
    private static final String ORIGINAL_SET_ARMOR_METHOD = "renderPlayerSetArmour";
    private static final String LAYER_BETTER_ELYTRA = "ganymedes01.etfuturum.client.renderer.entity.elytra.LayerBetterElytra";
    private static final String SPECTATOR_MODE = "ganymedes01.etfuturum.spectator.SpectatorMode";
    private static final String CONFIG_FUNCTIONS = "ganymedes01.etfuturum.configuration.configs.ConfigFunctions";

    private static boolean originalHandlerRemoved;
    private static boolean layerLookupDone;
    private static Method doRenderLayerMethod;
    private static boolean spectatorLookupDone;
    private static Method isSpectatorMethod;
    private static boolean configLookupDone;
    private static Field transparentArmorField;

    public static void unregisterOriginalHandlers() {
        if (originalHandlerRemoved) {
            return;
        }

        originalHandlerRemoved =
            ForgeEventBusUtil.unregisterHandlerMethodByClassName(ORIGINAL_HANDLER, ORIGINAL_SET_ARMOR_METHOD);
    }

    @SubscribeEvent
    public void renderPlayerSetArmour(RenderPlayerEvent.SetArmorModel event) {
        unregisterOriginalHandlers();

        if (event.slot == 2) {
            renderElytra(event);
        }

        applyEtFuturumSetArmorTail(event);
    }

    private static void renderElytra(RenderPlayerEvent.SetArmorModel event) {
        if (!EtFuturumElytraCompat.hasElytra(event.entityLiving)) {
            return;
        }

        SmartRenderTransformHelper.Transform transform = null;
        if (SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
            if (source != null) {
                transform = SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART);
            }
        }

        try {
            renderEtFuturumLayer(event.entityLiving, event.entityPlayer, event.partialRenderTick);
        } finally {
            if (transform != null) {
                transform.end();
            }
        }
    }

    private static void renderEtFuturumLayer(EntityLivingBase entity, EntityPlayer player, float partialTicks) {
        Method method = getDoRenderLayerMethod();
        if (method == null) {
            return;
        }

        float limbSwingAmount = EtFuturumElytraCompat.isElytraFlying(player) ? 0.0F : player.limbSwingAmount;
        float ageInTicks = (float) player.ticksExisted + partialTicks;
        try {
            method.invoke(
                null,
                entity,
                Float.valueOf(player.limbSwing),
                Float.valueOf(limbSwingAmount),
                Float.valueOf(partialTicks),
                Float.valueOf(ageInTicks),
                Float.valueOf(SmartRenderTransformHelper.MODEL_SCALE));
        } catch (Throwable ignored) {
        }
    }

    private static Method getDoRenderLayerMethod() {
        if (layerLookupDone) {
            return doRenderLayerMethod;
        }

        layerLookupDone = true;
        try {
            Class<?> layer = Class.forName(LAYER_BETTER_ELYTRA);
            doRenderLayerMethod = layer.getMethod(
                "doRenderLayer",
                EntityLivingBase.class,
                Float.TYPE,
                Float.TYPE,
                Float.TYPE,
                Float.TYPE,
                Float.TYPE);
        } catch (Throwable ignored) {
            doRenderLayerMethod = null;
        }
        return doRenderLayerMethod;
    }

    private static void applyEtFuturumSetArmorTail(RenderPlayerEvent.SetArmorModel event) {
        if (isSpectator(event.entityPlayer)) {
            event.result = 0;
            return;
        }

        if (isTransparentArmorEnabled()) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private static boolean isSpectator(EntityPlayer player) {
        Method method = getIsSpectatorMethod();
        if (method == null) {
            return false;
        }

        try {
            Object value = method.invoke(null, player);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method getIsSpectatorMethod() {
        if (spectatorLookupDone) {
            return isSpectatorMethod;
        }

        spectatorLookupDone = true;
        try {
            Class<?> spectatorMode = Class.forName(SPECTATOR_MODE);
            isSpectatorMethod = spectatorMode.getMethod("isSpectator", EntityPlayer.class);
        } catch (Throwable ignored) {
            isSpectatorMethod = null;
        }
        return isSpectatorMethod;
    }

    private static boolean isTransparentArmorEnabled() {
        Field field = getTransparentArmorField();
        if (field == null) {
            return false;
        }

        try {
            return field.getBoolean(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Field getTransparentArmorField() {
        if (configLookupDone) {
            return transparentArmorField;
        }

        configLookupDone = true;
        try {
            Class<?> configFunctions = Class.forName(CONFIG_FUNCTIONS);
            transparentArmorField = configFunctions.getField("enableTransparentAmour");
        } catch (Throwable ignored) {
            transparentArmorField = null;
        }
        return transparentArmorField;
    }
}
