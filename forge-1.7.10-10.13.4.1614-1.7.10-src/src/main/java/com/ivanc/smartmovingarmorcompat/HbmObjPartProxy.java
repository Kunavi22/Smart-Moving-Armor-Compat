package com.ivanc.smartmovingarmorcompat;

import com.hbm.render.loader.ModelRendererObj;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.smart.render.ModelRotationRenderer;
import org.lwjgl.opengl.GL11;

public class HbmObjPartProxy extends ModelRendererObj {
    private final ModelRendererObj original;
    private final String[] partNames;
    private ModelBiped sourceModel;

    public HbmObjPartProxy(Object original, ModelBiped sourceModel, String[] partNames) {
        super(null, new String[0]);
        this.original = (ModelRendererObj) original;
        this.sourceModel = sourceModel;
        this.partNames = partNames;
        copyObjTransform(this.original, this);
    }

    public ModelRendererObj getOriginal() {
        return this.original;
    }

    public void setSourceModel(ModelBiped sourceModel) {
        this.sourceModel = sourceModel;
    }

    @Override
    public void copyRotationFrom(ModelRenderer model) {
        super.copyRotationFrom(model);
    }

    @Override
    public void copyTo(ModelRendererObj obj) {
        obj.offsetX = this.offsetX;
        obj.offsetY = this.offsetY;
        obj.offsetZ = this.offsetZ;
        obj.rotateAngleX = this.rotateAngleX;
        obj.rotateAngleY = this.rotateAngleY;
        obj.rotateAngleZ = this.rotateAngleZ;
        obj.rotationPointX = this.rotationPointX;
        obj.rotationPointY = this.rotationPointY;
        obj.rotationPointZ = this.rotationPointZ;
    }

    @Override
    public void render(float scale) {
        ModelRenderer source = ArmorModelSynchronizer.getPart(this.sourceModel, this.partNames);
        if (source instanceof ModelRotationRenderer) {
            renderWithSmartRenderTransform((ModelRotationRenderer) source, scale);
        } else {
            copyObjTransform(this, this.original);
            this.original.render(scale);
        }
    }

    private void renderWithSmartRenderTransform(ModelRotationRenderer source, float scale) {
        ObjTransformSnapshot originalTransform = ObjTransformSnapshot.capture(this.original);
        boolean wrapperMatrixPushed = false;

        try {
            originalTransform.applySmartRenderIdentity(this.original, this.doRender);
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

    private static void copyObjTransform(ModelRendererObj source, ModelRendererObj target) {
        target.rotationPointX = source.rotationPointX;
        target.rotationPointY = source.rotationPointY;
        target.rotationPointZ = source.rotationPointZ;
        target.originPointX = source.originPointX;
        target.originPointY = source.originPointY;
        target.originPointZ = source.originPointZ;
        target.rotateAngleX = source.rotateAngleX;
        target.rotateAngleY = source.rotateAngleY;
        target.rotateAngleZ = source.rotateAngleZ;
        target.offsetX = source.offsetX;
        target.offsetY = source.offsetY;
        target.offsetZ = source.offsetZ;
        target.doRender = source.doRender;
    }

    private static final class ObjTransformSnapshot {
        private final float rotationPointX;
        private final float rotationPointY;
        private final float rotationPointZ;
        private final float originPointX;
        private final float originPointY;
        private final float originPointZ;
        private final float rotateAngleX;
        private final float rotateAngleY;
        private final float rotateAngleZ;
        private final float offsetX;
        private final float offsetY;
        private final float offsetZ;
        private final boolean doRender;

        private ObjTransformSnapshot(ModelRendererObj renderer) {
            this.rotationPointX = renderer.rotationPointX;
            this.rotationPointY = renderer.rotationPointY;
            this.rotationPointZ = renderer.rotationPointZ;
            this.originPointX = renderer.originPointX;
            this.originPointY = renderer.originPointY;
            this.originPointZ = renderer.originPointZ;
            this.rotateAngleX = renderer.rotateAngleX;
            this.rotateAngleY = renderer.rotateAngleY;
            this.rotateAngleZ = renderer.rotateAngleZ;
            this.offsetX = renderer.offsetX;
            this.offsetY = renderer.offsetY;
            this.offsetZ = renderer.offsetZ;
            this.doRender = renderer.doRender;
        }

        private static ObjTransformSnapshot capture(ModelRendererObj renderer) {
            return new ObjTransformSnapshot(renderer);
        }

        private void applySmartRenderIdentity(ModelRendererObj renderer, boolean visible) {
            renderer.rotationPointX = 0.0F;
            renderer.rotationPointY = 0.0F;
            renderer.rotationPointZ = 0.0F;
            renderer.rotateAngleX = 0.0F;
            renderer.rotateAngleY = 0.0F;
            renderer.rotateAngleZ = 0.0F;
            renderer.offsetX = 0.0F;
            renderer.offsetY = 0.0F;
            renderer.offsetZ = 0.0F;
            renderer.doRender = visible;
        }

        private void restore(ModelRendererObj renderer) {
            renderer.rotationPointX = this.rotationPointX;
            renderer.rotationPointY = this.rotationPointY;
            renderer.rotationPointZ = this.rotationPointZ;
            renderer.originPointX = this.originPointX;
            renderer.originPointY = this.originPointY;
            renderer.originPointZ = this.originPointZ;
            renderer.rotateAngleX = this.rotateAngleX;
            renderer.rotateAngleY = this.rotateAngleY;
            renderer.rotateAngleZ = this.rotateAngleZ;
            renderer.offsetX = this.offsetX;
            renderer.offsetY = this.offsetY;
            renderer.offsetZ = this.offsetZ;
            renderer.doRender = this.doRender;
        }
    }
}
