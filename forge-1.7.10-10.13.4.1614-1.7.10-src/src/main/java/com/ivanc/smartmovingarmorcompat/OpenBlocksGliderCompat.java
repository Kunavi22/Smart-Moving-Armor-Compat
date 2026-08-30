package com.ivanc.smartmovingarmorcompat;

import java.lang.reflect.Method;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

final class OpenBlocksGliderCompat {
    private static final String ORIGINAL_HANDLER = "openblocks.client.GliderPlayerRenderHandler";
    private static final String ENTITY_HANG_GLIDER = "openblocks.common.entity.EntityHangGlider";

    private static boolean originalHandlersRemoved;
    private static boolean lookupDone;
    private static Method isGliderDeployedMethod;

    private OpenBlocksGliderCompat() {
    }

    static void unregisterOriginalHandlers() {
        if (originalHandlersRemoved) {
            return;
        }

        originalHandlersRemoved = ForgeEventBusUtil.unregisterHandlersByClassName(ORIGINAL_HANDLER);
    }

    static boolean isGliderDeployed(EntityPlayer player) {
        Method method = getIsGliderDeployedMethod();
        if (player == null || method == null) {
            return false;
        }

        try {
            Object value = method.invoke(null, player);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void suppressWalkCycle(EntityPlayer player) {
        player.limbSwing = 0.0F;
        player.limbSwingAmount = 0.0F;
        player.prevLimbSwingAmount = 0.0F;
    }

    private static Method getIsGliderDeployedMethod() {
        if (lookupDone) {
            return isGliderDeployedMethod;
        }

        lookupDone = true;
        try {
            Class<?> type = Class.forName(ENTITY_HANG_GLIDER);
            isGliderDeployedMethod = type.getMethod("isGliderDeployed", Entity.class);
        } catch (Throwable ignored) {
            isGliderDeployedMethod = null;
        }
        return isGliderDeployedMethod;
    }
}
