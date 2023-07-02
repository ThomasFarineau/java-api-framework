package fr.thomasfar.jaf.utils;

import fr.thomasfar.jaf.annotations.entities.Json;
import fr.thomasfar.jaf.annotations.entities.NotMapped;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

public class EntityClassVisitor extends ClassVisitor {
    private final List<FieldInfo> fields;
    String className;

    public EntityClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
        fields = new ArrayList<>();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = cv.visitField(access, name, desc, signature, value);
        fields.add(new FieldInfo(access, name, desc));
        return fv;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("toJson")) return null; // ignore if toJson method already exists
        for (FieldInfo field : fields) {
            if (name.equals("get" + capitalize(field.name))) return null;
            if (name.equals("set" + capitalize(field.name))) return null;
        }
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
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
            FieldInfo field = fields.get(i);
            if (i > 0) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn(", ");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mv.visitInsn(Opcodes.POP);
            }

            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn("\"" + field.name + "\": ");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitInsn(Opcodes.POP);

            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, field.name, field.desc);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitInsn(Opcodes.POP);
        }

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("}");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitInsn(Opcodes.POP);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(3, 2);
        mv.visitEnd();

        for (FieldInfo field : fields) {
            // Create getter
            MethodVisitor getterMv = cv.visitMethod(Opcodes.ACC_PUBLIC, "get" + capitalize(field.name), "()" + field.desc, null, null);
            getterMv.visitCode();
            getterMv.visitVarInsn(Opcodes.ALOAD, 0);
            getterMv.visitFieldInsn(Opcodes.GETFIELD, className, field.name, field.desc);
            getterMv.visitInsn(Opcodes.ARETURN);
            getterMv.visitMaxs(1, 1);
            getterMv.visitEnd();

            MethodVisitor setterMv = cv.visitMethod(Opcodes.ACC_PUBLIC, "set" + capitalize(field.name), "(" + field.desc + ")V", null, null);
            setterMv.visitCode();
            setterMv.visitVarInsn(Opcodes.ALOAD, 0);
            setterMv.visitVarInsn(Opcodes.ALOAD, 1);
            setterMv.visitFieldInsn(Opcodes.PUTFIELD, className, field.name, field.desc);
            setterMv.visitInsn(Opcodes.RETURN);
            setterMv.visitMaxs(2, 2);
            setterMv.visitEnd();
        }

        cv.visitEnd();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public static class FieldInfo {
        int access;
        String name;
        String desc;

        public FieldInfo(int access, String name, String desc) {
            this.access = access;
            this.name = name;
            this.desc = desc;
        }

        public String getJsonName() {
            return name;
        }

        @Override
        public String toString() {
            return "FieldInfo{" +
                    "access=" + access +
                    ", name='" + name + '\'' +
                    ", desc='" + desc + '\'' +
                    '}';
        }
    }
}