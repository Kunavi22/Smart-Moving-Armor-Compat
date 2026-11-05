package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;
import travellersgear.api.TravellersGearAPI;
import vazkii.botania.api.item.IBaubleRender;
import vazkii.botania.api.item.ICosmeticAttachable;
import vazkii.botania.api.item.IPhantomInkable;

public class AlfheimCapeTransformHandler {
    private static final String ORIGINAL_HANDLER =
        "alfheim.common.integration.travellersgear.TGHandlerBotaniaRenderer";
    private static boolean originalHandlersRemoved;

    public static void unregisterOriginalHandlers() {
        if (originalHandlersRemoved) {
            return;
        }

        try {
            Class.forName(ORIGINAL_HANDLER);
        } catch (Throwable ignored) {
            return;
        }
        originalHandlersRemoved = ForgeEventBusUtil.unregisterHandlersByClassName(ORIGINAL_HANDLER);
    }

    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Specials.Post event) {
        unregisterOriginalHandlers();

        if (!SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)
            || event.entityLiving.getActivePotionEffect(Potion.invisibility) != null) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getActiveSmartModel(
            SmartRenderTransformHelper.getSmartModel(event.renderer));
        ItemStack[] equipment = TravellersGearAPI.getExtendedInventory(event.entityPlayer);
        if (source == null || equipment == null) {
            return;
        }

        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART);
        if (transform == null) {
            dispatchBodyRenders(equipment, event);
            return;
        }

        try {
            GL11.glPushMatrix();
            try {
                // Alfheim's bauble renderers add this vanilla sneak tilt themselves.
                if (event.entityPlayer.isSneaking()) {
                    GL11.glRotatef(-28.64789F, 1.0F, 0.0F, 0.0F);
                }
                dispatchBodyRenders(equipment, event);
            } finally {
                GL11.glPopMatrix();
            }
        } finally {
            transform.end();
        }
    }

    private static void dispatchBodyRenders(ItemStack[] equipment, RenderPlayerEvent event) {
        for (int i = 0; i < equipment.length; i++) {
            ItemStack stack = equipment[i];
            if (stack == null) {
                continue;
            }

            Item item = stack.getItem();
            if (item instanceof IPhantomInkable && ((IPhantomInkable) item).hasPhantomInk(stack)) {
                continue;
            }

            if (item instanceof ICosmeticAttachable) {
                ItemStack cosmetic = ((ICosmeticAttachable) item).getCosmeticItem(stack);
                if (cosmetic != null && cosmetic.getItem() instanceof IBaubleRender) {
                    renderBauble(cosmetic, event);
                }
            }
            if (item instanceof IBaubleRender) {
                renderBauble(stack, event);
            }
        }
    }

    private static void renderBauble(ItemStack stack, RenderPlayerEvent event) {
        GL11.glPushMatrix();
        try {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            ((IBaubleRender) stack.getItem()).onPlayerBaubleRender(
                stack,
                event,
                IBaubleRender.RenderType.BODY);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
