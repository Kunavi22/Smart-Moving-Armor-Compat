package com.ivanc.smartmovingarmorcompat;

import java.lang.reflect.Method;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

final class EtFuturumElytraCompat {
    private static boolean elytraItemLookupDone;
    private static Method getElytraMethod;

    private static boolean elytraPlayerLookupDone;
    private static Class<?> elytraPlayerClass;
    private static Method isElytraFlyingMethod;
    private static Method getTicksElytraFlyingMethod;

    private EtFuturumElytraCompat() {
    }

    static boolean hasElytra(EntityLivingBase entity) {
        if (entity == null) {
            return false;
        }

        Method method = getElytraMethod();
        if (method == null) {
            return false;
        }

        try {
            return method.invoke(null, entity) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean isElytraFlying(EntityPlayer player) {
        if (player == null || !lookupElytraPlayer()) {
            return false;
        }

        if (!elytraPlayerClass.isInstance(player)) {
            return false;
        }

        try {
            Object value = isElytraFlyingMethod.invoke(player);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static float getTicksElytraFlying(EntityPlayer player) {
        if (player == null || !lookupElytraPlayer() || !elytraPlayerClass.isInstance(player)) {
            return 0.0F;
        }

        try {
            Object value = getTicksElytraFlyingMethod.invoke(player);
            return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
        } catch (Throwable ignored) {
            return 0.0F;
        }
    }

    private static Method getElytraMethod() {
        if (elytraItemLookupDone) {
            return getElytraMethod;
        }

        elytraItemLookupDone = true;
        try {
            Class<?> itemArmorElytra = Class.forName("ganymedes01.etfuturum.items.equipment.ItemArmorElytra");
            getElytraMethod = itemArmorElytra.getMethod("getElytra", EntityLivingBase.class);
        } catch (Throwable ignored) {
            getElytraMethod = null;
        }
        return getElytraMethod;
    }

    private static boolean lookupElytraPlayer() {
        if (elytraPlayerLookupDone) {
            return elytraPlayerClass != null;
        }

        elytraPlayerLookupDone = true;
        try {
            elytraPlayerClass = Class.forName("ganymedes01.etfuturum.elytra.IElytraPlayer");
            isElytraFlyingMethod = elytraPlayerClass.getMethod("etfu$isElytraFlying");
            getTicksElytraFlyingMethod = elytraPlayerClass.getMethod("etfu$getTicksElytraFlying");
        } catch (Throwable ignored) {
            elytraPlayerClass = null;
            isElytraFlyingMethod = null;
            getTicksElytraFlyingMethod = null;
        }

        return elytraPlayerClass != null;
    }
}
