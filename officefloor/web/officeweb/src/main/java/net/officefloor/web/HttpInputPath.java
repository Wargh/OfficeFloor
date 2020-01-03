package net.officefloor.web;

import net.officefloor.server.http.HttpException;
import net.officefloor.web.build.HttpInput;
import net.officefloor.web.build.HttpPathFactory;

/**
 * Provides path details for the {@link HttpInput}.
 * 
 * @author Daniel Sagenschneider
 */
public interface HttpInputPath {

	/**
	 * Indicates if the path matches the {@link HttpInput} path.
	 * 
	 * @param path
	 *            Path.
	 * @param endingPathParameterTerminatingCharacter
	 *            {@link Character} value for the {@link Character} that terminates
	 *            the ending path parameter. This is ignored if the last part of the
	 *            path is static (i.e. only applies for last parameter to know when
	 *            it terminates the path, e.g. <code>/path/{last}</code>). Should
	 *            the last parameter consume the remainder of the path, provide
	 *            <code>-1</code> to indicate no terminating {@link Character}.
	 * @return <code>true</code> if the path matches the {@link HttpInput} path.
	 */
	boolean isMatchPath(String path, int endingPathParameterTerminatingCharacter);

	/**
	 * Indicates if the path contains parameters (e.g. <code>/{param}</code>).
	 * 
	 * @return <code>true</code> if the path contains parameters.
	 */
	boolean isPathParameters();

	/**
	 * Creates the {@link HttpPathFactory}.
	 * 
	 * @param <T>
	 *            Value type.
	 * @param valuesType
	 *            Type to use for obtaining values to construct the path. Should the
	 *            path not contain parameters, it may be <code>null</code>.
	 * @return {@link HttpPathFactory}.
	 * @throws HttpException
	 *             If required path parameters are not available on the values type.
	 */
	<T> HttpPathFactory<T> createHttpPathFactory(Class<T> valuesType) throws HttpException;

}