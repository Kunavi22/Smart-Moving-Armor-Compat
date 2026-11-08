package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;

public class BackToolsTransformHandler {
    private static final String ORIGINAL_HANDLER = "backtools.common.core.EventHandler";
    private static final String ORIGINAL_METHOD = "onPlayerRender";
    private static final String BACK_TOOLS_CLASS = "backtools.common.BackTools";
    private static final String RENDERER_HELPER_CLASS = "ichun.client.render.RendererHelper";
    private static final String MORPH_API_CLASS = "morph.api.Api";

    private static boolean originalHandlerRemoved;
    private static boolean lookupAttempted;
    private static Field tickHandlerField;
    private static Field playerToolField;
    private static Field hasMorphModField;
    private static Method getOrientationMethod;
    private static Method renderItemMethod;
    private static Method getMorphEntityMethod;

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

        EntityPlayer player = event.entityPlayer;
        if (player.getHideCape() || player.isInvisible() || isNonPlayerMorph(player)) {
            return;
        }

        ItemStack storedTool = getStoredTool(player);
        if (storedTool == null || ItemStack.areItemStacksEqual(storedTool, copyWithoutDamage(player.getHeldItem()))) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        boolean useSmartTransform = SmartRenderTransformHelper.isRealClientPlayer(player);
        if (useSmartTransform) {
            source = SmartRenderTransformHelper.getActiveSmartModel(source);
        }

        SmartRenderTransformHelper.Transform transform = useSmartTransform
            ? SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART)
            : null;
        if (transform == null) {
            renderTool(player, storedTool, false);
            return;
        }

        try {
            renderTool(player, storedTool, true);
        } finally {
            transform.end();
        }
    }

    private static void renderTool(EntityPlayer player, ItemStack tool, boolean bodyAttached) {
        if (!loadHooks()) {
            return;
        }

        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0.0F, 0.35F, 0.16F);
            if (player.inventory.armorItemInSlot(2) != null) {
                GL11.glTranslatef(
                    0.0F,
                    player.isSneaking() ? -0.1F : 0.0F,
                    player.isSneaking() ? 0.025F : 0.06F);
            }

            // Smart Moving's body transform already contains the crouch pitch and displacement.
            if (player.isSneaking() && !bodyAttached) {
                GL11.glRotatef(28.8F, 1.0F, 0.0F, 0.0F);
                GL11.glTranslatef(0.0F, 0.0F, 0.18F);
            }

            GL11.glRotatef(getOrientation(tool) * -90.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            renderItemMethod.invoke(null, tool);
        } catch (Throwable ignored) {
        } finally {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
    }

    private static ItemStack copyWithoutDamage(ItemStack stack) {
        if (stack == null) {
            return null;
        }

        ItemStack copy = stack.copy();
        copy.setItemDamage(0);
        return copy;
    }

    private static ItemStack getStoredTool(EntityPlayer player) {
        if (!loadHooks()) {
            return null;
        }

        try {
            Object tickHandler = tickHandlerField.get(null);
            if (tickHandler == null) {
                return null;
            }

            Object value = playerToolField.get(tickHandler);
            if (!(value instanceof Map)) {
                return null;
            }

            Object stack = ((Map<?, ?>) value).get(player.getCommandSenderName());
            return stack instanceof ItemStack ? (ItemStack) stack : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float getOrientation(ItemStack tool) {
        try {
            Object value = getOrientationMethod.invoke(null, tool.getItem().getClass());
            return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
        } catch (Throwable ignored) {
            return 0.0F;
        }
    }

    private static boolean isNonPlayerMorph(EntityPlayer player) {
        if (!loadHooks()) {
            return false;
        }

        try {
            if (!hasMorphModField.getBoolean(null)) {
                return false;
            }

            if (getMorphEntityMethod == null) {
                Class<?> morphApi = Class.forName(MORPH_API_CLASS);
                getMorphEntityMethod = morphApi.getMethod(
                    "getMorphEntity", String.class, Boolean.TYPE);
            }

            Object morph = getMorphEntityMethod.invoke(
                null, player.getCommandSenderName(), Boolean.TRUE);
            return morph instanceof EntityLivingBase && !(morph instanceof EntityPlayer);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean loadHooks() {
        if (lookupAttempted) {
            return tickHandlerField != null && playerToolField != null
                && getOrientationMethod != null && renderItemMethod != null;
        }

        lookupAttempted = true;
        try {
            Class<?> backTools = Class.forName(BACK_TOOLS_CLASS);
            tickHandlerField = backTools.getField("tickHandlerClient");
            hasMorphModField = backTools.getField("hasMorphMod");
            getOrientationMethod = backTools.getMethod("getOrientation", Class.class);

            Object tickHandler = tickHandlerField.get(null);
            Class<?> tickHandlerClass = tickHandler != null
                ? tickHandler.getClass()
                : Class.forName("backtools.client.core.TickHandlerClient");
            playerToolField = tickHandlerClass.getField("playerTool");

            Class<?> rendererHelper = Class.forName(RENDERER_HELPER_CLASS);
            renderItemMethod = rendererHelper.getMethod("renderItemIn3d", ItemStack.class);
            return true;
        } catch (Throwable ignored) {
            tickHandlerField = null;
            playerToolField = null;
            hasMorphModField = null;
            getOrientationMethod = null;
            renderItemMethod = null;
            return false;
        }
    }
}
