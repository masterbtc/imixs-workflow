package org.imixs.workflow.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ejb.AbstractWorkflowEnvironment;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test the Owner plug-in concerning the settings in a process entity.
 * 
 * If Owner settings are provided by the next process entity than these settings
 * should be set per default and the activity entity can provide additional
 * setting.
 * 
 * 
 * e.g.
 * 
 * ProcessEntity 'namaddwriteaccess' = 'jo'
 * 
 * ActivityEntity 'namaddwriteaccess' = 'anna'
 * 
 * 
 * then namowner should be 'jo','anna'
 * 
 * 
 * 
 * These tests extend the JUnit test in TestAccessPlugin
 * 
 * 
 * @author rsoika
 * 
 */
public class TestOwnerPluginProcessEntity extends AbstractWorkflowEnvironment {

	private final static Logger logger = Logger.getLogger(TestOwnerPluginProcessEntity.class.getName());

	OwnerPlugin ownerPlugin = null;
	ItemCollection documentContext;
	ItemCollection documentActivity, documentProcess;

	@Before
	public void setup() throws PluginException {

		this.setModelPath("/bpmn/acl-test.bpmn");
		super.setup();

		ownerPlugin = new OwnerPlugin();
		try {
			ownerPlugin.init(workflowContext);
		} catch (PluginException e) {

			e.printStackTrace();
		}

		// prepare data
		documentContext = new ItemCollection();
		logger.info("[TestOwnerPlugin] setup test data...");
		Vector<String> list = new Vector<String>();
		list.add("manfred");
		list.add("anna");
		documentContext.replaceItemValue("namTeam", list);

		documentContext.replaceItemValue("namCreator", "ronny");

	}

	/**
	 * Test if the Owner settings will not be changed if no ACL is set be
	 * process or activity
	 * 
	 * @throws ModelException
	 ***/
	@Test
	public void testOwnerNoUpdate() throws ModelException {
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		documentContext.replaceItemValue("namowner", list);
		documentContext.replaceItemValue("$processid", 100);

		documentActivity = this.getModel().getEvent(100, 10);
		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		@SuppressWarnings("unchecked")
		List<String> ownerList = documentContext.getItemValue("namowner");

		Assert.assertEquals(2, ownerList.size());
		Assert.assertTrue(ownerList.contains("Kevin"));
		Assert.assertTrue(ownerList.contains("Julian"));

	}

	/**
	 * Test if the Owner settings from the activityEntity are injected into the
	 * workitem
	 * 
	 * @throws ModelException
	 **/
	@Test
	public void testOwnerfromActivityEntity() throws ModelException {

		documentActivity = this.getModel().getEvent(100, 20);
		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		@SuppressWarnings("unchecked")
		List<String> onwerList = documentContext.getItemValue("namOwner");

		Assert.assertEquals(3, onwerList.size());
		Assert.assertTrue(onwerList.contains("joe"));
		Assert.assertTrue(onwerList.contains("manfred"));
		Assert.assertTrue(onwerList.contains("anna"));

	}

	/**
	 * Test if the ACL settings from the next processEntity are injected into
	 * the workitem
	 * 
	 * @throws ModelException
	 **/
	@Test
	public void testOwnerfromProcessEntity() throws ModelException {

		documentActivity = this.getModel().getEvent(300, 10);
		documentContext.replaceItemValue("$processid", 300);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		@SuppressWarnings("unchecked")
		List<String> ownerList = documentContext.getItemValue("namowneR");

		Assert.assertEquals(2, ownerList.size());
		Assert.assertTrue(ownerList.contains("joe"));
		Assert.assertTrue(ownerList.contains("sam"));

	}

	/**
	 * Test if the Owner settings from the next processEntity are ignored in
	 * case the ActivityEnttiy provides settings. Merge is not supported!
	 * 
	 * @throws ModelException
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@Test
	public void testOwnerfromProcessEntityAndActivityEntity() throws ModelException {

		// set some old values
		Vector<String> list = new Vector<String>();
		list.add("Kevin");
		list.add("Julian");
		documentContext.replaceItemValue("namOwner", list);
		documentContext.replaceItemValue("$processid", 300);

		documentActivity = this.getModel().getEvent(300, 20);

		try {
			ownerPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		// $writeAccess= anna , manfred, joe, sam
		List<String> onwerList = documentContext.getItemValue("namOwner");
		Assert.assertEquals(3, onwerList.size());
		Assert.assertTrue(onwerList.contains("joe"));
		// Assert.assertTrue(onwerList.contains("sam"));
		Assert.assertTrue(onwerList.contains("manfred"));
		Assert.assertTrue(onwerList.contains("anna"));

	}

}
