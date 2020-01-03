package net.officefloor.web.security.build;

import net.officefloor.web.spi.security.HttpSecurity;

/**
 * Securable HTTP item.
 * 
 * @author Daniel Sagenschneider
 */
public interface HttpSecurable {

	/**
	 * Obtains the name for the {@link HttpSecurity} to use. May be
	 * <code>null</code> if generic {@link HttpSecurity}.
	 * 
	 * @return Name of {@link HttpSecurity} or <code>null</code> for generic
	 *         {@link HttpSecurity}.
	 */
	String getHttpSecurityName();

	/**
	 * <p>
	 * Obtains the list of roles that must have at least one for access.
	 * <p>
	 * Empty/<code>null</code> list means needs only be authenticated.
	 * 
	 * @return List of any roles.
	 */
	String[] getAnyRoles();

	/**
	 * <p>
	 * Obtains the list of roles that must have all for access.
	 * <p>
	 * Empty/<code>null</code> list means needs only be authenticated.
	 * 
	 * @return List of required roles.
	 */
	String[] getRequiredRoles();

}