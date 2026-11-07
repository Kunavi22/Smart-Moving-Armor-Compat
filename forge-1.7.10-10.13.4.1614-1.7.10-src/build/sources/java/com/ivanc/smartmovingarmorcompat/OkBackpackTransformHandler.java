package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;

public class OkBackpackTransformHandler {
    private static final String ORIGINAL_HANDLER = "ruiseki.okbackpack.common.event.ItemRenderEvent";
    private static final String ORIGINAL_METHOD = "onPlayerRender";
    private static final String RENDERER_INTERFACE =
        "ruiseki.okbackpack.client.renderer.player.IPlayerItemRender";
    private static final String RENDER_TYPE =
        "ruiseki.okbackpack.client.renderer.RenderHelpers$RenderType";
    private static final String BAUBLES_HELPERS =
        "ruiseki.okbackpack.common.helpers.BaublesHelpers";

    private static boolean originalHandlerRemoved;
    private static boolean lookupAttempted;
    private static Class<?> rendererInterface;
    private static Method renderMethod;
    private static Method getBaublesMethod;
    private static Object bodyRenderType;
    private static Object headRenderType;

    public static void unregisterOriginalHandlers() {
        if (originalHandlerRemoved) {
            return;
        }

        originalHandlerRemoved =
            ForgeEventBusUtil.unregisterHandlerMethodByClassName(ORIGINAL_HANDLER, ORIGINAL_METHOD);
    }

    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Specials.Post event) {
        unregisterOriginalHandlers();

        if (event.entityLiving.getActivePotionEffect(Potion.invisibility) != null || !loadHooks()) {
            return;
        }

        EntityPlayer player = event.entityPlayer;
        IInventory baubles = getBaubles(player);
        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        boolean useSmartTransform = SmartRenderTransformHelper.isRealClientPlayer(player);
        if (useSmartTransform) {
            source = SmartRenderTransformHelper.getActiveSmartModel(source);
        }

        renderBody(player.inventory, baubles, player, event, source, useSmartTransform);
        renderHead(player.inventory, baubles, player, event);
    }

    private static void renderBody(
        IInventory armor,
        IInventory baubles,
        EntityPlayer player,
        RenderPlayerEvent event,
        ModelBiped source,
        boolean useSmartTransform) {
        SmartRenderTransformHelper.Transform transform = useSmartTransform
            ? SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART)
            : null;

        if (transform == null) {
            dispatchArmor(armor, player, event, bodyRenderType, true, false);
            dispatchInventory(baubles, player, event, bodyRenderType, true, false);
            return;
        }

        try {
            dispatchArmor(armor, player, event, bodyRenderType, false, false);
            dispatchInventory(baubles, player, event, bodyRenderType, false, false);
        } finally {
            transform.end();
        }
    }

    private static void renderHead(
        IInventory armor,
        IInventory baubles,
        EntityPlayer player,
        RenderPlayerEvent.Specials.Post event) {
        float yaw = player.prevRenderYawOffset
            + (player.renderYawOffset - player.prevRenderYawOffset) * event.partialRenderTick;
        float yawOffset = player.prevRotationYawHead
            + (player.rotationYawHead - player.prevRotationYawHead) * event.partialRenderTick;
        float pitch = player.prevRotationPitch
            + (player.rotationPitch - player.prevRotationPitch) * event.partialRenderTick;

        GL11.glPushMatrix();
        try {
            GL11.glRotatef(yawOffset, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(yaw - 270.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 0.0F, 0.0F, 1.0F);
            dispatchArmor(armor, player, event, headRenderType, false, true);
            dispatchInventory(baubles, player, event, headRenderType, false, true);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void dispatchArmor(
        IInventory inventory,
        EntityPlayer player,
        RenderPlayerEvent event,
        Object renderType,
        boolean applySneakRotation,
        boolean translateToHead) {
        if (inventory == null) {
            return;
        }

        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            int slot = inventory.getSizeInventory() - 1 - armorIndex;
            renderStack(
                inventory.getStackInSlot(slot), player, event, renderType,
                applySneakRotation, translateToHead);
        }
    }

    private static void dispatchInventory(
        IInventory inventory,
        EntityPlayer player,
        RenderPlayerEvent event,
        Object renderType,
        boolean applySneakRotation,
        boolean translateToHead) {
        if (inventory == null) {
            return;
        }

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            renderStack(
                inventory.getStackInSlot(i), player, event, renderType,
                applySneakRotation, translateToHead);
        }
    }

    private static void renderStack(
        ItemStack stack,
        EntityPlayer player,
        RenderPlayerEvent event,
        Object renderType,
        boolean applySneakRotation,
        boolean translateToHead) {
        if (stack == null || renderType == null) {
            return;
        }

        Item item = stack.getItem();
        if (item == null || !rendererInterface.isInstance(item)) {
            return;
        }

        GL11.glPushMatrix();
        try {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (applySneakRotation && player.isSneaking()) {
                GL11.glRotatef(28.65F, 1.0F, 0.0F, 0.0F);
            }
            if (translateToHead) {
                translateToHead(player);
            }
            renderMethod.invoke(item, stack, player, event, renderType);
        } catch (Throwable ignored) {
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
    }

    private static void translateToHead(EntityPlayer player) {
        float remoteOffset = player != Minecraft.getMinecraft().thePlayer ? 1.62F : 0.0F;
        double y = remoteOffset - player.getDefaultEyeHeight() + (player.isSneaking() ? 0.0625D : 0.0D);
        GL11.glTranslated(0.0D, y, 0.0D);
    }

    private static IInventory getBaubles(EntityPlayer player) {
        if (getBaublesMethod == null) {
            return null;
        }

        try {
            Object value = getBaublesMethod.invoke(null, player);
            return value instanceof IInventory ? (IInventory) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean loadHooks() {
        if (lookupAttempted) {
            return rendererInterface != null && renderMethod != null
                && bodyRenderType != null && headRenderType != null;
        }

        lookupAttempted = true;
        try {
            rendererInterface = Class.forName(RENDERER_INTERFACE);
            Class<?> renderTypeClass = Class.forName(RENDER_TYPE);
            bodyRenderType = findEnumConstant(renderTypeClass, "BODY");
            headRenderType = findEnumConstant(renderTypeClass, "HEAD");
            renderMethod = rendererInterface.getMethod(
                "render",
                ItemStack.class,
                EntityPlayer.class,
                RenderPlayerEvent.class,
                renderTypeClass);
        } catch (Throwable ignored) {
            rendererInterface = null;
            renderMethod = null;
            bodyRenderType = null;
            headRenderType = null;
        }

        try {
            Class<?> baublesHelpers = Class.forName(BAUBLES_HELPERS);
            getBaublesMethod = baublesHelpers.getMethod("getBaubles", EntityPlayer.class);
        } catch (Throwable ignored) {
            getBaublesMethod = null;
        }

        return rendererInterface != null && renderMethod != null
            && bodyRenderType != null && headRenderType != null;
    }

    private static Object findEnumConstant(Class<?> enumClass, String name) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return null;
        }

        for (int i = 0; i < constants.length; i++) {
            if (name.equals(String.valueOf(constants[i]))) {
                return constants[i];
            }
        }
        return null;
    }
}
