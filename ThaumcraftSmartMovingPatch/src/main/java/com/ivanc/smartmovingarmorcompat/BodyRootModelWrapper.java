package com.ivanc.smartmovingarmorcompat;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;

final class BodyRootModelWrapper extends ModelBiped {
    private static final String FLUDD_MODEL = "maritech.model.ModelFLUDD";
    private static final String LIFEJACKET_MODEL = "mariculture.diving.render.ModelLifejacket";

    private final ModelBiped original;
    private ModelBiped sourceModel;

    BodyRootModelWrapper(ModelBiped original, ModelBiped sourceModel) {
        this.original = original;
        this.sourceModel = sourceModel;
    }

    static boolean supports(ModelBiped model) {
        if (model == null) {
            return false;
        }

        String className = model.getClass().getName();
        return FLUDD_MODEL.equals(className) || LIFEJACKET_MODEL.equals(className);
    }

    void setSourceModel(ModelBiped sourceModel) {
        this.sourceModel = sourceModel;
    }

    ModelBiped getOriginal() {
        return this.original;
    }

    @Override
    public void render(
        Entity entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale) {
        ModelBiped activeSource = SmartRenderTransformHelper.getActiveSmartModel(this.sourceModel);
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(activeSource, SmartRenderTransformHelper.BODY_PART);
        if (transform == null) {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }

        try {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        } finally {
            transform.end();
        }
    }
}
