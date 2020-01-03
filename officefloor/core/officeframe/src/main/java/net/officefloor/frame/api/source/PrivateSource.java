package net.officefloor.frame.api.source;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.managedobject.ManagedObject;

/**
 * <p>
 * Annotation to be applied to private sources so they are ignored from being
 * dynamically &quot;discovered&quot; for use in configuration.
 * <p>
 * Typically source instances are always available but in some circumstances,
 * for example, there may be specific {@link ManagedFunction} or
 * {@link ManagedObject} sources that are built specific to the section (and
 * make no sense using outside the section).
 * 
 * @author Daniel Sagenschneider
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PrivateSource {
}