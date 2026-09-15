package com.ivanc.smartmovingarmorcompat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.smart.render.ModelRotationRenderer;
import org.lwjgl.opengl.GL11;

public class GalaxySpaceArmorModelWrapper extends ModelBiped {
    private static final String MODEL_BASE = "galaxyspace.core.client.models.ModelOBJArmor";
    private static final Part[] PARTS = new Part[] {
        new Part(SmartRenderTransformHelper.HEAD_PART, "partHead"),
        new Part(SmartRenderTransformHelper.BODY_PART, "partBody"),
        new Part(new String[] {"bipedRightArm", "field_78112_f"}, "partRightArm"),
        new Part(new String[] {"bipedLeftArm", "field_78113_g"}, "partLeftArm"),
        new Part(new String[] {"bipedRightLeg", "field_78123_h"}, "partRightLeg"),
        new Part(new String[] {"bipedLeftLeg", "field_78124_i"}, "partLeftLeg")
    };
    private static final Map<String, Method> METHOD_CACHE = new HashMap<String, Method>();

    private final ModelBiped original;
    private ModelBiped sourceModel;

    public GalaxySpaceArmorModelWrapper(ModelBiped original, ModelBiped sourceModel) {
        this.original = original;
        this.sourceModel = sourceModel;
    }

    public static boolean isGalaxySpaceArmorModel(ModelBiped model) {
        if (model == null) {
            return false;
        }

        Class<?> current = model.getClass();
        while (current != null) {
            if (MODEL_BASE.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return model.getClass().getName().startsWith("galaxyspace.") && hasPartMethods(model.getClass());
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

        GL11.glPushMatrix();
        boolean prepared = false;
        try {
            prepared = invoke(this.original, "pre");
            applyArmorColor(this.original);
            for (int i = 0; i < PARTS.length; i++) {
                renderPart(activeModel, PARTS[i]);
            }
        } finally {
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            if (prepared) {
                invoke(this.original, "post");
            }
            GL11.glPopMatrix();
        }
    }

    private static boolean hasSmartParts(ModelBiped activeModel) {
        if (activeModel == null) {
            return false;
        }

        for (int i = 0; i < PARTS.length; i++) {
            if (!(ArmorModelSynchronizer.getPart(activeModel, PARTS[i].sourceNames) instanceof ModelRotationRenderer)) {
                return false;
            }
        }
        return true;
    }

    private void renderPart(ModelBiped activeModel, Part part) {
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(activeModel, part.sourceNames);
        if (transform == null) {
            return;
        }

        GL11.glPushMatrix();
        try {
            GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
            invoke(this.original, part.methodName);
        } finally {
            GL11.glPopMatrix();
            transform.end();
        }
    }

    private static boolean hasPartMethods(Class<?> type) {
        for (int i = 0; i < PARTS.length; i++) {
            if (findMethod(type, PARTS[i].methodName) == null) {
                return false;
            }
        }
        return findMethod(type, "pre") != null && findMethod(type, "post") != null;
    }

    private static void applyArmorColor(Object model) {
        Field field = findIntField(model.getClass(), "color");
        if (field == null) {
            return;
        }

        try {
            int color = field.getInt(model);
            if (color == -1) {
                return;
            }

            float red = (float) (color >> 16 & 255) / 255.0F;
            float green = (float) (color >> 8 & 255) / 255.0F;
            float blue = (float) (color & 255) / 255.0F;
            GL11.glColor3f(red, green, blue);
        } catch (Throwable ignored) {
        }
    }

    private static Field findIntField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() == Integer.TYPE) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return null;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean invoke(Object owner, String methodName) {
        Method method = findMethod(owner.getClass(), methodName);
        if (method == null) {
            return false;
        }

        try {
            method.invoke(owner);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method findMethod(Class<?> type, String methodName) {
        String key = type.getName() + "#" + methodName;
        if (METHOD_CACHE.containsKey(key)) {
            return METHOD_CACHE.get(key);
        }

        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
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
