package fr.thomasfar.jaf.core.reflections.entity;

import fr.thomasfar.jaf.annotations.entities.Json;
import fr.thomasfar.jaf.annotations.entities.NotMapped;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.List;

public class EntityClassVisitor extends ClassVisitor {
    private final List<FieldRegister> fields;
    String className;

    public EntityClassVisitor(ClassVisitor cv, List<FieldRegister> fields) {
        super(Opcodes.ASM9, cv);
        this.fields = fields;
    }

    private static MethodNode removeToJson(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("toJson")) {
            return new MethodNode(Opcodes.ASM9, access, name, desc, signature, exceptions) {
                @Override
                public void visitEnd() {
                    // remove all instructions
                    instructions.clear();
                    // add return "{}"
                    visitLdcInsn("{}");
                    visitInsn(Opcodes.ARETURN);
                    super.visitEnd();
                }
            };
        }
        return null;
    }

    private static void invokeAppend(MethodVisitor mv, Class<?> type) {
        if (type == int.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
        } else if (type == long.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
        } else if (type == float.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(F)Ljava/lang/StringBuilder;", false);
        } else if (type == double.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(D)Ljava/lang/StringBuilder;", false);
        } else if (type == char.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;", false);
        } else if (type == short.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(S)Ljava/lang/StringBuilder;", false);
        } else if (type == byte.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(B)Ljava/lang/StringBuilder;", false);
        } else if (type == boolean.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", false);
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // if toJson exist, delete it
        MethodNode removeToJson = removeToJson(access, name, desc, signature, exceptions);
        if (removeToJson != null) return removeToJson;
        // check all getter, delete it
        MethodNode removeGetterAndSetter = removeGetterAndSetter(access, name, desc, signature, exceptions);
        if (removeGetterAndSetter != null) return removeGetterAndSetter;
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }

    private MethodNode removeGetterAndSetter(int access, String name, String desc, String signature, String[] exceptions) {
        for (FieldRegister field : fields) {
            String fieldName = field.name();
            String fieldDescriptor = field.descriptor();
            String fieldGetter = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            String fieldSetter = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

            if (name.equals(fieldGetter) && desc.equals("()" + fieldDescriptor)) {
                return new MethodNode(Opcodes.ASM9, access, name, desc, signature, exceptions) {
                    @Override
                    public void visitEnd() {
                        instructions.clear();
                        visitVarInsn(Opcodes.ALOAD, 0);
                        visitFieldInsn(Opcodes.GETFIELD, className, fieldName, fieldDescriptor);
                        visitInsn(Opcodes.ARETURN);
                        super.visitEnd();
                    }
                };
            }

            if (name.equals(fieldSetter) && desc.equals("(" + fieldDescriptor + ")V")) {
                return new MethodNode(Opcodes.ASM9, access, name, desc, signature, exceptions) {
                    @Override
                    public void visitEnd() {
                        instructions.clear();
                        visitVarInsn(Opcodes.ALOAD, 0);
                        visitVarInsn(Opcodes.ALOAD, 1);
                        visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, fieldDescriptor);
                        visitInsn(Opcodes.RETURN);
                        super.visitEnd();
                    }
                };
            }
        }
        return null;
    }

    @Override
    public void visitEnd() {
        createGetter();
        createSetter();
        createToJson();
        super.visitEnd();
    }

    private void createGetter() {
        fields.forEach(field -> {
            String fieldName = field.name();
            String fieldDescriptor = field.descriptor();
            String fieldGetter = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, fieldGetter, "()" + fieldDescriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, fieldDescriptor);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        });
    }

    private void createSetter() {
        fields.stream().filter(field -> !field.isFinal()).forEach(field -> {
            String fieldName = field.name();
            String fieldDescriptor = field.descriptor();
            String fieldSetter = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, fieldSetter, "(" + fieldDescriptor + ")V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, fieldDescriptor);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        });
    }

    private void createToJson() {
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "toJson", "()Ljava/lang/String;", null, null);
        mv.visitCode();

        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("{");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitInsn(Opcodes.POP);

        for (int i = 0; i < fields.size(); i++) {
            FieldRegister field = fields.get(i);
            if (field.hasAnnotation(NotMapped.class)) continue;

            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn("\"");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitInsn(Opcodes.POP);

            if (field.hasAnnotation(Json.class)) {
                Json json = (Json) field.getAnnotation(Json.class);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn(json.value());
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn(field.name());
            }
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitInsn(Opcodes.POP);

            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn("\":");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitInsn(Opcodes.POP);

            if (!field.type().isPrimitive()) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn("\"");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mv.visitInsn(Opcodes.POP);
            }

            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, field.name(), field.descriptor());
            if (field.type().isPrimitive()) {
                invokeAppend(mv, field.type());
            } else {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
            }

            if (!field.type().isPrimitive()) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn("\"");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mv.visitInsn(Opcodes.POP);
            }

            if (i < fields.size() - 1) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn(",");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mv.visitInsn(Opcodes.POP);
            }
        }

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("}");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitInsn(Opcodes.POP);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

}