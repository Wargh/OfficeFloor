package net.officefloor.compile.spi.office.source;

/**
 * Property of the {@link OfficeSourceSpecification}.
 * 
 * @author Daniel Sagenschneider
 */
public interface OfficeSourceProperty {

	/**
	 * Obtains the name of the property.
	 * 
	 * @return Name of the property.
	 */
	String getName();

	/**
	 * Obtains the display name of the property. If this returns
	 * <code>null</code> then the return value of {@link #getName()} is used.
	 * 
	 * @return Display name of property.
	 */
	String getLabel();

}