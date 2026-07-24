package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import travellersgear.api.RenderTravellersGearEvent;

public class TravellersGearRenderHandler {
    private static final String TRAVELLERS_CLOAK_MODEL = "travellersgear.client.ModelCloak";
    private static final String WITCHING_GADGETS_CLOAK_MODEL = "witchinggadgets.client.render.ModelCloak";
    private static final String WITCHING_GADGETS_KAMA_MODEL = "witchinggadgets.client.render.ModelKama";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void renderTravellersGear(RenderTravellersGearEvent event) {
        if (!event.shouldRender || event.stack == null || event.stack.getItem() == null) {
            return;
        }

        if (!SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        if (source == null) {
            return;
        }

        ArmorRenderData renderData = getArmorRenderData(event.entityPlayer, event.stack);
        if (renderData == null || renderData.model == null) {
            return;
        }

        event.shouldRender = false;
        bindArmorTexture(event.stack, event.entityPlayer, renderData.armorSlot);
        syncAndRender(renderData.model, source, event.entityPlayer, event.renderer, event.partialRenderTick);
    }

    private static void syncAndRender(ModelBiped model, ModelBiped source, EntityPlayer player, RenderPlayer renderer, float partialTicks) {
        if (usesBodyTransform(model)) {
            renderWithBodyTransform(model, source, player, partialTicks);
            return;
        }

        ArmorModelSynchronizer.prepare(model, source);
        renderModel(model, player, partialTicks);
    }

    private static boolean usesBodyTransform(ModelBiped model) {
        String className = model.getClass().getName();
        return TRAVELLERS_CLOAK_MODEL.equals(className) ||
            WITCHING_GADGETS_CLOAK_MODEL.equals(className) ||
            WITCHING_GADGETS_KAMA_MODEL.equals(className);
    }

    private static void renderWithBodyTransform(ModelBiped model, ModelBiped source, EntityPlayer player, float partialTicks) {
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART);
        try {
            renderModel(model, player, partialTicks);
        } finally {
            if (transform != null) {
                transform.end();
            }
        }
    }

    private static void renderModel(ModelBiped model, EntityPlayer player, float partialTicks) {
        float bodyYaw = interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
        float headYaw = interpolateRotation(player.prevRotationYawHead, player.rotationYawHead, partialTicks);
        float relativeHeadYaw;

        if (player.isRiding() && player.ridingEntity instanceof EntityLivingBase) {
            EntityLivingBase mount = (EntityLivingBase) player.ridingEntity;
            bodyYaw = interpolateRotation(mount.prevRenderYawOffset, mount.renderYawOffset, partialTicks);
            relativeHeadYaw = MathHelper.wrapAngleTo180_float(headYaw - bodyYaw);
            relativeHeadYaw = Math.min(85.0F, Math.max(-85.0F, relativeHeadYaw));
            bodyYaw = headYaw - relativeHeadYaw;
            if (relativeHeadYaw * relativeHeadYaw > 2500.0F) {
                bodyYaw += relativeHeadYaw * 0.2F;
            }
        }

        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float ageInTicks = (float) player.ticksExisted + partialTicks;
        float limbSwingAmount = Math.min(1.0F, player.prevLimbSwingAmount + (player.limbSwingAmount - player.prevLimbSwingAmount) * partialTicks);
        float limbSwing = (float) (player.isChild() ? 3 : 1) *
            (player.limbSwing - player.limbSwingAmount * (1.0F - partialTicks));

        model.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);
        model.render(player, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, SmartRenderTransformHelper.MODEL_SCALE);
    }

    private static ArmorRenderData getArmorRenderData(EntityPlayer player, ItemStack stack) {
        int preferredSlot = getPreferredArmorSlot(stack);
        ModelBiped model = getArmorModel(player, stack, preferredSlot);
        if (model != null) {
            return new ArmorRenderData(model, preferredSlot);
        }

        for (int slot = 0; slot < 8; slot++) {
            if (slot == preferredSlot) {
                continue;
            }
            model = getArmorModel(player, stack, slot);
            if (model != null) {
                return new ArmorRenderData(model, slot);
            }
        }

        return null;
    }

    private static int getPreferredArmorSlot(ItemStack stack) {
        Integer travellersSlot = getTravellersSlot(stack);
        return travellersSlot == null ? 0 : 4 + travellersSlot.intValue();
    }

    private static Integer getTravellersSlot(ItemStack stack) {
        try {
            Method method = stack.getItem().getClass().getMethod("getSlot", ItemStack.class);
            Object value = method.invoke(stack.getItem(), stack);
            return value instanceof Integer ? (Integer) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ModelBiped getArmorModel(EntityPlayer player, ItemStack stack, int armorSlot) {
        try {
            return stack.getItem().getArmorModel(player, stack, armorSlot);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void bindArmorTexture(ItemStack stack, EntityPlayer player, int armorSlot) {
        try {
            Item item = stack.getItem();
            String texture = item.getArmorTexture(stack, (Entity) player, armorSlot, null);
            if (texture != null && texture.length() > 0) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(texture));
            }
        } catch (Throwable ignored) {
        }
    }

    private static float interpolateRotation(float previous, float current, float partialTicks) {
        float delta;
        for (delta = current - previous; delta < -180.0F; delta += 360.0F) {
        }
        while (delta >= 180.0F) {
            delta -= 360.0F;
        }
        return previous + partialTicks * delta;
    }

    private static final class ArmorRenderData {
        private final ModelBiped model;
        private final int armorSlot;

        private ArmorRenderData(ModelBiped model, int armorSlot) {
            this.model = model;
            this.armorSlot = armorSlot;
        }
    }
}
