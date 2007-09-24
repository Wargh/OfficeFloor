/*
 *  Office Floor, Application Server
 *  Copyright (C) 2006 Daniel Sagenschneider
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms 
 *  of the GNU General Public License as published by the Free Software Foundation; either 
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program; 
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
 *  MA 02111-1307 USA
 */
package net.officefloor.model.generate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

import net.officefloor.model.generate.model.AbstractPropertyMetaData;
import net.officefloor.model.generate.model.FieldMetaData;
import net.officefloor.model.generate.model.ListMetaData;
import net.officefloor.model.generate.model.ModelMetaData;
import net.officefloor.repository.ConfigurationContext;
import net.officefloor.repository.ConfigurationItem;

/**
 * Generates the Model.
 * 
 * @author Daniel
 */
public class ModelGenerator {

	/**
	 * Meta-data of the Model.
	 */
	private final ModelMetaData metaData;

	/**
	 * Generic meta-data for creating the Model.
	 */
	private final GenericMetaData general;

	/**
	 * Writer to output the model.
	 */
	private PrintWriter writer;

	/**
	 * Initiate.
	 * 
	 * @param metaData
	 *            Meta-data of the Model.
	 * @param general
	 *            Generic meta-data for creating the Model.
	 */
	public ModelGenerator(ModelMetaData metaData, GenericMetaData general) {
		this.metaData = metaData;
		this.general = general;
	}

	/**
	 * Generates the Model.
	 * 
	 * @param context
	 *            Context to create the Model.
	 * @return {@link ConfigurationItem} for the Model.
	 * @throws Exception
	 *             If fail.
	 */
	public synchronized ConfigurationItem generateModel(
			ConfigurationContext context) throws Exception {

		// Create the writer to output contents
		ByteArrayOutputStream marshalledModel = new ByteArrayOutputStream();
		this.writer = new PrintWriter(new OutputStreamWriter(marshalledModel));

		// Write the model
		this.header();
		writeLine();
		this.imports();
		writeLine();
		this.classDefinition();

		// Flush the model
		this.writer.flush();

		// Create the configuration item
		return context.createConfigurationItem(this.metaData.getPackageName()
				.replace('.', '/')
				+ "/" + this.metaData.getClassName() + ".java",
				new ByteArrayInputStream(marshalledModel.toByteArray()));
	}

	/**
	 * Header.
	 */
	private void header() {
		writeLine("/*");
		writeLine(" * " + this.general.getLicense());
		writeLine(" */");
		writeLine("package " + this.metaData.getPackageName() + ";");
	}

	/**
	 * Imports.
	 */
	private void imports() {
		// Only import lists if have lists
		if (this.metaData.getLists().size() > 0) {
			writeLine("import java.util.List;");
			writeLine("import java.util.LinkedList;");
			writeLine();
		}
		// Write extra imports
		for (String importClass : this.metaData.getImportClasses()) {
			writeLine("import " + importClass + ";");
		}
		// Write necessary extension
		writeLine("import net.officefloor.model.AbstractModel;");
		if (this.metaData.isConnectionModel()) {
			writeLine("import net.officefloor.model.ConnectionModel;");
		}
	}

	/**
	 * Class definition.
	 */
	private void classDefinition() {
		// Class signature
		writeLine("public class "
				+ this.metaData.getClassName()
				+ this.metaData.getClassSuffix()
				+ " extends AbstractModel"
				+ (this.metaData.isConnectionModel() ? " implements ConnectionModel"
						: "") + " {");

		// Write class contents
		writeLine();
		this.events();
		writeLine();
		this.defaultConstructor();
		writeLine();
		this.convenienceConstructor();
		writeLine();
		this.convenienceXyConstructor();
		writeLine();
		this.fields();
		writeLine();
		this.lists();
		if (this.metaData.isConnectionModel()) {
			writeLine();
			this.connectionMethods();
		}

		// Close class
		writeLine("}");
	}

	/**
	 * Events.
	 */
	@SuppressWarnings("unchecked")
	private void events() {
		writeLine("    public static enum " + this.metaData.getEventName()
				+ " {");
		write("    ");
		writeListing(",", new WriteAction() {
			protected void writeField(FieldMetaData field) {
				write(" CHANGE_" + field.getCapitalisedName());
			}

			protected void writeList(ListMetaData list) {
				write(" ADD_" + list.getCapitalisedName() + ", REMOVE_"
						+ list.getCapitalisedName());
			}
		}, this.metaData.getFields(), this.metaData.getLists());
		writeLine();
		writeLine("    }");
	}

	/**
	 * Default constructor.
	 */
	private void defaultConstructor() {
		writeLine("    /**");
		writeLine("     * Default constructor.");
		writeLine("     */");
		writeLine("    public " + this.metaData.getClassName() + "() {");
		writeLine("    }");
	}

	/**
	 * Convenience constructor.
	 */
	@SuppressWarnings("unchecked")
	private void convenienceConstructor() {
		writeLine("    /**");
		writeLine("     * Convenience constructor.");
		writeLine("     */");
		writeLine("    public " + this.metaData.getClassName() + "(");

		// Parameters
		write("      ");
		writeListing("    , ", new WriteAction() {
			protected void writeField(FieldMetaData field) {
				writeLine(field.getType() + " " + field.getPropertyName());
			}

			protected void writeList(ListMetaData list) {
				writeLine(list.getType() + "[] " + list.getPropertyName());
			}
		}, this.metaData.getFields(), this.metaData.getLists());

		writeLine("    ) {");

		// Specify values
		writeListing("", new WriteAction() {
			protected void writeField(FieldMetaData field) {
				writeLine("        this." + field.getPropertyName() + " = "
						+ field.getPropertyName() + ";");
			}

			protected void writeList(ListMetaData list) {
				writeLine("        if (" + list.getPropertyName()
						+ " != null) {");
				writeLine("            for (" + list.getType() + " model : "
						+ list.getPropertyName() + ") {");
				writeLine("                this." + list.getPropertyName()
						+ ".add(model);");
				writeLine("            }");
				writeLine("        }");
			}
		}, this.metaData.getFields(), this.metaData.getLists());

		writeLine("    }");
	}

	/**
	 * Convenience constructor.
	 */
	@SuppressWarnings("unchecked")
	private void convenienceXyConstructor() {
		writeLine("    /**");
		writeLine("     * Convenience constructor allowing XY initialising.");
		writeLine("     */");
		writeLine("    public " + this.metaData.getClassName() + "(");

		// Parameters
		write("      ");
		writeListing("    , ", new WriteAction() {
			protected void writeField(FieldMetaData field) {
				writeLine(field.getType() + " " + field.getPropertyName());
			}

			protected void writeList(ListMetaData list) {
				writeLine(list.getType() + "[] " + list.getPropertyName());
			}
		}, this.metaData.getFields(), this.metaData.getLists());

		// X/Y parameters
		writeLine("    , int x");
		writeLine("    , int y");

		writeLine("    ) {");

		// Specify values
		writeListing("", new WriteAction() {
			protected void writeField(FieldMetaData field) {
				writeLine("        this." + field.getPropertyName() + " = "
						+ field.getPropertyName() + ";");
			}

			protected void writeList(ListMetaData list) {
				writeLine("        if (" + list.getPropertyName()
						+ " != null) {");
				writeLine("            for (" + list.getType() + " model : "
						+ list.getPropertyName() + ") {");
				writeLine("                this." + list.getPropertyName()
						+ ".add(model);");
				writeLine("            }");
				writeLine("        }");
			}
		}, this.metaData.getFields(), this.metaData.getLists());

		// Specify the X/Y values
		writeLine("        this.setX(x);");
		writeLine("        this.setY(y);");

		writeLine("    }");
	}

	/**
	 * Fields.
	 */
	@SuppressWarnings("unchecked")
	private void fields() {
		writeListing("", new WriteAction() {
			protected void writeField(FieldMetaData field) {
				// Description
				writeLine("    /**");
				writeLine("     * " + field.getDescription());
				writeLine("     */");

				// Variable
				writeLine("    private " + field.getType() + " "
						+ field.getPropertyName() + ";");
				writeLine();

				// Accessor
				writeLine("    public " + field.getType() + " get"
						+ field.getCamelCaseName() + "() {");
				writeLine("        return this." + field.getPropertyName()
						+ ";");
				writeLine("    }");
				writeLine();

				// Mutator
				writeLine("    public void set" + field.getCamelCaseName()
						+ "(" + field.getType() + " " + field.getPropertyName()
						+ ") {");
				writeLine("        " + field.getType() + " oldValue = this."
						+ field.getPropertyName() + ";");
				writeLine("        this." + field.getPropertyName() + " = "
						+ field.getPropertyName() + ";");
				writeLine("        this.changeField(oldValue, this."
						+ field.getPropertyName() + ", "
						+ ModelGenerator.this.metaData.getEventName()
						+ ".CHANGE_" + field.getCapitalisedName() + ");");
				writeLine("    }");
				writeLine();
			}
		}, this.metaData.getFields());
	}

	/**
	 * Lists.
	 */
	@SuppressWarnings("unchecked")
	private void lists() {
		writeListing("", new WriteAction() {
			protected void writeList(ListMetaData list) {
				// Description
				writeLine("    /**");
				writeLine("     * " + list.getDescription());
				writeLine("     */");

				// Variable
				writeLine("    private List<" + list.getType() + "> "
						+ list.getPropertyName() + " = new LinkedList<"
						+ list.getType() + ">();");
				writeLine();

				// Accessor
				writeLine("    public List<" + list.getType() + "> get"
						+ list.getPluralName() + "() {");
				writeLine("        return this." + list.getPropertyName() + ";");
				writeLine("    }");
				writeLine();

				// Add method
				writeLine("    public void add" + list.getCamelCaseName() + "("
						+ list.getType() + " " + list.getPropertyName() + ") {");
				writeLine("        this.addItemToList("
						+ list.getPropertyName() + ", this."
						+ list.getPropertyName() + ", "
						+ ModelGenerator.this.metaData.getEventName() + ".ADD_"
						+ list.getCapitalisedName() + ");");
				writeLine("    }");
				writeLine();

				// Remove method
				writeLine("    public void remove" + list.getCamelCaseName()
						+ "(" + list.getType() + " " + list.getPropertyName()
						+ ") {");
				writeLine("        this.removeItemFromList("
						+ list.getPropertyName() + ", this."
						+ list.getPropertyName() + ", "
						+ ModelGenerator.this.metaData.getEventName()
						+ ".REMOVE_" + list.getCapitalisedName() + ");");
				writeLine("    }");
				writeLine();
			}
		}, this.metaData.getLists());
	}

	/**
	 * Connection methods.
	 */
	@SuppressWarnings("unchecked")
	private void connectionMethods() {
		// Is remove
		writeLine("    /*");
		writeLine("     * ConnectionModel");
		writeLine("     */");
		writeLine("    public boolean isRemovable() {");
		writeLine("        return true;");
		writeLine("    }");
		writeLine();

		// Connect
		writeLine("    public void connect() {");
		writeListing("", new WriteAction() {
			protected void writeField(FieldMetaData field) {
				String endPointConnect = field.getEndPointConnect();
				if (endPointConnect != null) {
					writeLine("        this." + field.getPropertyName() + "."
							+ endPointConnect + ";");
				}
			}
		}, this.metaData.getFields());
		writeLine("    }");
		writeLine();

		// Remove
		writeLine("    public void remove() {");
		writeListing("", new WriteAction() {
			protected void writeField(FieldMetaData field) {
				String endPointRemove = field.getEndPointRemove();
				if (endPointRemove != null) {
					writeLine("        this." + field.getPropertyName() + "."
							+ endPointRemove + ";");
				}
			}
		}, this.metaData.getFields());
		writeLine("    }");
		writeLine();
	}

	/*
	 * ========================================================================
	 * Write methods
	 * ========================================================================
	 */

	/**
	 * Writes the listing of objects.
	 */
	private void writeListing(String separator, WriteAction action,
			List<? extends AbstractPropertyMetaData>... properties) {

		// Flag first
		boolean isFirst = true;

		// Write the listing
		for (List<? extends AbstractPropertyMetaData> propertyListing : properties) {
			// Write the properties
			for (AbstractPropertyMetaData property : propertyListing) {
				if (!isFirst) {
					write(separator);
				}
				action.writeProperty(property);
				isFirst = false;
			}
		}
	}

	/**
	 * Action to write the particular entry.
	 */
	private abstract class WriteAction {

		/**
		 * Override to write the property.
		 * 
		 * @param property
		 *            Property.
		 */
		protected void writeProperty(AbstractPropertyMetaData property) {
			if (property instanceof FieldMetaData) {
				writeField((FieldMetaData) property);
			} else if (property instanceof ListMetaData) {
				writeList((ListMetaData) property);
			} else {
				throw new UnsupportedOperationException("Unkown type "
						+ property.getClass().getName());
			}
		}

		/**
		 * Override to write the field.
		 * 
		 * @param field
		 *            Field.
		 */
		protected void writeField(FieldMetaData field) {
		}

		/**
		 * Override to write the list.
		 * 
		 * @param list
		 *            List.
		 */
		protected void writeList(ListMetaData list) {
		}
	}

	/**
	 * Moves to the next line.
	 */
	private void writeLine() {
		this.writer.println();
	}

	/**
	 * Writes the text followed by a end of line.
	 * 
	 * @param text
	 *            Text.
	 */
	private void writeLine(String text) {
		this.writer.println(text);
	}

	/**
	 * Writes the text only.
	 * 
	 * @param text
	 *            Text.
	 */
	private void write(String text) {
		this.writer.print(text);
	}

}
