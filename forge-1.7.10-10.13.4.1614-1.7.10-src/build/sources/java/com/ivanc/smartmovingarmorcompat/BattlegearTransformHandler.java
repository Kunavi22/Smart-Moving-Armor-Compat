package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mods.battlegear2.api.IBackSheathedRender;
import mods.battlegear2.api.IDyable;
import mods.battlegear2.api.RenderPlayerEventChild;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.api.quiver.IArrowContainer2;
import mods.battlegear2.api.quiver.QuiverArrowRegistry;
import mods.battlegear2.api.shield.IShield;
import mods.battlegear2.client.model.QuiverModel;
import mods.battlegear2.client.utils.BattlegearRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.smart.render.ModelRotationRenderer;
import org.lwjgl.opengl.GL11;

public class BattlegearTransformHandler {
    private static final String ORIGINAL_HANDLER = "mods.battlegear2.client.BattlegearClientEvents";
    private static final String ORIGINAL_METHOD = "render3rdPersonBattlemode";
    private static final ResourceLocation QUIVER_DETAILS =
        new ResourceLocation("battlegear2", "textures/armours/quiver/QuiverDetails.png");
    private static final ResourceLocation QUIVER_BASE =
        new ResourceLocation("battlegear2", "textures/armours/quiver/QuiverBase.png");

    private static boolean registered;
    private final QuiverModel quiverModel = new QuiverModel();

    public static void register() {
        if (registered) {
            return;
        }

        BattlegearTransformHandler handler = new BattlegearTransformHandler();
        BattlegearUtils.RENDER_BUS.register(handler);
        if (ForgeEventBusUtil.unregisterHandlerMethodByClassName(ORIGINAL_HANDLER, ORIGINAL_METHOD)) {
            MinecraftForge.EVENT_BUS.register(handler);
        }
        registered = true;
    }

    @SubscribeEvent
    public void renderThirdPersonBattlegear(RenderPlayerEvent.Specials.Post event) {
        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        if (SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            source = SmartRenderTransformHelper.getActiveSmartModel(source);
        }
        if (source == null) {
            return;
        }

        BattlegearRenderHelper.renderItemIn3rdPerson(
            event.entityPlayer, source, event.partialRenderTick);
        renderQuiver(event.entityPlayer, source);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void alignOffhand(RenderPlayerEventChild.PostRenderPlayerElement event) {
        if (event.isFirstPerson
            || event.type != RenderPlayerEventChild.PlayerElementType.Offhand
            || !SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getActiveSmartModel(
            SmartRenderTransformHelper.getSmartModel(event.renderer));
        ModelRenderer leftArm = ArmorModelSynchronizer.getPart(
            source, SmartRenderTransformHelper.LEFT_ARM_PART);
        if (!(leftArm instanceof ModelRotationRenderer)) {
            return;
        }

        // Battlegear owns one matrix level around this event and pops it after rendering the item.
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        ((ModelRotationRenderer) leftArm).preTransforms(
            SmartRenderTransformHelper.MODEL_SCALE, false, true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void alignSheathedItem(RenderPlayerEventChild.PreRenderSheathed event) {
        if (!SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getActiveSmartModel(
            SmartRenderTransformHelper.getSmartModel(event.renderer));
        ModelRenderer body = ArmorModelSynchronizer.getPart(
            source, SmartRenderTransformHelper.BODY_PART);
        if (!(body instanceof ModelRotationRenderer)) {
            return;
        }

        GL11.glPopMatrix();
        GL11.glPushMatrix();
        ((ModelRotationRenderer) body).preTransforms(
            SmartRenderTransformHelper.MODEL_SCALE, false, true);
        applySheathedItemTransform(event);
    }

    private static void applySheathedItemTransform(RenderPlayerEventChild.PreRenderSheathed event) {
        ItemStack stack = event.element;
        if (stack == null) {
            return;
        }

        boolean isMainHand =
            event.type == RenderPlayerEventChild.PlayerElementType.ItemMainhandSheathed;
        int backCount = event.isOnBack ? Math.max(0, event.backCount - 1) : event.backCount;

        if (event.isOnBack) {
            if (stack.getItem() instanceof IBackSheathedRender) {
                ((IBackSheathedRender) stack.getItem()).preRenderBackSheathed(
                    stack, backCount, event, isMainHand);
            } else if (!isMainHand && stack.getItem() instanceof IShield) {
                GL11.glScalef(-0.6F, -0.6F, 0.6F);
                GL11.glTranslatef(0.0F, -1.0F, 0.0F);
            } else {
                GL11.glScalef(isMainHand ? 0.6F : -0.6F, 0.6F, 0.6F);
            }

            GL11.glTranslatef(-0.5F, 0.0F, 0.375F);
            GL11.glRotatef(-5.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(130.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(
                0.0F, 0.0F,
                0.25F - backCount * 2.0F * SmartRenderTransformHelper.MODEL_SCALE);
            return;
        }

        GL11.glScalef(0.6F, 0.6F, 0.6F);
        GL11.glTranslatef(isMainHand ? 0.5F : -0.4375F, 1.0F, -0.25F);
        if (hasBodyArmor(event.entityPlayer)) {
            GL11.glTranslatef(isMainHand ? 0.125F : -0.125F, 0.0F, 0.0F);
        }
        GL11.glRotatef(35.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(40.0F, 0.0F, 1.0F, 0.0F);
    }

    private static boolean hasBodyArmor(EntityPlayer player) {
        return player.getEquipmentInSlot(3) != null || player.getEquipmentInSlot(2) != null;
    }

    private void renderQuiver(EntityPlayer player, ModelBiped source) {
        ItemStack stack = QuiverArrowRegistry.getArrowContainer(player);
        if (stack == null || !(stack.getItem() instanceof IArrowContainer2)) {
            return;
        }

        IArrowContainer2 quiver = (IArrowContainer2) stack.getItem();
        if (!quiver.renderDefaultQuiverModel(stack)) {
            return;
        }

        int arrowCount = 0;
        for (int i = 0; i < quiver.getSlotCount(stack); i++) {
            if (quiver.getStackInSlot(stack, i) != null) {
                arrowCount++;
            }
        }

        ModelRenderer body = ArmorModelSynchronizer.getPart(
            source, SmartRenderTransformHelper.BODY_PART);
        if (body == null) {
            return;
        }

        GL11.glPushMatrix();
        try {
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().getTextureManager().bindTexture(QUIVER_DETAILS);
            if (player.getEquipmentInSlot(3) != null) {
                GL11.glTranslatef(0.0F, 0.0F, SmartRenderTransformHelper.MODEL_SCALE);
            }

            if (body instanceof ModelRotationRenderer) {
                ((ModelRotationRenderer) body).preTransforms(
                    SmartRenderTransformHelper.MODEL_SCALE, false, true);
            } else {
                body.postRender(SmartRenderTransformHelper.MODEL_SCALE);
            }

            GL11.glScalef(1.05F, 1.05F, 1.05F);
            this.quiverModel.render(arrowCount, SmartRenderTransformHelper.MODEL_SCALE);
            Minecraft.getMinecraft().getTextureManager().bindTexture(QUIVER_BASE);
            if (stack.getItem() instanceof IDyable) {
                int color = ((IDyable) stack.getItem()).getColor(stack);
                GL11.glColor3f(
                    (float) (color >> 16 & 255) / 255.0F,
                    (float) (color >> 8 & 255) / 255.0F,
                    (float) (color & 255) / 255.0F);
            }
            this.quiverModel.render(0, SmartRenderTransformHelper.MODEL_SCALE);
        } finally {
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
    }
}
