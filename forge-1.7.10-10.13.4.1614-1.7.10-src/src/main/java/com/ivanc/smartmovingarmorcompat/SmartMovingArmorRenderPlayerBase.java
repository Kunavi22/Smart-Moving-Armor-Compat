package com.ivanc.smartmovingarmorcompat;

import api.player.render.RenderPlayerAPI;
import api.player.render.RenderPlayerBase;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.smart.render.IModelPlayer;
import net.smart.render.IRenderPlayer;
import net.smart.render.SmartRenderModel;
import org.lwjgl.opengl.GL11;

public class SmartMovingArmorRenderPlayerBase extends RenderPlayerBase {
    private static final String ADVENTURE_HAT_MODEL = "com.darkona.adventurebackpack.client.models.ModelAdventureHat";
    private static final String HOVER_HARNESS_MODEL = "thaumcraft.client.renderers.models.gear.ModelHoverHarness";
    private static final String SMART_MOVING_BASE = "Smart Moving";
    private static final String SMART_RENDER_BASE = "Smart Render";

    public SmartMovingArmorRenderPlayerBase(RenderPlayerAPI renderPlayerAPI) {
        super(renderPlayerAPI);
    }

    @Override
    public void renderModel(
        EntityLivingBase entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale) {
        if (entity instanceof EntityPlayer && EtFuturumElytraCompat.isElytraFlying((EntityPlayer) entity)) {
            suppressSmartMovingFallingAnimation((EntityPlayer) entity);
            super.renderModel(entity, limbSwing, 0.0F, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }

        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
    }

    @Override
    public void rotatePlayer(AbstractClientPlayer player, float ageInTicks, float rotationYaw, float partialTicks) {
        if (!EtFuturumElytraCompat.isElytraFlying(player)) {
            super.rotatePlayer(player, ageInTicks, rotationYaw, partialTicks);
            return;
        }

        GL11.glPushMatrix();
        super.rotatePlayer(player, ageInTicks, rotationYaw, partialTicks);
        GL11.glPopMatrix();
        clearSmartRenderBodyYaw();

        float flyingTicks = EtFuturumElytraCompat.getTicksElytraFlying(player) + partialTicks;
        float blend = MathHelper.clamp_float(flyingTicks * flyingTicks / 100.0F, 0.0F, 1.0F);
        float glidePitch = blend * (-90.0F - player.rotationPitch);

        GL11.glRotatef(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(glidePitch, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(getElytraBankAngle(player, partialTicks), 0.0F, 1.0F, 0.0F);
    }

    private void suppressSmartMovingFallingAnimation(EntityPlayer player) {
        if (!EtFuturumElytraCompat.isElytraFlying(player)) {
            return;
        }

        clearSmartMovingFalling(net.smart.moving.render.SmartMovingRender.CurrentMainModel);

        try {
            RenderPlayerBase base = this.renderPlayerAPI.getRenderPlayerBase(SMART_MOVING_BASE);
            if (!(base instanceof net.smart.moving.render.playerapi.SmartMovingRenderPlayerBase)) {
                return;
            }

            net.smart.moving.render.IModelPlayer[] models =
                ((net.smart.moving.render.playerapi.SmartMovingRenderPlayerBase) base).getPlayerModels();
            for (int i = 0; i < models.length; i++) {
                clearSmartMovingFalling(models[i] == null ? null : models[i].getMovingModel());
            }
        } catch (Throwable ignored) {
        }
    }

    private static void clearSmartMovingFalling(net.smart.moving.render.SmartMovingModel model) {
        if (model != null) {
            model.isFalling = false;
        }
    }

    private static float getElytraBankAngle(AbstractClientPlayer player, float partialTicks) {
        Vec3 look = player.getLook(partialTicks);
        double motionLengthSq = player.motionX * player.motionX + player.motionZ * player.motionZ;
        double lookLengthSq = look.xCoord * look.xCoord + look.zCoord * look.zCoord;

        if (motionLengthSq <= 0.0D || lookLengthSq <= 0.0D) {
            return 0.0F;
        }

        double dot = (player.motionX * look.xCoord + player.motionZ * look.zCoord) /
            (Math.sqrt(motionLengthSq) * Math.sqrt(lookLengthSq));
        double cross = player.motionX * look.zCoord - player.motionZ * look.xCoord;
        double clampedDot = Math.min(Math.max(dot, -1.0D), 1.0D);
        return (float) (Math.signum(cross) * Math.acos(clampedDot) * 180.0D / Math.PI);
    }

    private void clearSmartRenderBodyYaw() {
        try {
            RenderPlayerBase base = this.renderPlayerAPI.getRenderPlayerBase(SMART_RENDER_BASE);
            if (!(base instanceof IRenderPlayer)) {
                return;
            }

            IModelPlayer[] models = ((IRenderPlayer) base).getRenderModels();
            for (int i = 0; i < models.length; i++) {
                SmartRenderModel model = models[i] == null ? null : models[i].getRenderModel();
                if (model == null) {
                    continue;
                }

                model.actualRotation = 0.0F;
                if (model.prevOuterRenderData != null) {
                    model.prevOuterRenderData.rotateAngleY = 0.0F;
                }
                if (model.bipedOuter != null) {
                    model.bipedOuter.rotateAngleY = 0.0F;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void afterSetArmorModel(AbstractClientPlayer player, int slot, float partialTicks) {
        suppressSmartMovingFallingAnimation(player);

        ModelBase renderPassModel = this.renderPlayerAPI.getRenderPassModelField();
        if (!SmartRenderTransformHelper.isRealClientPlayer(player)) {
            restoreRenderPassModel(renderPassModel);
            return;
        }

        if (!(renderPassModel instanceof ModelBiped)) {
            return;
        }

        ModelBiped target = (ModelBiped) renderPassModel;
        ModelBiped source = this.renderPlayerAPI.getModelBipedMainField();
        if (source == null || source == target) {
            return;
        }

        if (target instanceof HbmArmorModelWrapper) {
            ((HbmArmorModelWrapper) target).setSourceModel(source);
            return;
        }

        if (isAdventureHatModel(target)) {
            this.renderPlayerAPI.setRenderPassModelField(wrapAdventureHat(target, source));
            return;
        }

        if (isHoverHarnessModel(target)) {
            this.renderPlayerAPI.setRenderPassModelField(wrapHoverHarness(target, source));
            return;
        }

        if (HbmArmorModelWrapper.isHbmArmorModel(target)) {
            this.renderPlayerAPI.setRenderPassModelField(new HbmArmorModelWrapper(target, source));
            return;
        }

        ArmorModelSynchronizer.prepare(target, source);
    }

    private void restoreRenderPassModel(ModelBase renderPassModel) {
        if (renderPassModel instanceof AdventureHatModelWrapper) {
            this.renderPlayerAPI.setRenderPassModelField(((AdventureHatModelWrapper) renderPassModel).getOriginal());
        } else if (renderPassModel instanceof HoverHarnessModelWrapper) {
            ModelBiped original = ((HoverHarnessModelWrapper) renderPassModel).getOriginal();
            ArmorModelSynchronizer.restore(original);
            this.renderPlayerAPI.setRenderPassModelField(original);
        } else if (renderPassModel instanceof HbmArmorModelWrapper) {
            ModelBiped original = ((HbmArmorModelWrapper) renderPassModel).getOriginal();
            HbmArmorModelWrapper.restoreHbmPartProxies(original);
            this.renderPlayerAPI.setRenderPassModelField(original);
        } else if (renderPassModel instanceof ModelBiped) {
            HbmArmorModelWrapper.restoreHbmPartProxies((ModelBiped) renderPassModel);
            ArmorModelSynchronizer.restore((ModelBiped) renderPassModel);
        }
    }

    private static boolean isAdventureHatModel(ModelBiped model) {
        return ADVENTURE_HAT_MODEL.equals(model.getClass().getName());
    }

    private static boolean isHoverHarnessModel(ModelBiped model) {
        return HOVER_HARNESS_MODEL.equals(model.getClass().getName());
    }

    private static ModelBiped wrapAdventureHat(ModelBiped target, ModelBiped source) {
        if (target instanceof AdventureHatModelWrapper) {
            ((AdventureHatModelWrapper) target).setSourceModel(source);
            return target;
        }
        return new AdventureHatModelWrapper(target, source);
    }

    private static ModelBiped wrapHoverHarness(ModelBiped target, ModelBiped source) {
        if (target instanceof HoverHarnessModelWrapper) {
            ((HoverHarnessModelWrapper) target).setSourceModel(source);
            return target;
        }
        return new HoverHarnessModelWrapper(target, source);
    }
}
