package ru.biosoft.access.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Icon for default representation of given DataElement-descendant class or interface
 * Can be overridden using nodeImage/childrenNodeImage properties
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClassIcon
{
    String value();
}
