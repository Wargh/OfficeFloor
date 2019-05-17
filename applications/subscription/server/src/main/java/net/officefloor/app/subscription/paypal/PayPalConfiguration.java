/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2019 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.officefloor.app.subscription.paypal;

import com.googlecode.objectify.Objectify;
import com.paypal.core.PayPalEnvironment;

import net.officefloor.app.subscription.store.PayPalCredentials;
import net.officefloor.pay.paypal.PayPalConfigurationRepository;
import net.officefloor.plugin.managedobject.clazz.Dependency;

/**
 * Provides the PayPal configuration.
 * 
 * @author Daniel Sagenschneider
 */
public class PayPalConfiguration implements PayPalConfigurationRepository {

	@Dependency
	private Objectify objectify;

	@Override
	public PayPalEnvironment createPayPalEnvironment() {
		PayPalCredentials credentials = this.objectify.load().type(PayPalCredentials.class).first().now();
		if (credentials == null) {
			return null;
		}
		switch (credentials.getEnvironment()) {
		case "sandbox":
			return new PayPalEnvironment.Sandbox(credentials.getClientId(), credentials.getClientSecret());
		case "live":
			return new PayPalEnvironment.Live(credentials.getClientId(), credentials.getClientSecret());
		default:
			throw new IllegalStateException("Unknown PayPal environment: " + credentials.getEnvironment());
		}
	}

}