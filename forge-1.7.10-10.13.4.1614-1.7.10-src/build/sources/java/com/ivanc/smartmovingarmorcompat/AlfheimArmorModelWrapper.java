package com.ivanc.smartmovingarmorcompat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.smart.render.ModelRotationRenderer;
import org.lwjgl.opengl.GL11;

public class AlfheimArmorModelWrapper extends ModelBiped {
    private static final String FENRIR_MODEL = "alfheim.client.model.armor.ModelFenrirArmor";
    private static final String ELVORIUM_MODEL = "alfheim.client.model.armor.ModelElvoriumArmor";
    private static final String ADVANCED_ARMOR_MODEL = "alexsocol.asjlib.render.AdvancedArmorModel";
    private static final String ASJ_RENDER_HELPER = "alexsocol.asjlib.render.ASJRenderHelper";

    private static final String[] RIGHT_ARM_PART = new String[] {"bipedRightArm", "field_78112_f"};
    private static final String[] LEFT_ARM_PART = new String[] {"bipedLeftArm", "field_78113_g"};
    private static final String[] RIGHT_LEG_PART = new String[] {"bipedRightLeg", "field_78123_h"};
    private static final String[] LEFT_LEG_PART = new String[] {"bipedLeftLeg", "field_78124_i"};

    private static final Part[] ADVANCED_PARTS = new Part[] {
        new Part(SmartRenderTransformHelper.HEAD_PART, "partHead"),
        new Part(SmartRenderTransformHelper.BODY_PART, "partBody"),
        new Part(RIGHT_ARM_PART, "partRightArm"),
        new Part(LEFT_ARM_PART, "partLeftArm"),
        new Part(RIGHT_LEG_PART, "partRightLeg"),
        new Part(LEFT_LEG_PART, "partLeftLeg")
    };

    private static final Map<String, Field> FIELD_CACHE = new HashMap<String, Field>();
    private static final Map<String, Method> METHOD_CACHE = new HashMap<String, Method>();

    private final ModelBiped original;
    private ModelBiped sourceModel;

    public AlfheimArmorModelWrapper(ModelBiped original, ModelBiped sourceModel) {
        this.original = original;
        this.sourceModel = sourceModel;
    }

    public static boolean isAlfheimArmorModel(ModelBiped model) {
        if (model == null) {
            return false;
        }

        String className = model.getClass().getName();
        if (FENRIR_MODEL.equals(className)) {
            return true;
        }
        return ELVORIUM_MODEL.equals(className) && hasSuperclass(model.getClass(), ADVANCED_ARMOR_MODEL);
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
        if (!hasSmartParts(activeModel)) {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }

        if (FENRIR_MODEL.equals(this.original.getClass().getName())) {
            renderFenrir(activeModel, scale);
        } else {
            renderAdvanced(activeModel, entity);
        }
    }

    private void renderFenrir(ModelBiped activeModel, float scale) {
        Integer slot = getIntField(this.original, "slot");
        if (slot == null) {
            return;
        }

        try {
            switch (slot.intValue()) {
                case 0:
                    renderModelRenderer(activeModel, SmartRenderTransformHelper.HEAD_PART, "helmAnchor", scale);
                    break;
                case 1:
                    renderModelRenderer(activeModel, SmartRenderTransformHelper.BODY_PART, "bodyAnchor", scale);
                    renderModelRenderer(activeModel, RIGHT_ARM_PART, "armRAnchor", scale);
                    renderModelRenderer(activeModel, LEFT_ARM_PART, "armLAnchor", scale);
                    break;
                case 2:
                    renderModelRenderer(activeModel, SmartRenderTransformHelper.BODY_PART, "pantsAnchor", scale);
                    renderModelRenderer(activeModel, RIGHT_LEG_PART, "legR", scale);
                    renderModelRenderer(activeModel, LEFT_LEG_PART, "legL", scale);
                    break;
                case 3:
                    renderModelRenderer(activeModel, RIGHT_LEG_PART, "bootR", scale);
                    renderModelRenderer(activeModel, LEFT_LEG_PART, "bootL", scale);
                    break;
                default:
                    break;
            }
        } finally {
            invokeStaticNoArgs(ASJ_RENDER_HELPER, "discard");
        }
    }

    private void renderModelRenderer(ModelBiped activeModel, String[] sourcePart, String fieldName, float scale) {
        Object value = getFieldValue(this.original, fieldName);
        if (!(value instanceof ModelRenderer)) {
            return;
        }

        ModelRenderer renderer = (ModelRenderer) value;
        SmartRenderTransformHelper.Transform transform = SmartRenderTransformHelper.begin(activeModel, sourcePart);
        if (transform == null) {
            return;
        }

        RendererTransformSnapshot snapshot = RendererTransformSnapshot.capture(renderer);
        boolean showModel = renderer.showModel;
        boolean hidden = renderer.isHidden;
        try {
            snapshot.applyIdentity(renderer);
            renderer.showModel = true;
            renderer.isHidden = false;
            renderer.render(scale);
        } finally {
            renderer.showModel = showModel;
            renderer.isHidden = hidden;
            snapshot.restore(renderer);
            transform.end();
        }
    }

    private void renderAdvanced(ModelBiped activeModel, Entity entity) {
        GL11.glPushMatrix();
        try {
            invokeEntity(this.original, "pre", entity);
            applyArmorColor(this.original);
            for (int i = 0; i < ADVANCED_PARTS.length; i++) {
                renderAdvancedPart(activeModel, entity, ADVANCED_PARTS[i]);
            }
        } finally {
            invokeEntity(this.original, "post", entity);
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
    }

    private void renderAdvancedPart(ModelBiped activeModel, Entity entity, Part part) {
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(activeModel, part.sourceNames);
        if (transform == null) {
            return;
        }

        GL11.glPushMatrix();
        try {
            GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
            invokeEntity(this.original, part.methodName, entity);
        } finally {
            GL11.glPopMatrix();
            transform.end();
        }
    }

    private static boolean hasSmartParts(ModelBiped model) {
        if (model == null) {
            return false;
        }

        for (int i = 0; i < ADVANCED_PARTS.length; i++) {
            if (!(ArmorModelSynchronizer.getPart(model, ADVANCED_PARTS[i].sourceNames) instanceof ModelRotationRenderer)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasSuperclass(Class<?> type, String className) {
        Class<?> current = type;
        while (current != null) {
            if (className.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static void applyArmorColor(Object model) {
        Integer color = getIntField(model, "color");
        if (color == null || color.intValue() == -1) {
            return;
        }

        int value = color.intValue();
        GL11.glColor3f(
            (float) (value >> 16 & 255) / 255.0F,
            (float) (value >> 8 & 255) / 255.0F,
            (float) (value & 255) / 255.0F);
    }

    private static Integer getIntField(Object owner, String fieldName) {
        Object value = getFieldValue(owner, fieldName);
        return value instanceof Integer ? (Integer) value : null;
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

    private static void invokeEntity(Object owner, String methodName, Entity entity) {
        Method method = findMethod(owner.getClass(), methodName, Entity.class);
        if (method == null) {
            return;
        }

        try {
            method.invoke(owner, entity);
        } catch (Throwable ignored) {
        }
    }

    private static void invokeStaticNoArgs(String className, String methodName) {
        try {
            Method method = Class.forName(className).getMethod(methodName);
            method.invoke(null);
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
        String key = type.getName() + "#" + methodName + "#" + parameterType.getName();
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

    private static final class Part {
        private final String[] sourceNames;
        private final String methodName;

        private Part(String[] sourceNames, String methodName) {
            this.sourceNames = sourceNames;
            this.methodName = methodName;
        }
    }
}
