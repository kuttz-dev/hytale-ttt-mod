package ar.ncode.plugin.patches;

import com.hypixel.hytale.plugin.early.ClassTransformer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Optional;

public final class ExplosionTransformer implements ClassTransformer {

    private static final String TARGET_CLASS_NAME = "com.hypixel.hytale.server.core.entity.ExplosionUtils";
    private static final String TARGET_METHOD_NAME = "performExplosion";
    private static final String HOOK_METHOD_NAME = "ttt$onExplosionEnter";
    private static final MethodTypeDesc TARGET_METHOD_TYPE = MethodTypeDesc.ofDescriptor(
            "(Lcom/hypixel/hytale/server/core/modules/entity/damage/Damage$Source;" +
                    "Lcom/hypixel/hytale/math/vector/Vector3d;" +
                    "Lcom/hypixel/hytale/server/core/entity/ExplosionConfig;" +
                    "Lcom/hypixel/hytale/component/Ref;" +
                    "Lcom/hypixel/hytale/component/CommandBuffer;" +
                    "Lcom/hypixel/hytale/component/ComponentAccessor;)V"
    );
    private static final ClassDesc TARGET_CLASS_DESC = ClassDesc.of(TARGET_CLASS_NAME);
    private static final MethodTypeDesc HOOK_METHOD_TYPE = MethodTypeDesc.ofDescriptor(
            "(Lcom/hypixel/hytale/server/core/entity/ExplosionConfig;" +
                    "Lcom/hypixel/hytale/component/CommandBuffer;)V"
    );

    @Override
    public byte[] transform(String className, String internalName, byte[] classBytes) {
        if (!TARGET_CLASS_NAME.equals(className)) {
            return null;
        }

        try {
            ClassFile classFile = ClassFile.of();
            var model = classFile.parse(classBytes);
            Optional<MethodModel> hookMethod = loadTemplateHook(classFile);

            if (hookMethod.isEmpty()) {
                System.err.println("[EarlyPlugin] Failed to locate template hook method");
                return null;
            }

            return classFile.transformClass(
                    model,
                    ClassTransform.transformingMethods(this::isTargetMethod, MethodTransform.transformingCode(injectEntryHook()))
                            .andThen(ClassTransform.endHandler(builder ->
                                    builder.transformMethod(hookMethod.get(), MethodTransform.ACCEPT_ALL)
                            ))
            );

        } catch (Throwable throwable) {
            System.err.println("[EarlyPlugin] Failed to transform " + className + ": " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        }
    }

    private boolean isTargetMethod(MethodModel method) {
        return TARGET_METHOD_NAME.equals(method.methodName().stringValue())
                && TARGET_METHOD_TYPE.equals(method.methodTypeSymbol());
    }

    private CodeTransform injectEntryHook() {
        return CodeTransform.ofStateful(() -> {
            boolean[] injected = {false};

            return (builder, element) -> {
                if (!injected[0]) {
                    injected[0] = true;
                    builder.aload(2);
                    builder.aload(4);
                    builder.invokestatic(TARGET_CLASS_DESC, HOOK_METHOD_NAME, HOOK_METHOD_TYPE);
                }

                builder.with(element);
            };
        });
    }

    private Optional<MethodModel> loadTemplateHook(ClassFile classFile) throws Exception {
        String resourceName = ExplosionMethodTemplate.class.getName().replace('.', '/') + ".class";
        try (var inputStream = ExplosionMethodTemplate.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                return Optional.empty();
            }

            var templateClass = classFile.parse(inputStream.readAllBytes());
            return templateClass.methods().stream()
                    .filter(method -> HOOK_METHOD_NAME.equals(method.methodName().stringValue()))
                    .filter(method -> HOOK_METHOD_TYPE.equals(method.methodTypeSymbol()))
                    .findFirst();
        }
    }
}
