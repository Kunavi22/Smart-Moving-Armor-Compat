package com.ivanc.smartmovingarmorcompat;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class AdventureHatModelWrapper extends ModelBiped {
    private static final String[] WING_PART = new String[] {"wing"};
    private static final String[] THING_PART = new String[] {"thing"};
    private static final String[] TOP_PART = new String[] {"top"};

    private final ModelBiped original;
    private final ModelRenderer wing;
    private final ModelRenderer thing;
    private final ModelRenderer top;
    private final RendererTransformSnapshot wingTransform;
    private final RendererTransformSnapshot thingTransform;
    private final RendererTransformSnapshot topTransform;
    private ModelBiped sourceModel;

    public AdventureHatModelWrapper(ModelBiped original, ModelBiped sourceModel) {
        this.original = original;
        this.sourceModel = sourceModel;
        this.wing = ArmorModelSynchronizer.getPart(original, WING_PART);
        this.thing = ArmorModelSynchronizer.getPart(original, THING_PART);
        this.top = ArmorModelSynchronizer.getPart(original, TOP_PART);
        this.wingTransform = this.wing == null ? null : RendererTransformSnapshot.capture(this.wing);
        this.thingTransform = this.thing == null ? null : RendererTransformSnapshot.capture(this.thing);
        this.topTransform = this.top == null ? null : RendererTransformSnapshot.capture(this.top);
    }

    public void setSourceModel(ModelBiped sourceModel) {
        this.sourceModel = sourceModel;
    }

    public ModelBiped getOriginal() {
        return this.original;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(this.sourceModel, SmartRenderTransformHelper.HEAD_PART);
        if (transform == null) {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }

        try {
            renderPart(this.wing, this.wingTransform, scale);
            renderPart(this.thing, this.thingTransform, scale);
            renderPart(this.top, this.topTransform, scale);
        } finally {
            transform.end();
        }
    }

    private static void renderPart(ModelRenderer part, RendererTransformSnapshot baseTransform, float scale) {
        if (part == null || baseTransform == null) {
            return;
        }

        RendererTransformSnapshot currentTransform = RendererTransformSnapshot.capture(part);
        try {
            baseTransform.restore(part);
            part.render(scale);
        } finally {
            currentTransform.restore(part);
        }
    }
}
