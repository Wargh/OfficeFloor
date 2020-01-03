package net.officefloor.woof.model.woof;

import net.officefloor.model.change.Change;
import net.officefloor.woof.model.woof.WoofResourceModel;
import net.officefloor.woof.model.woof.WoofSectionInputModel;
import net.officefloor.woof.model.woof.WoofSecurityModel;
import net.officefloor.woof.model.woof.WoofTemplateModel;
import net.officefloor.woof.model.woof.WoofTemplateOutputModel;
import net.officefloor.woof.model.woof.WoofTemplateOutputToWoofResourceModel;
import net.officefloor.woof.model.woof.WoofTemplateOutputToWoofSectionInputModel;
import net.officefloor.woof.model.woof.WoofTemplateOutputToWoofSecurityModel;
import net.officefloor.woof.model.woof.WoofTemplateOutputToWoofTemplateModel;

/**
 * Tests linking from the {@link WoofTemplateOutputModel}.
 * 
 * @author Daniel Sagenschneider
 */
public class LinkTemplateOutputTest extends AbstractWoofChangesTestCase {

	/**
	 * Index of template A.
	 */
	private static final int A = 0;

	/**
	 * Index of template B.
	 */
	private static final int B = 1;

	/**
	 * Ensure can link to {@link WoofTemplateModel}.
	 */
	public void testLinkToTemplate() {
		this.doLinkToTemplate(A);
	}

	/**
	 * Ensure link overrides other links for
	 * {@link WoofTemplateOutputToWoofTemplateModel}.
	 */
	public void testLinkOverrideToTemplate() {
		this.doLinkToTemplate(B);
	}

	/**
	 * Undertakes linking to a {@link WoofTemplateModel}.
	 * 
	 * @param templateIndex {@link WoofTemplateModel} index.
	 */
	private void doLinkToTemplate(int templateIndex) {

		// Obtain the items to link
		WoofTemplateOutputModel templateOutput = this.model.getWoofTemplates().get(templateIndex).getOutputs().get(0);
		WoofTemplateModel template = this.model.getWoofTemplates().get(B);

		// Link the template output to template
		Change<WoofTemplateOutputToWoofTemplateModel> change = this.operations
				.linkTemplateOutputToTemplate(templateOutput, template);

		// Validate change
		this.assertChange(change, null, "Link Template Output to Template", true);
	}

	/**
	 * Ensure can remove the {@link WoofTemplateOutputToWoofTemplateModel}.
	 */
	public void testRemoveToTemplate() {

		// Obtain the link to remove
		WoofTemplateOutputToWoofTemplateModel link = this.model.getWoofTemplates().get(B).getOutputs().get(0)
				.getWoofTemplate();

		// Remove the link
		Change<WoofTemplateOutputToWoofTemplateModel> change = this.operations.removeTemplateOutputToTemplate(link);

		// Validate change
		this.assertChange(change, null, "Remove Template Output to Template", true);
	}

	/**
	 * Ensure can link to {@link WoofProcedureModel}.
	 */
	public void testLinkToProcedure() {
		this.doLinkToProcedure(A);
	}

	/**
	 * Ensure link overrides other links for
	 * {@link WoofTemplateOutputToWoofProcedureModel}.
	 */
	public void testLinkOverrideToProcedure() {
		this.doLinkToProcedure(B);
	}

	/**
	 * Undertakes linking to a {@link WoofProcedureModel}.
	 * 
	 * @param templateIndex {@link WoofTemplateModel} index.
	 */
	private void doLinkToProcedure(int templateIndex) {

		// Obtain the items to link
		WoofTemplateOutputModel templateOutput = this.model.getWoofTemplates().get(templateIndex).getOutputs().get(0);
		WoofProcedureModel procedure = this.model.getWoofProcedures().get(1);

		// Link the template output to procedure
		Change<WoofTemplateOutputToWoofProcedureModel> change = this.operations
				.linkTemplateOutputToProcedure(templateOutput, procedure);

		// Validate change
		this.assertChange(change, null, "Link Template Output to Procedure", true);
	}

	/**
	 * Ensure can remove the {@link WoofTemplateOutputToWoofProcedureModel}.
	 */
	public void testRemoveToProcedure() {

		// Obtain the link to remove
		WoofTemplateOutputToWoofProcedureModel link = this.model.getWoofTemplates().get(B).getOutputs().get(0)
				.getWoofProcedure();

		// Remove the link
		Change<WoofTemplateOutputToWoofProcedureModel> change = this.operations.removeTemplateOutputToProcedure(link);

		// Validate change
		this.assertChange(change, null, "Remove Template Output to Procedure", true);
	}

	/**
	 * Ensure can link to {@link WoofSectionInputModel}.
	 */
	public void testLinkToSectionInput() {
		this.doLinkToSectionInput(A);
	}

	/**
	 * Ensure link overrides other links for
	 * {@link WoofTemplateOutputToWoofSectionInputModel}.
	 */
	public void testLinkOverrideToSectionInput() {
		this.doLinkToSectionInput(B);
	}

	/**
	 * Undertakes linking to a {@link WoofSectionInputModel}.
	 * 
	 * @param templateIndex {@link WoofTemplateModel} index.
	 */
	private void doLinkToSectionInput(int templateIndex) {

		// Obtain the items to link
		WoofTemplateOutputModel templateOutput = this.model.getWoofTemplates().get(templateIndex).getOutputs().get(0);
		WoofSectionInputModel sectionInput = this.model.getWoofSections().get(1).getInputs().get(0);

		// Link the template output to section input
		Change<WoofTemplateOutputToWoofSectionInputModel> change = this.operations
				.linkTemplateOutputToSectionInput(templateOutput, sectionInput);

		// Validate change
		this.assertChange(change, null, "Link Template Output to Section Input", true);
	}

	/**
	 * Ensure can remove the {@link WoofTemplateOutputToWoofSectionInputModel}.
	 */
	public void testRemoveToSectionInput() {

		// Obtain the link to remove
		WoofTemplateOutputToWoofSectionInputModel link = this.model.getWoofTemplates().get(B).getOutputs().get(0)
				.getWoofSectionInput();

		// Remove the link
		Change<WoofTemplateOutputToWoofSectionInputModel> change = this.operations
				.removeTemplateOutputToSectionInput(link);

		// Validate change
		this.assertChange(change, null, "Remove Template Output to Section Input", true);
	}

	/**
	 * Ensure can link to {@link WoofSecurityModel}.
	 */
	public void testLinkToSecurity() {
		this.doLinkToSecurity(A);
	}

	/**
	 * Ensure link overrides other links for
	 * {@link WoofTemplateOutputToWoofSecurityModel}.
	 */
	public void testLinkOverrideToSecurity() {
		this.doLinkToSecurity(B);
	}

	/**
	 * Undertakes linking to a {@link WoofSecurityModel}.
	 * 
	 * @param templateIndex {@link WoofTemplateModel} index.
	 */
	private void doLinkToSecurity(int templateIndex) {

		// Obtain the items to link
		WoofTemplateOutputModel templateOutput = this.model.getWoofTemplates().get(templateIndex).getOutputs().get(0);
		WoofSecurityModel security = this.model.getWoofSecurities().get(1);

		// Link the template output to security
		Change<WoofTemplateOutputToWoofSecurityModel> change = this.operations
				.linkTemplateOutputToSecurity(templateOutput, security);

		// Validate change
		this.assertChange(change, null, "Link Template Output to Security", true);
	}

	/**
	 * Ensure can remove the {@link WoofTemplateOutputToWoofSecurityModel}.
	 */
	public void testRemoveToSecurity() {

		// Obtain the link to remove
		WoofTemplateOutputToWoofSecurityModel link = this.model.getWoofTemplates().get(B).getOutputs().get(0)
				.getWoofSecurity();

		// Remove the link
		Change<WoofTemplateOutputToWoofSecurityModel> change = this.operations.removeTemplateOutputToSecurity(link);

		// Validate change
		this.assertChange(change, null, "Remove Template Output to Security", true);
	}

	/**
	 * Ensure can link to {@link WoofResourceModel}.
	 */
	public void testLinkToResource() {
		this.doLinkToResource(A);
	}

	/**
	 * Ensure link overrides other links for
	 * {@link WoofTemplateOutputToWoofResourceModel}.
	 */
	public void testLinkOverrideToResource() {
		this.doLinkToResource(B);
	}

	/**
	 * Undertakes linking to a {@link WoofResourceModel}.
	 * 
	 * @param templateIndex {@link WoofTemplateModel} index.
	 */
	private void doLinkToResource(int templateIndex) {

		// Obtain the items to link
		WoofTemplateOutputModel templateOutput = this.model.getWoofTemplates().get(templateIndex).getOutputs().get(0);
		WoofResourceModel resource = this.model.getWoofResources().get(1);

		// Link the template output to resource
		Change<WoofTemplateOutputToWoofResourceModel> change = this.operations
				.linkTemplateOutputToResource(templateOutput, resource);

		// Validate change
		this.assertChange(change, null, "Link Template Output to Resource", true);
	}

	/**
	 * Ensure can remove the {@link WoofTemplateOutputToWoofResourceModel}.
	 */
	public void testRemoveToResource() {

		// Obtain the link to remove
		WoofTemplateOutputToWoofResourceModel link = this.model.getWoofTemplates().get(B).getOutputs().get(0)
				.getWoofResource();

		// Remove the link
		Change<WoofTemplateOutputToWoofResourceModel> change = this.operations.removeTemplateOutputToResource(link);

		// Validate change
		this.assertChange(change, null, "Remove Template Output to Resource", true);
	}

}