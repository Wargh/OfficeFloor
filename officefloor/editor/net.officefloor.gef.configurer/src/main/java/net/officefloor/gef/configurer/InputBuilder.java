/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
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
package net.officefloor.gef.configurer;

import java.util.Properties;
import java.util.function.Function;

import javafx.collections.ObservableList;
import net.officefloor.compile.properties.PropertyList;

/**
 * Builder of the inputs.
 * 
 * @author Daniel Sagenschneider
 */
public interface InputBuilder<M> extends ItemBuilder<M> {

	/**
	 * Builds choices in configuration.
	 * 
	 * @param label Label for the choices.
	 * @return {@link ChoiceBuilder}.
	 */
	ChoiceBuilder<M> choices(String label);

	/**
	 * Configures creating a list of items.
	 * 
	 * @param <I>      Item type.
	 * @param label    Label for the items.
	 * @param itemType Item type.
	 * @return {@link ListBuilder}.
	 */
	<I> ListBuilder<M, I> list(String label, Class<I> itemType);

	/**
	 * Configures selecting from a list of items.
	 * 
	 * @param <I>      Item type.
	 * @param label    Label for the selection.
	 * @param getItems Function to extract the items.
	 * @return {@link SelectBuilder}.
	 */
	<I> SelectBuilder<M, I> select(String label, Function<M, ObservableList<I>> getItems);

	/**
	 * Configures multiple items.
	 *
	 * @param <I>      Item type.
	 * @param label    Label for the items.
	 * @param itemType Item type.
	 * @return {@link MultipleBuilder}.
	 */
	<I> MultipleBuilder<M, I> multiple(String label, Class<I> itemType);

	/**
	 * Configures {@link PropertyList}.
	 * 
	 * @param label Label for the {@link Properties}.
	 * @return {@link PropertiesBuilder}.
	 */
	PropertiesBuilder<M> properties(String label);

	/**
	 * Configures a mapping of name to name.
	 * 
	 * @param label      Label for the mapping.
	 * @param getSources {@link Function} to extract the sources.
	 * @param getTargets {@link Function} to extract the targets.
	 * @return {@link MappingBuilder}.
	 */
	MappingBuilder<M> map(String label, Function<M, ObservableList<String>> getSources,
			Function<M, ObservableList<String>> getTargets);

	/**
	 * Adds a {@link Class} property to be configured.
	 * 
	 * @param label Label.
	 * @return {@link ClassBuilder}.
	 */
	ClassBuilder<M> clazz(String label);

	/**
	 * Adds a resource property to be configured.
	 * 
	 * @param label Label.
	 * @return {@link ResourceBuilder}.
	 */
	ResourceBuilder<M> resource(String label);

}