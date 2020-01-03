package net.officefloor.compile.spi.section;

import net.officefloor.compile.internal.structure.SectionNode;
import net.officefloor.compile.managedobject.ManagedObjectDependencyType;
import net.officefloor.compile.managedobject.ManagedObjectFlowType;
import net.officefloor.compile.properties.PropertyConfigurable;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.api.managedobject.source.ManagedObjectSource;
import net.officefloor.frame.internal.structure.ManagedObjectScope;

/**
 * {@link ManagedObjectSource} within an {@link SectionNode}.
 * 
 * @author Daniel Sagenschneider
 */
public interface SectionManagedObjectSource extends PropertyConfigurable {

	/**
	 * Obtains the name of this {@link SectionManagedObjectSource}.
	 * 
	 * @return Name of this {@link SectionManagedObjectSource}.
	 */
	String getSectionManagedObjectSourceName();

	/**
	 * Specifies the timeout for the {@link ManagedObject}.
	 * 
	 * @param timeout
	 *            Timeout for the {@link ManagedObject}.
	 */
	void setTimeout(long timeout);

	/**
	 * Obtains the {@link SectionManagedObjectFlow} for the
	 * {@link ManagedObjectFlowType}.
	 * 
	 * @param managedObjectSourceFlowName
	 *            Name of the {@link ManagedObjectFlowType}.
	 * @return {@link SectionManagedObjectFlow}.
	 */
	SectionManagedObjectFlow getSectionManagedObjectFlow(String managedObjectSourceFlowName);

	/**
	 * Obtains the {@link SectionManagedObjectDependency} for the
	 * {@link ManagedObjectDependencyType} for the Input {@link ManagedObject}.
	 * 
	 * @param managedObjectDependencyName
	 *            Name of the {@link ManagedObjectDependencyType}.
	 * @return {@link SectionManagedObjectDependency}.
	 */
	SectionManagedObjectDependency getInputSectionManagedObjectDependency(String managedObjectDependencyName);

	/**
	 * Obtains the {@link SectionManagedObject} representing an instance use of
	 * a {@link ManagedObject} from the {@link ManagedObjectSource}.
	 * 
	 * @param managedObjectName
	 *            Name of the {@link SectionManagedObject}. Typically this will
	 *            be the name under which the {@link ManagedObject} will be
	 *            registered to the {@link Office}.
	 * @param managedObjectScope
	 *            {@link ManagedObjectScope} of the {@link SectionManagedObject}
	 *            within the {@link Office}.
	 * @return {@link SectionManagedObject}.
	 */
	SectionManagedObject addSectionManagedObject(String managedObjectName, ManagedObjectScope managedObjectScope);

}