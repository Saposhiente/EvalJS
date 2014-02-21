package com.github.Saposhiente.EvalJS;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Saposhiente
 */
public class ClassInspector {

    public static final byte ALL = 0b1_111_11;
    public static final byte ALL_ACCESS = 0b11;
    public static final byte PUBLIC = 0b1;
    public static final byte DECLARED = 0b10;
    public static final byte ALL_ACCESSIBLES = 0b111_00;
    public static final byte FIELDS = 0b1_00;
    public static final byte METHODS = 0b10_00;
    public static final byte CONSTRUCTORS = 0b100_00;
    /**
     * If false, will only view static variables
     */
    public static final byte INSTANCE = 0b1_000_00;

    public static String getReturnTypeNameOf(final Method method) {
        method.getGenericParameterTypes();
        final Class clazz = method.getReturnType();
        if (clazz.getTypeParameters().length == 0) {
            return clazz.getSimpleName();
        } else {
            return method.getGenericReturnType().toString();
        }
    }

    public static String getTypeNameOf(final Field field) {
        final Class clazz = field.getType();
        if (clazz.getTypeParameters().length == 0) {
            return clazz.getSimpleName();
        } else {
            return field.getGenericType().toString();
        }
    }

    public static String getParameterNameOf(final Type[] genericParameters, final Class[] parameters, int pos) {
        final Class clazz = parameters[pos];
        if (clazz.getTypeParameters().length == 0) {
            return clazz.getSimpleName();
        } else {
            return genericParameters[pos].toString();
        }
    }
    public final JavascriptRunner bridge;

    public ClassInspector(final JavascriptRunner bridge) {
        this.bridge = bridge;
    }

    public static String getDescription(final Object obj, final Field field, final Class asClass) {
        try {
            if (!Modifier.isPublic(field.getModifiers())) {
                field.setAccessible(true);
            }
            return Modifier.toString(field.getModifiers()) + " " + getTypeNameOf(field) + " " + (field.getDeclaringClass().equals(asClass) ? "" : field.getDeclaringClass().getSimpleName() + ".") + field.getName() + " --> " + field.get(obj);
        } catch (IllegalAccessException ex) {
            EvalJS.instance.getLogger().log(Level.SEVERE, "Could not view protected field " + field.getName() + ": ", ex);
            return "..Could not view protected field " + field.getName() + ". See console for details.";
        }
    }

    public static String getDescription(final Object obj, final Method method, final Class asClass) {
        final Class[] parameters = method.getParameterTypes();
        final boolean noparams = parameters.length == 0;
        if (noparams && !Modifier.isAbstract(method.getModifiers()) && ((method.getName().startsWith("get") && method.getName().length() > 3 && Character.isUpperCase(method.getName().charAt(3))
                && !method.getGenericReturnType().toString().equals("void") && !method.getGenericReturnType().toString().equals("boolean"))
                || (method.getName().startsWith("is") && method.getName().length() > 2 && Character.isUpperCase(method.getName().charAt(2))
                && method.getGenericReturnType().toString().equals("boolean")))) {
            try {
                if (!Modifier.isPublic(method.getModifiers())) {
                    method.setAccessible(true);
                }
                return Modifier.toString(method.getModifiers()) + " " + getReturnTypeNameOf(method) + " " + (method.getDeclaringClass().equals(asClass) ? "" : method.getDeclaringClass().getSimpleName() + ".") + method.getName() + "() --> " + method.invoke(obj);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                EvalJS.instance.getLogger().log(Level.SEVERE, "Could not execute protected method " + method.getName() + ": ", ex);
                return "..Could not execute protected method " + method.getName() + ". See console for details.";
            }
        } else {
            final String args;
            if (!noparams) {
                final StringBuilder builder = new StringBuilder();
                final Type[] genericParameters = method.getGenericParameterTypes();
                builder.append("(");
                for (int i = 0;;) {
                    builder.append(getParameterNameOf(genericParameters, parameters, i));
                    i++;
                    if (i < parameters.length) {
                        builder.append(", ");
                    } else {
                        builder.append(")");
                        break;
                    }
                }
                args = builder.toString();
            } else {
                args = "()";
            }
            return Modifier.toString(method.getModifiers()) + " " + getReturnTypeNameOf(method) + " " + (method.getDeclaringClass().equals(asClass) ? "" : method.getDeclaringClass().getSimpleName() + ".") + method.getName() + args;
        }
    }

    public static String getDescription(final Class clazz, final Constructor constructor) {
        final Class[] parameters = constructor.getParameterTypes();
        final boolean noparams = parameters.length == 0;
        final String args;
        if (!noparams) {
            final StringBuilder builder = new StringBuilder();
            final Type[] genericParameters = constructor.getGenericParameterTypes();
            builder.append("(");
            for (int i = 0;;) {
                builder.append(getParameterNameOf(genericParameters, parameters, i));
                i++;
                if (i < parameters.length) {
                    builder.append(", ");
                } else {
                    builder.append(")");
                    break;
                }
            }
            args = builder.toString();
        } else {
            args = "()";
        }
        return Modifier.toString(constructor.getModifiers()) + " " + clazz.getSimpleName() + args;
    }

    public void substituteClassDef(final NoClassDefFoundError err, final int recursions) {
        if (recursions < 0) {
            throw new RuntimeException("Could not print fields: Too many NoClassDefFoundErrors!", err);
        }
        bridge.verbose("Substituting class def for " + err);
        EvalJS.instance.substitutor.substituteClassDef(err);
    }

    public void printFields(final Object obj, final Class asClass, final boolean viewInstance, final String searchName, byte recursions) {
        try {
            for (final Field f : asClass.getFields()) {
                if ((viewInstance || Modifier.isStatic(f.getModifiers())) && f.getName().contains(searchName)) {
                    bridge.bufferedOutput(getDescription(obj, f, asClass));
                }
            }
        } catch (final NoClassDefFoundError err) {
            substituteClassDef(err, recursions);
            printFields(obj, asClass, viewInstance, searchName, ++recursions);
        }
    }

    public void printDeclaredFields(final Object obj, final Class asClass, final boolean viewInstance, final String searchName, final boolean getPublic, byte recursions) {
        try {
            for (final Field f : asClass.getDeclaredFields()) {
                if ((viewInstance || Modifier.isStatic(f.getModifiers())) && (getPublic || !Modifier.isPublic(f.getModifiers())) && f.getName().contains(searchName)) {
                    bridge.bufferedOutput(getDescription(obj, f, asClass));
                }
            }
        } catch (final NoClassDefFoundError err) {
            substituteClassDef(err, recursions);
            printDeclaredFields(obj, asClass, viewInstance, searchName, getPublic, ++recursions);
        }
    }

    public void printMethods(final Object obj, final Class asClass, final boolean viewInstance, final String searchName, byte recursions) {
        // <editor-fold defaultstate="collapsed" desc="etc">
        try {
            for (final Method m : asClass.getMethods()) {
                if ((viewInstance || Modifier.isStatic(m.getModifiers())) && m.getName().contains(searchName)) {
                    bridge.bufferedOutput(getDescription(obj, m, asClass));
                }
            }
        } catch (final NoClassDefFoundError err) {
            substituteClassDef(err, recursions);
            printMethods(obj, asClass, viewInstance, searchName, ++recursions);
        }
        // </editor-fold>
    }

    public void printDeclaredMethods(final Object obj, final Class asClass, final boolean viewInstance, final String searchName, final boolean getPublic, byte recursions) {
        // <editor-fold defaultstate="collapsed" desc="etc">
        try {
            for (final Method m : asClass.getDeclaredMethods()) {
                if ((viewInstance || Modifier.isStatic(m.getModifiers())) && (getPublic || !Modifier.isPublic(m.getModifiers())) && m.getName().contains(searchName)) {
                    bridge.bufferedOutput(getDescription(obj, m, asClass));
                }
            }
        } catch (final NoClassDefFoundError err) {
            substituteClassDef(err, recursions);
            printDeclaredMethods(obj, asClass, viewInstance, searchName, getPublic, ++recursions);
        }
        // </editor-fold>
    }

    public void printConstructors(final Object obj, final Class asClass, final String searchName, byte recursions) {
        // <editor-fold defaultstate="collapsed" desc="etc">
        try {
            for (final Constructor c : asClass.getConstructors()) {
                if (c.getName().contains(searchName)) {
                    bridge.bufferedOutput(getDescription(asClass, c));
                }
            }
        } catch (final NoClassDefFoundError err) {
            substituteClassDef(err, recursions);
            printConstructors(obj, asClass, searchName, ++recursions);
        }
        // </editor-fold>
    }

    public void printDeclaredConstructors(final Object obj, final Class asClass, final String searchName, final boolean getPublic, byte recursions) {
        // <editor-fold defaultstate="collapsed" desc="etc">
        try {
            for (final Constructor c : asClass.getDeclaredConstructors()) {
                if ((getPublic || !Modifier.isPublic(c.getModifiers())) && c.getName().contains(searchName)) {
                    bridge.bufferedOutput(getDescription(asClass, c));
                }
            }
        } catch (final NoClassDefFoundError err) {
            substituteClassDef(err, recursions);
            printDeclaredConstructors(obj, asClass, searchName, getPublic, ++recursions);
        }
        // </editor-fold>
    }
    private static final byte ZERO = 0;

    public void inspect(final Object obj, final Class asClass, final byte view, final String searchName) {
        final boolean viewPublic = (view & PUBLIC) != 0;
        final boolean viewDeclared = (view & DECLARED) != 0;
        final boolean viewInstance = (view & INSTANCE) != 0;
        if ((view & FIELDS) != 0) {
            if (viewPublic) {
                printFields(obj, asClass, viewInstance, searchName, ZERO);
                if (viewDeclared) {
                    printDeclaredFields(obj, asClass, viewInstance, searchName, false, ZERO);
                }
            } else if (viewDeclared) {
                printDeclaredFields(obj, asClass, viewInstance, searchName, true, ZERO);
            }
        }
        if ((view & METHODS) != 0) {
            // <editor-fold defaultstate="collapsed" desc="etc">
            if (viewPublic) {
                printMethods(obj, asClass, viewInstance, searchName, ZERO);
                if (viewDeclared) {
                    printDeclaredMethods(obj, asClass, viewInstance, searchName, false, ZERO);
                }
            } else if (viewDeclared) {
                printDeclaredMethods(obj, asClass, viewInstance, searchName, true, ZERO);
            }
            // </editor-fold>
        }
        if (!viewInstance && (view & CONSTRUCTORS) != 0) {
            // <editor-fold defaultstate="collapsed" desc="etc">
            if (viewPublic) {
                printConstructors(obj, asClass, searchName, ZERO);
                if (viewDeclared) {
                    printDeclaredConstructors(obj, asClass, searchName, false, ZERO);
                }
            } else if (viewDeclared) {
                printDeclaredConstructors(obj, asClass, searchName, true, ZERO);
            }
            // </editor-fold>
        }
    }

    public static Class rhinoClassToJava(String className) throws ClassNotFoundException {
        int end = className.length() - 1;
        int start = className.lastIndexOf(" ", end) + 1;
        return Class.forName(className.substring(start, end));
    }
    
    public static List<Object> find(Class clazz, String name, boolean viewInstance) {
        EvalJS.instance.substitutor.substituteClassDefs(clazz);
        List<Object> results = new ArrayList<>();
        do {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getName().contains(name) && (viewInstance || Modifier.isStatic(f.getModifiers()))) results.add(f);
            }
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().contains(name) && (viewInstance || Modifier.isStatic(m.getModifiers()))) results.add(m);
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return results;
    }
}
