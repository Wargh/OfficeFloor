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
package net.officefloor.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.officefloor.server.http.HttpRequest;
import net.officefloor.web.build.HttpArgumentParser;

/**
 * Annotation to in-line configuration of parameters from content of
 * {@link HttpRequest}.
 * 
 * @author Daniel Sagenschneider
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
public @interface HttpContentParameter {

	/**
	 * Name of the parameter.
	 * 
	 * @return Name of the parameter. Use blank string to default to property
	 *         name.
	 */
	String name();

	/**
	 * {@link HttpArgumentParser} instances to retrieve the arguments.
	 * 
	 * @return {@link HttpArgumentParser} instances to retrieve the arguments.
	 */
	Class<? extends HttpArgumentParser>[] content() default {};

}