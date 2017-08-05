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
package net.officefloor.server.http;

/**
 * Root {@link Exception} of all HTTP related exceptions.
 *
 * @author Daniel Sagenschneider
 */
public class HttpException extends Exception {

	/**
	 * {@link HttpStatus}.
	 */
	private final HttpStatus httpStatus;

	/**
	 * Initiate.
	 *
	 * @param httpStatus
	 *            {@link HttpStatus} of exception.
	 */
	public HttpException(HttpStatus httpStatus) {
		this.httpStatus = httpStatus;
	}

	/**
	 * Initiate.
	 *
	 * @param httpStatus
	 *            {@link HttpStatus} of exception.
	 * @param message
	 *            Message.
	 * @param cause
	 *            Cause.
	 */
	public HttpException(HttpStatus httpStatus, String message, Throwable cause) {
		super(message, cause);
		this.httpStatus = httpStatus;
	}

	/**
	 * Initiate.
	 *
	 * @param httpStatus
	 *            {@link HttpStatus} of exception.
	 * @param message
	 *            Message.
	 */
	public HttpException(HttpStatus httpStatus, String message) {
		super(message);
		this.httpStatus = httpStatus;
	}

	/**
	 * Initiate.
	 *
	 * @param httpStatus
	 *            {@link HttpStatus} of exception.
	 * @param cause
	 *            Cause.
	 */
	public HttpException(HttpStatus httpStatus, Throwable cause) {
		super(cause);
		this.httpStatus = httpStatus;
	}

	/**
	 * Obtains the {@link HttpStatus} of this exception.
	 *
	 * @return {@link HttpStatus} of this exception.
	 */
	public HttpStatus getHttpStatus() {
		return this.httpStatus;
	}

}