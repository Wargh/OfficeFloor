/*-
 * #%L
 * OfficeXml
 * %%
 * Copyright (C) 2005 - 2020 Daniel Sagenschneider
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package net.officefloor.plugin.xml.marshall.tree;

import java.lang.reflect.Method;

import net.officefloor.plugin.xml.XmlMarshallException;
import net.officefloor.plugin.xml.XmlOutput;
import net.officefloor.plugin.xml.marshall.translate.Translator;

/**
 * Writes a XML value with value sourced from object.
 * 
 * @author Daniel Sagenschneider
 */
public class ValueXmlMapping extends AbstractValueXmlMapping {

	/**
	 * From super.
	 * 
	 * @param tagName
	 *            Name of the XML tag.
	 * @param getMethod
	 *            {@link Method} to obtain the value.
	 * @param translator
	 *            {@link Translator}.
	 * @param isUseRaw
	 *            Indicates to use raw.
	 */
	public ValueXmlMapping(String tagName, Method getMethod,
			Translator translator, boolean isUseRaw) {
		super(tagName, getMethod, translator, isUseRaw);
	}

	@Override
	protected void writeXml(String tagName, String value, XmlOutput output)
			throws XmlMarshallException {

		// Write value based on value being null
		XmlMarshallerUtil.writeXml("<", output);
		XmlMarshallerUtil.writeXml(tagName, output);
		if (value == null) {
			// Value null
			XmlMarshallerUtil.writeXml("/>", output);
		} else {
			// Value supplied
			XmlMarshallerUtil.writeXml(">", output);
			XmlMarshallerUtil.writeXml(value, output);
			XmlMarshallerUtil.writeXml("</", output);
			XmlMarshallerUtil.writeXml(tagName, output);
			XmlMarshallerUtil.writeXml(">", output);
		}
	}

}
