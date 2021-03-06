/*-
 * #%L
 * JavaScript
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

package net.officefloor.javascript;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import net.officefloor.activity.procedure.spi.ProcedureSourceServiceFactory;
import net.officefloor.frame.api.source.SourceContext;
import net.officefloor.script.AbstractScriptProcedureSourceServiceFactory;
import net.officefloor.script.ScriptExceptionTranslator;
import net.officefloor.script.graalvm.GraalvmScriptExceptionTranslator;

/**
 * JavaScript function {@link ProcedureSourceServiceFactory}.
 * 
 * @author Daniel Sagenschneider
 */
public class JavaScriptProcedureSourceServiceFactory extends AbstractScriptProcedureSourceServiceFactory {

	@Override
	protected String getSourceName() {
		return "JavaScript";
	}

	@Override
	protected String[] getScriptFileExtensions(SourceContext context) throws Exception {
		return new String[] { "js" };
	}

	@Override
	protected String getScriptEngineName(SourceContext context) throws Exception {
		return "graal.js";
	}

	@Override
	protected void decorateScriptEngine(ScriptEngine engine, SourceContext context) throws Exception {
		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put("polyglot.js.allowAllAccess", true);
	}

	@Override
	protected String getMetaDataScriptPath(SourceContext context) throws Exception {
		return this.getClass().getPackage().getName().replace('.', '/') + "/OfficeFloorFunctionMetaData.js";
	}

	@Override
	protected ScriptExceptionTranslator getScriptExceptionTranslator() {
		return new GraalvmScriptExceptionTranslator();
	}

}
