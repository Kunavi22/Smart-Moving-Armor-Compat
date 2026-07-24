package com.ivanc.smartmovingarmorcompat;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.smart.render.ModelRotationRenderer;
import org.lwjgl.opengl.GL11;

public class SyncingModelRenderer extends ModelRenderer {
    private ModelBiped sourceModel;
    private final String[] partNames;
    private final ModelRenderer original;
    private final boolean copyAnglesFromSource;
    private final boolean preserveLocalTransform;

    public SyncingModelRenderer(ModelBase owner, ModelBiped sourceModel, String[] partNames, ModelRenderer original) {
        this(owner, sourceModel, partNames, original, true, false);
    }

    public SyncingModelRenderer(
        ModelBase owner,
        ModelBiped sourceModel,
        String[] partNames,
        ModelRenderer original,
        boolean copyAnglesFromSource,
        boolean preserveLocalTransform) {
        super(owner);
        this.sourceModel = sourceModel;
        this.partNames = partNames;
        this.original = original;
        this.copyAnglesFromSource = copyAnglesFromSource;
        this.preserveLocalTransform = preserveLocalTransform;
        ArmorModelSynchronizer.copyRendererTransform(original, this);
        ArmorModelSynchronizer.copyRendererVisibility(original, this);
    }

    public void setSource(ModelBiped sourceModel) {
        this.sourceModel = sourceModel;
    }

    public ModelRenderer getOriginal() {
        return this.original;
    }

    @Override
    public void render(float scale) {
        ModelRenderer source = syncOriginal();
        if (source instanceof ModelRotationRenderer) {
            renderWithSmartRenderTransform((ModelRotationRenderer) source, scale);
        } else {
            this.original.render(scale);
        }
    }

    @Override
    public void postRender(float scale) {
        syncOriginal();
        this.original.postRender(scale);
    }

    @Override
    public void addChild(ModelRenderer child) {
        this.original.addChild(child);
    }

    private ModelRenderer syncOriginal() {
        ModelRenderer source = ArmorModelSynchronizer.getPart(this.sourceModel, this.partNames);
        if (this.copyAnglesFromSource && source != null) {
            ArmorModelSynchronizer.syncAngles(source, this.original);
        } else {
            ArmorModelSynchronizer.copyRendererTransform(this, this.original);
        }
        ArmorModelSynchronizer.copyRendererVisibility(this, this.original);
        return source;
    }

    private void renderWithSmartRenderTransform(ModelRotationRenderer source, float scale) {
        RendererTransformSnapshot originalTransform = RendererTransformSnapshot.capture(this.original);
        boolean wrapperMatrixPushed = false;

        try {
            if (!this.preserveLocalTransform) {
                originalTransform.applyIdentity(this.original);
            }
            GL11.glPushMatrix();
            wrapperMatrixPushed = true;
            source.preTransforms(scale, true, true);
            this.original.render(scale);
            source.postTransforms(scale, true, true);
            GL11.glPopMatrix();
            wrapperMatrixPushed = false;
        } finally {
            if (wrapperMatrixPushed) {
                GL11.glPopMatrix();
            }
            originalTransform.restore(this.original);
        }
    }
}
