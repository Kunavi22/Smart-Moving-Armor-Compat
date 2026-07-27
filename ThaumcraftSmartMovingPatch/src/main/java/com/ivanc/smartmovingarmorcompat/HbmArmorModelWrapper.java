package com.ivanc.smartmovingarmorcompat;

import api.player.render.IRenderPlayer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;

public class HbmArmorModelWrapper extends ModelBiped {
    private static final String HBM_ARMOR_BASE = "com.hbm.render.model.ModelArmorBase";
    private static final String HBM_OBJ_PROXY = "com.ivanc.smartmovingarmorcompat.HbmObjPartProxy";
    private static final String HBM_MANUAL_BODY_WINGS = "com.hbm.render.model.ModelArmorWings";
    private static final PartMap[] PARTS = new PartMap[] {
        new PartMap(SmartRenderTransformHelper.HEAD_PART, "head", PartMap.HEAD),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "body", PartMap.BODY),
        new PartMap(new String[] {"bipedLeftArm", "field_78113_g"}, "leftArm", PartMap.LEFT_ARM),
        new PartMap(new String[] {"bipedRightArm", "field_78112_f"}, "rightArm", PartMap.RIGHT_ARM),
        new PartMap(new String[] {"bipedLeftLeg", "field_78124_i"}, "leftLeg", PartMap.LEFT_LEG),
        new PartMap(new String[] {"bipedRightLeg", "field_78123_h"}, "rightLeg", PartMap.RIGHT_LEG),
        new PartMap(new String[] {"bipedLeftLeg", "field_78124_i"}, "leftFoot", PartMap.LEFT_LEG),
        new PartMap(new String[] {"bipedRightLeg", "field_78123_h"}, "rightFoot", PartMap.RIGHT_LEG)
    };
    private static final PartMap[] EXTRA_OBJ_PARTS = new PartMap[] {
        new PartMap(SmartRenderTransformHelper.BODY_PART, "jetpack", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "cassette", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "glow", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "fan", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "tail", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "axe", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "wingLB", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "wingLT", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "wingRB", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.BODY_PART, "wingRT", PartMap.BODY),
        new PartMap(SmartRenderTransformHelper.HEAD_PART, "lamps", PartMap.HEAD),
        new PartMap(SmartRenderTransformHelper.HEAD_PART, "light", PartMap.HEAD),
        new PartMap(SmartRenderTransformHelper.HEAD_PART, "eyes", PartMap.HEAD)
    };

    private static final Map<String, Field> FIELD_CACHE = new HashMap<String, Field>();
    private static Method proxyOriginalMethod;

    private final ModelBiped original;
    private final ModelBiped poseModel = new ModelBiped(0.0F);
    private ModelBiped sourceModel;

    public HbmArmorModelWrapper(ModelBiped original, ModelBiped sourceModel) {
        this.original = original;
        this.sourceModel = sourceModel;
    }

    public static boolean isHbmArmorModel(ModelBiped model) {
        Class<?> current = model.getClass();
        while (current != null) {
            if (HBM_ARMOR_BASE.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    public static boolean isManualBodyModel(ModelBiped model) {
        return model != null && HBM_MANUAL_BODY_WINGS.equals(model.getClass().getName());
    }

    public void setSourceModel(ModelBiped sourceModel) {
        this.sourceModel = sourceModel;
    }

    public ModelBiped getOriginal() {
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
        if (this.sourceModel == null) {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }

        Render render = RenderManager.instance.getEntityRenderObject(entity);
        if (!(render instanceof IRenderPlayer)) {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }

        IRenderPlayer renderPlayer = (IRenderPlayer) render;
        ModelBase oldMainModel = renderPlayer.getMainModelField();
        ModelBiped oldBipedMain = renderPlayer.getModelBipedMainField();

        updatePoseModel();
        if (isManualBodyModel(this.original)) {
            renderManualBodyModel(
                renderPlayer,
                oldMainModel,
                oldBipedMain,
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks,
                netHeadYaw,
                headPitch,
                scale);
            return;
        }

        List installedParts = installObjPartProxies();
        try {
            renderPlayer.setMainModelField(this.poseModel);
            renderPlayer.setModelBipedMainField(this.poseModel);
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        } finally {
            renderPlayer.setMainModelField(oldMainModel);
            renderPlayer.setModelBipedMainField(oldBipedMain);
            restoreObjPartProxies(installedParts);
        }
    }

    private void renderManualBodyModel(
        IRenderPlayer renderPlayer,
        ModelBase oldMainModel,
        ModelBiped oldBipedMain,
        Entity entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale) {
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(this.sourceModel, SmartRenderTransformHelper.BODY_PART);

        zeroPosePart(this.poseModel.bipedBody);
        try {
            renderPlayer.setMainModelField(this.poseModel);
            renderPlayer.setModelBipedMainField(this.poseModel);
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        } finally {
            renderPlayer.setMainModelField(oldMainModel);
            renderPlayer.setModelBipedMainField(oldBipedMain);
            if (transform != null) {
                transform.end();
            }
        }
    }

    public static void restoreHbmPartProxies(ModelBiped model) {
        if (model == null || !isHbmArmorModel(model)) {
            return;
        }

        restoreObjPartProxies(model, PARTS);
        restoreObjPartProxies(model, EXTRA_OBJ_PARTS);
    }

    public static List prepareHbmPartProxies(ModelBiped model, ModelBiped sourceModel) {
        List installed = new ArrayList();
        if (model == null || sourceModel == null || !isHbmArmorModel(model)) {
            return installed;
        }

        installObjPartProxies(model, sourceModel, PARTS, installed);
        installObjPartProxies(model, sourceModel, EXTRA_OBJ_PARTS, installed);
        return installed;
    }

    public static void restorePreparedHbmPartProxies(List installed) {
        restoreObjPartProxies(installed);
    }

    public static SmartRenderContext beginSmartRenderContext(Entity entity, ModelBiped sourceModel) {
        if (entity == null || sourceModel == null) {
            return null;
        }

        Render render = RenderManager.instance.getEntityRenderObject(entity);
        if (!(render instanceof IRenderPlayer)) {
            return null;
        }

        ModelBiped poseModel = new ModelBiped(0.0F);
        updatePoseModel(sourceModel, poseModel, null);
        return new SmartRenderContext((IRenderPlayer) render, poseModel);
    }

    private void updatePoseModel() {
        updatePoseModel(this.sourceModel, this.poseModel, this.original);
    }

    private static void updatePoseModel(ModelBiped sourceModel, ModelBiped poseModel, ModelBiped hbmModel) {
        copyState(sourceModel, poseModel);

        for (int i = 0; i < PARTS.length; i++) {
            PartMap part = PARTS[i];
            ModelRenderer sourcePart = ArmorModelSynchronizer.getPart(sourceModel, part.sourceNames);
            ModelRenderer posePart = getPosePart(poseModel, part.posePart);
            Object hbmPart = hbmModel == null ? null : getFieldValue(hbmModel, part.hbmFieldName);
            if (sourcePart != null && posePart != null) {
                copySmartAnglesWithHbmPivot(sourcePart, posePart, hbmPart, part.fallbackX, part.fallbackY, part.fallbackZ);
            }
        }

        copyPosePart(poseModel.bipedHead, poseModel.bipedHeadwear);
    }

    private static void zeroPosePart(ModelRenderer renderer) {
        renderer.rotationPointX = 0.0F;
        renderer.rotationPointY = 0.0F;
        renderer.rotationPointZ = 0.0F;
        renderer.rotateAngleX = 0.0F;
        renderer.rotateAngleY = 0.0F;
        renderer.rotateAngleZ = 0.0F;
        renderer.offsetX = 0.0F;
        renderer.offsetY = 0.0F;
        renderer.offsetZ = 0.0F;
    }

    private List installObjPartProxies() {
        return prepareHbmPartProxies(this.original, this.sourceModel);
    }

    private static void installObjPartProxies(ModelBiped model, ModelBiped sourceModel, PartMap[] parts, List installed) {
        for (int i = 0; i < parts.length; i++) {
            Field field = findField(model.getClass(), parts[i].hbmFieldName);
            if (field == null) {
                continue;
            }

            try {
                Object current = field.get(model);
                Object unwrapped = unwrapProxy(current);
                if (unwrapped != null) {
                    current = unwrapped;
                }

                Object proxy = newObjPartProxy(current, sourceModel, parts[i].sourceNames);
                if (proxy != null) {
                    field.set(model, proxy);
                    installed.add(new ObjPartInstall(model, field, current));
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void restoreObjPartProxies(List installed) {
        for (int i = installed.size() - 1; i >= 0; i--) {
            ObjPartInstall install = (ObjPartInstall) installed.get(i);
            try {
                install.field.set(install.owner, install.originalPart);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void restoreObjPartProxies(ModelBiped model, PartMap[] parts) {
        for (int i = 0; i < parts.length; i++) {
            Field field = findField(model.getClass(), parts[i].hbmFieldName);
            if (field == null) {
                continue;
            }

            try {
                Object current = field.get(model);
                Object originalPart = unwrapProxy(current);
                if (originalPart != null) {
                    field.set(model, originalPart);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static Object newObjPartProxy(Object originalPart, ModelBiped sourceModel, String[] sourceNames) {
        if (originalPart == null) {
            return null;
        }

        try {
            Class proxyClass = Class.forName(HBM_OBJ_PROXY);
            Object proxy =
                proxyClass
                    .getConstructor(new Class[] {Object.class, ModelBiped.class, String[].class})
                    .newInstance(new Object[] {originalPart, sourceModel, sourceNames});
            Method setSource = proxyClass.getMethod("setSourceModel", new Class[] {ModelBiped.class});
            setSource.invoke(proxy, new Object[] {sourceModel});
            return proxy;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object unwrapProxy(Object value) {
        if (value == null || !HBM_OBJ_PROXY.equals(value.getClass().getName())) {
            return null;
        }

        try {
            if (proxyOriginalMethod == null) {
                proxyOriginalMethod = value.getClass().getMethod("getOriginal", new Class[0]);
            }
            return proxyOriginalMethod.invoke(value, new Object[0]);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void copyState(ModelBiped source, ModelBiped target) {
        target.heldItemLeft = source.heldItemLeft;
        target.heldItemRight = source.heldItemRight;
        target.isSneak = source.isSneak;
        target.aimedBow = source.aimedBow;
        target.isRiding = source.isRiding;
        target.onGround = source.onGround;
        target.isChild = source.isChild;
    }

    private static void copySmartAnglesWithHbmPivot(
        ModelRenderer source,
        ModelRenderer target,
        Object hbmPart,
        float fallbackX,
        float fallbackY,
        float fallbackZ) {
        target.rotateAngleX = source.rotateAngleX;
        target.rotateAngleY = source.rotateAngleY;
        target.rotateAngleZ = source.rotateAngleZ;
        target.rotationPointX = readFloat(hbmPart, "originPointX", fallbackX);
        target.rotationPointY = readFloat(hbmPart, "originPointY", fallbackY);
        target.rotationPointZ = readFloat(hbmPart, "originPointZ", fallbackZ);
        target.offsetX = 0.0F;
        target.offsetY = 0.0F;
        target.offsetZ = 0.0F;
        target.showModel = source.showModel;
        target.isHidden = source.isHidden;
    }

    private static void copyPosePart(ModelRenderer source, ModelRenderer target) {
        target.rotateAngleX = source.rotateAngleX;
        target.rotateAngleY = source.rotateAngleY;
        target.rotateAngleZ = source.rotateAngleZ;
        target.rotationPointX = source.rotationPointX;
        target.rotationPointY = source.rotationPointY;
        target.rotationPointZ = source.rotationPointZ;
        target.offsetX = source.offsetX;
        target.offsetY = source.offsetY;
        target.offsetZ = source.offsetZ;
        target.showModel = source.showModel;
        target.isHidden = source.isHidden;
    }

    private static ModelRenderer getPosePart(ModelBiped poseModel, int part) {
        switch (part) {
            case PartMap.HEAD:
                return poseModel.bipedHead;
            case PartMap.BODY:
                return poseModel.bipedBody;
            case PartMap.LEFT_ARM:
                return poseModel.bipedLeftArm;
            case PartMap.RIGHT_ARM:
                return poseModel.bipedRightArm;
            case PartMap.LEFT_LEG:
                return poseModel.bipedLeftLeg;
            case PartMap.RIGHT_LEG:
                return poseModel.bipedRightLeg;
            default:
                return null;
        }
    }

    private static Object getFieldValue(Object target, String fieldName) {
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            return null;
        }

        try {
            return field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float readFloat(Object target, String fieldName, float fallback) {
        if (target == null) {
            return fallback;
        }

        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            return fallback;
        }

        try {
            return field.getFloat(target);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        String key = type.getName() + "#" + fieldName;
        if (FIELD_CACHE.containsKey(key)) {
            return FIELD_CACHE.get(key);
        }

        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                FIELD_CACHE.put(key, field);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                FIELD_CACHE.put(key, null);
                return null;
            }
        }

        FIELD_CACHE.put(key, null);
        return null;
    }

    private static final class PartMap {
        private static final int HEAD = 0;
        private static final int BODY = 1;
        private static final int LEFT_ARM = 2;
        private static final int RIGHT_ARM = 3;
        private static final int LEFT_LEG = 4;
        private static final int RIGHT_LEG = 5;

        private final String[] sourceNames;
        private final String hbmFieldName;
        private final int posePart;
        private final float fallbackX;
        private final float fallbackY;
        private final float fallbackZ;

        private PartMap(String[] sourceNames, String hbmFieldName, int posePart) {
            this.sourceNames = sourceNames;
            this.hbmFieldName = hbmFieldName;
            this.posePart = posePart;
            this.fallbackX = getFallbackX(posePart);
            this.fallbackY = getFallbackY(posePart);
            this.fallbackZ = 0.0F;
        }

        private static float getFallbackX(int posePart) {
            if (posePart == LEFT_ARM) {
                return 5.0F;
            }
            if (posePart == RIGHT_ARM) {
                return -5.0F;
            }
            if (posePart == LEFT_LEG) {
                return 1.9F;
            }
            if (posePart == RIGHT_LEG) {
                return -1.9F;
            }
            return 0.0F;
        }

        private static float getFallbackY(int posePart) {
            if (posePart == LEFT_ARM || posePart == RIGHT_ARM) {
                return 2.0F;
            }
            if (posePart == LEFT_LEG || posePart == RIGHT_LEG) {
                return 12.0F;
            }
            return 0.0F;
        }
    }

    public static final class SmartRenderContext {
        private final IRenderPlayer renderPlayer;
        private final ModelBase oldMainModel;
        private final ModelBiped oldBipedMain;
        private boolean active = true;

        private SmartRenderContext(IRenderPlayer renderPlayer, ModelBiped poseModel) {
            this.renderPlayer = renderPlayer;
            this.oldMainModel = renderPlayer.getMainModelField();
            this.oldBipedMain = renderPlayer.getModelBipedMainField();
            renderPlayer.setMainModelField(poseModel);
            renderPlayer.setModelBipedMainField(poseModel);
        }

        public void end() {
            if (!this.active) {
                return;
            }

            this.renderPlayer.setMainModelField(this.oldMainModel);
            this.renderPlayer.setModelBipedMainField(this.oldBipedMain);
            this.active = false;
        }
    }

    private static final class ObjPartInstall {
        private final Object owner;
        private final Field field;
        private final Object originalPart;

        private ObjPartInstall(Object owner, Field field, Object originalPart) {
            this.owner = owner;
            this.field = field;
            this.originalPart = originalPart;
        }
    }
}
