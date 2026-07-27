package com.ivanc.smartmovingarmorcompat;

import api.player.model.ModelPlayerAPI;
import api.player.model.ModelPlayerBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.smart.render.ModelRotationRenderer;

public class SmartMovingArmorModelPlayerBase extends ModelPlayerBase {
    private static final float ELYTRA_HEAD_PITCH = -0.7853982F;
    private static final float ELYTRA_HEAD_TICKS = 4.0F;

    public SmartMovingArmorModelPlayerBase(ModelPlayerAPI modelPlayerAPI) {
        super(modelPlayerAPI);
    }

    @Override
    public void afterSetRotationAngles(
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scaleFactor,
        Entity entity) {
        if (!(entity instanceof EntityPlayer)) {
            return;
        }

        if (EtFuturumElytraCompat.getTicksElytraFlying((EntityPlayer) entity) <= ELYTRA_HEAD_TICKS) {
            return;
        }

        fixHeadPitch(this.modelPlayer.bipedHead, this.modelPlayer.bipedHeadwear, netHeadYaw);
    }

    private static void fixHeadPitch(ModelRenderer head, ModelRenderer headwear, float netHeadYaw) {
        if (head != null) {
            head.rotateAngleX = ELYTRA_HEAD_PITCH;
            head.rotateAngleY = netHeadYaw * ((float) Math.PI / 180.0F);
        }

        if (headwear == null) {
            return;
        }

        if (headwear instanceof ModelRotationRenderer) {
            headwear.rotateAngleX = 0.0F;
            headwear.rotateAngleY = 0.0F;
        } else {
            headwear.rotateAngleX = ELYTRA_HEAD_PITCH;
            headwear.rotateAngleY = netHeadYaw * ((float) Math.PI / 180.0F);
        }
    }
}
