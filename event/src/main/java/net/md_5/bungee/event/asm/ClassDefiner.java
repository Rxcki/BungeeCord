package net.md_5.bungee.event.asm;


import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

public interface ClassDefiner
{

    /**
     * Returns if the defined classes can bypass access checks
     *
     * @return if classes bypass access checks
     */
    default boolean isBypassAccessChecks()
    {
        return false;
    }

    /**
     * Define a class
     *
     * @param parentLoader the parent classloader
     * @param name the name of the class
     * @param data the class data to load
     * @return the defined class
     * @throws ClassFormatError if the class data is invalid
     * @throws NullPointerException if any of the arguments are null
     */
    Class<?> defineClass(ClassLoader parentLoader, Type name, byte[] data);

    static ClassDefiner getInstance()
    {
        return SafeClassDefiner.INSTANCE;
    }
}
