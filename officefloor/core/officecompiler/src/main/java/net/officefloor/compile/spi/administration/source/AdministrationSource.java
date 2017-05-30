/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2013 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.officefloor.compile.spi.administration.source;

import net.officefloor.frame.api.administration.Administration;

/**
 * <p>
 * Source to obtain a particular type of {@link Administration}.
 * <p>
 * Implemented by the {@link Administration} provider.
 * 
 * @author Daniel Sagenschneider
 */
public interface AdministrationSource<E, F extends Enum<F>, G extends Enum<G>> {

	/**
	 * <p>
	 * Obtains the specification for this.
	 * <p>
	 * This will be called before any other methods, therefore this method must
	 * be able to return the specification immediately after a default
	 * constructor instantiation.
	 * 
	 * @return Specification of this.
	 */
	AdministrationSourceSpecification getSpecification();

	/**
	 * Called only once after the {@link AdministrationSource} is instantiated.
	 * 
	 * @param context
	 *            {@link AdministrationSourceContext} to initialise this
	 *            instance of the {@link AdministrationSource}.
	 * @throws Exception
	 *             Should the {@link AdministrationSource} fail to configure
	 *             itself from the input properties.
	 */
	void init(AdministrationSourceContext context) throws Exception;

	/**
	 * <p>
	 * Obtains the meta-data to describe this.
	 * <p>
	 * This is called after the {@link #init(AdministrationSourceContext)}
	 * method and therefore may use the configuration.
	 * <p>
	 * This should always return non-null. If there is a problem due to
	 * incorrect configuration, the {@link #init(AdministrationSourceContext)}
	 * should indicate this via an exception.
	 * 
	 * @return Meta-data to describe this.
	 */
	@Deprecated // make return of init
	AdministrationSourceMetaData<E, F, G> getMetaData();

}