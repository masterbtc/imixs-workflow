/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.faces.workitem;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;

/**
 * The WorkflowController can be used in JSF Applications to manage workflow
 * processing on any workitem.
 * 
 * The WorklfowController can be extended by a web module implementing a project
 * specific behavior.
 * 
 * The properties modelVersion and processID are used to control and create new
 * workitem instances based on a model definition
 * 
 * The workflowController bean is typically used in session scope.
 * 
 * @author rsoika
 * @version 2.0.0
 */
public class WorkflowController extends DocumentController {

	private static final long serialVersionUID = 1L;

	@EJB
	private ModelService modelService;

	@EJB
	private WorkflowService workflowService;

	private static Logger logger = Logger.getLogger(WorkflowController.class.getName());

	public WorkflowController() {
		super();
	}

	/**
	 * returns an instance of the ModelService EJB
	 * 
	 * @return
	 */
	public ModelService getModelService() {
		return modelService;
	}

	public WorkflowService getWorkflowService() {
		return workflowService;
	}

	/**
	 * This action method is used to initialize a new workitem with the inital
	 * values of the assigend workflow task. The method updates the Workflow
	 * attributes '$WriteAccess','txtworkflowgroup', 'txtworkflowStatus',
	 * 'txtWorkflowImageURL' and 'txtWorkflowEditorid'.
	 * 
	 * @param action
	 *            - the action returned by this method
	 * @return - action
	 * @throws ModelException
	 *             is thrown in case not valid worklfow task if defined by the
	 *             current model.
	 */
	public String init(String action) throws ModelException {

		if (workitem == null) {
			return action;
		}
		ItemCollection startProcessEntity = null;
		// if not process id was set fetch the first start workitem
		if (workitem.getItemValueInteger(WorkflowKernel.PROCESSID) <= 0) {
			// get ProcessEntities by version
			List<ItemCollection> col;
			col = modelService.getModelByWorkitem(getWorkitem()).findAllTasks();
			if (!col.isEmpty()) {
				startProcessEntity = col.iterator().next();
				getWorkitem().replaceItemValue(WorkflowKernel.PROCESSID,
						startProcessEntity.getItemValueInteger("numProcessID"));
			}
		}

		// find the ProcessEntity
		startProcessEntity = modelService.getModelByWorkitem(workitem)
				.getTask(workitem.getItemValueInteger(WorkflowKernel.PROCESSID));

		// ProcessEntity found?
		if (startProcessEntity == null)
			throw new InvalidAccessException(ModelException.INVALID_MODEL_ENTRY,
					"unable to find ProcessEntity in model version "
							+ workitem.getItemValueString(WorkflowKernel.MODELVERSION) + " for ID="
							+ workitem.getItemValueInteger(WorkflowKernel.PROCESSID));

		// update processId and WriteAccess
		workitem.replaceItemValue("$WriteAccess", workitem.getItemValue("namCreator"));

		// assign WorkflowGroup and editor
		workitem.replaceItemValue("txtworkflowgroup", startProcessEntity.getItemValueString("txtworkflowgroup"));
		workitem.replaceItemValue("txtworkflowStatus", startProcessEntity.getItemValueString("txtname"));
		workitem.replaceItemValue("txtWorkflowImageURL", startProcessEntity.getItemValueString("txtimageurl"));
		workitem.replaceItemValue("txtWorkflowEditorid", startProcessEntity.getItemValueString("txteditorid"));

		return action;
	}

	/**
	 * This method processes the current workItem and returns an action result.
	 * The method expects that the current workItem provides a valid
	 * $ActiviytID.
	 * 
	 * The method returns the value of the property 'action' if provided by the
	 * workflow model or a plug-in. The 'action' property is typically evaluated
	 * from the ResultPlugin. Alternatively the property can be provided by an
	 * application. If no 'action' property is provided the method evaluates the
	 * default property 'txtworkflowResultmessage' from the model as an action
	 * result.
	 * 
	 * The method resets the current ActivityList after the workitem was
	 * processed.
	 * 
	 * @return the action result provided in the 'action' property or evaluated
	 *         from the default property 'txtworkflowResultmessage' from the
	 *         ActivityEntity
	 * @throws AccessDeniedException
	 * @throws PluginException
	 */
	public String process() throws AccessDeniedException, ProcessingErrorException, PluginException {
		if (workitem == null) {
			logger.warning("Unable to process workitem == null!");
			return null;
		}

		// clear last action
		workitem.replaceItemValue("action", "");

		// process workItem now...
		workitem = this.getWorkflowService().processWorkItem(workitem);

		// test if the property 'action' is provided
		String action = workitem.getItemValueString("action");
		if ("".equals(action))
			// get default workflowResult message
			action = workitem.getItemValueString("txtworkflowresultmessage");
		return ("".equals(action) ? null : action);
	}

	/**
	 * This method processes the current workItem with the provided activityID.
	 * The method can be used as an actionListener.
	 * 
	 * @param id
	 *            - activityID to be processed
	 * @param resetWorkitem
	 *            - boolean indicates if the workitem should be reset
	 * @throws PluginException
	 * @see process()
	 */
	public String process(int id, boolean resetWorkitem)
			throws AccessDeniedException, ProcessingErrorException, PluginException {
		// update the property $ActivityID
		this.getWorkitem().replaceItemValue("$ActivityID", id);
		String result = process();

		return result;
	}

	/**
	 * This method processes the current workItem with the provided activityID.
	 * The meethod can be used as an actionListener.
	 * 
	 * @param id
	 *            - activityID to be processed
	 * @throws PluginException
	 * 
	 * @see process()
	 * @see process(id,resetWorkitem)
	 */
	public String process(int id) throws AccessDeniedException, ProcessingErrorException, PluginException {
		return process(id, false);
	}

	/**
	 * This method returns a List of workflow events assigned to the
	 * corresponding '$processid' and '$modelversion' of the current WorkItem.
	 * 
	 * @return
	 */
	public List<ItemCollection> getEvents() {
		List<ItemCollection> activityList = new ArrayList<ItemCollection>();

		if (getWorkitem() == null)
			return activityList;

		// get Events form workflowService
		try {
			activityList = this.getWorkflowService().getEvents(getWorkitem());
		} catch (ModelException e) {
			logger.warning("Unable to get workflow event list: " + e.getMessage());
		}

		return activityList;
	}


}