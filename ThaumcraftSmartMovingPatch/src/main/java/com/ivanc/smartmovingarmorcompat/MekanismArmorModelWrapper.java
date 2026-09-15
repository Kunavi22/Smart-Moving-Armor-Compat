package com.ivanc.smartmovingarmorcompat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class MekanismArmorModelWrapper extends ModelBiped {
    private static final String MODEL_CLASS = "mekanism.client.render.ModelCustomArmor";
    private static final String[] RIGHT_LEG_PART = new String[] {"bipedRightLeg", "field_78123_h"};
    private static final String[] LEFT_LEG_PART = new String[] {"bipedLeftLeg", "field_78124_i"};
    private static final Map<String, Field> FIELD_CACHE = new HashMap<String, Field>();
    private static final Map<String, Method> METHOD_CACHE = new HashMap<String, Method>();

    private final ModelBiped original;
    private ModelBiped sourceModel;

    public MekanismArmorModelWrapper(ModelBiped original, ModelBiped sourceModel) {
        this.original = original;
        this.sourceModel = sourceModel;
    }

    public static boolean isMekanismArmorModel(ModelBiped model) {
        if (model == null) {
            return false;
        }

        String className = model.getClass().getName();
        return MODEL_CLASS.equals(className) ||
            className.startsWith("mekanism.") && findField(model.getClass(), "modelType") != null;
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
        ModelBiped activeModel = SmartRenderTransformHelper.getActiveSmartModel(this.sourceModel);
        Object modelType = getFieldValue(this.original, "modelType");
        if (activeModel == null || modelType == null) {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }

        String typeName = modelType instanceof Enum ? ((Enum) modelType).name() : modelType.toString();
        if (!canTransform(activeModel, typeName)) {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }

        ResourceLocation texture = getResource(modelType, "resource");
        if (texture != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        }

        if ("GASMASK".equals(typeName)) {
            renderPart(activeModel, modelType, "gasMaskModel", "render", SmartRenderTransformHelper.HEAD_PART, scale, 0);
        } else if ("FREERUNNERS".equals(typeName)) {
            renderPart(activeModel, modelType, "freeRunnersModel", "renderRight", RIGHT_LEG_PART, scale, 1);
            renderPart(activeModel, modelType, "freeRunnersModel", "renderLeft", LEFT_LEG_PART, scale, 2);
        } else if ("JETPACK".equals(typeName)) {
            renderPart(activeModel, modelType, "jetpackModel", "render", SmartRenderTransformHelper.BODY_PART, scale, 0);
        } else if ("ARMOREDJETPACK".equals(typeName)) {
            renderPart(activeModel, modelType, "armoredJetpackModel", "render", SmartRenderTransformHelper.BODY_PART, scale, 0);
        } else if ("SCUBATANK".equals(typeName)) {
            renderPart(activeModel, modelType, "scubaTankModel", "render", SmartRenderTransformHelper.BODY_PART, scale, 0);
        } else {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
    }

    private static boolean canTransform(ModelBiped model, String typeName) {
        if ("GASMASK".equals(typeName)) {
            return SmartRenderTransformHelper.canTransform(model, SmartRenderTransformHelper.HEAD_PART);
        }
        if ("FREERUNNERS".equals(typeName)) {
            return SmartRenderTransformHelper.canTransform(model, RIGHT_LEG_PART) &&
                SmartRenderTransformHelper.canTransform(model, LEFT_LEG_PART);
        }
        return SmartRenderTransformHelper.canTransform(model, SmartRenderTransformHelper.BODY_PART);
    }

    private void renderPart(
        ModelBiped activeModel,
        Object modelType,
        String modelField,
        String renderMethod,
        String[] sourcePart,
        float scale,
        int localTransform) {
        Object customModel = getFieldValue(modelType, modelField);
        SmartRenderTransformHelper.Transform transform = SmartRenderTransformHelper.begin(activeModel, sourcePart);
        if (customModel == null || transform == null) {
            return;
        }

        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0.0F, 0.0F, 0.06F);
            if (localTransform == 0 && "gasMaskModel".equals(modelField)) {
                GL11.glTranslatef(0.0F, 0.0F, -0.05F);
            } else if (localTransform == 1) {
                GL11.glScalef(1.02F, 1.02F, 1.02F);
                GL11.glTranslatef(0.1375F, -0.75F, -0.0625F);
            } else if (localTransform == 2) {
                GL11.glScalef(1.02F, 1.02F, 1.02F);
                GL11.glTranslatef(-0.1375F, -0.75F, -0.0625F);
            }
            invokeFloat(customModel, renderMethod, scale);
        } finally {
            GL11.glPopMatrix();
            transform.end();
        }
    }

    private static ResourceLocation getResource(Object owner, String fieldName) {
        Object value = getFieldValue(owner, fieldName);
        return value instanceof ResourceLocation ? (ResourceLocation) value : null;
    }

    private static Object getFieldValue(Object owner, String fieldName) {
        if (owner == null) {
            return null;
        }

        Field field = findField(owner.getClass(), fieldName);
        if (field == null) {
            return null;
        }

        try {
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void invokeFloat(Object owner, String methodName, float value) {
        Method method = findMethod(owner.getClass(), methodName, Float.TYPE);
        if (method == null) {
            return;
        }

        try {
            method.invoke(owner, Float.valueOf(value));
        } catch (Throwable ignored) {
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
                break;
            }
        }

        FIELD_CACHE.put(key, null);
        return null;
    }

    private static Method findMethod(Class<?> type, String methodName, Class<?> parameterType) {
        String key = type.getName() + "#" + methodName;
        if (METHOD_CACHE.containsKey(key)) {
            return METHOD_CACHE.get(key);
        }

        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterType);
                method.setAccessible(true);
                METHOD_CACHE.put(key, method);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                break;
            }
        }

        METHOD_CACHE.put(key, null);
        return null;
    }
}
