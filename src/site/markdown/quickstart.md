# Quickstart

In the following section you will see how **Imixs-Workflow** works and how you can use the engine efficiently for your business application.

Imixs-Workflow is an **open source workflow engine** for a human-centric business process management (BPM). Human-centric BPM means to support human skills and activities in a task oriented and event driven way. To start with Imixs-Workflow you first create a workflow-model. A model describes your business logic and it can be easily changed during runtime - without changing one line of code.

Imixs-Workflow is based on the [BPMN 2.0 modeling standard](http://www.bpmn.org/) and a Imixs-Workflow model can be created with the modeling tool [Imixs-BPMN](./modelling/). 
 
Let's take look at a simple example of a business process of an ordering process:


<img src="./images/modelling/order-01.png" />


This workflow model has several tasks and events to describe the ordering process. A **Task** element is used to describe the status of the current process and the **Event** elements are used to describe a transition from one state to another. The Events can be triggered by the user within your application. The example model can be download from [Github](https://github.com/imixs/imixs-workflow/tree/master/src/site/resources/bpmn). 

Now lets see how you can use this model within your own application. 

## How to Use Imixs-Workflow

The Imixs-Workflow engine can be run in different ways. You can run it as a [Microservice](https://github.com/imixs/imixs-microservice) and interact with the engine via the [Imixs-Rest API](/restapi/index.html). You can build a web application like the [MVC Example](https://github.com/imixs/imixs-mvc-example). Or you can use the engine in the embedded mode within your Jakarta EE application. In the following we use the last variant to keep the code examples small.

To start a new order process with the embedded mode all you have to do is to inject the WorkflowService EJB, create a new business object and assign it to your model:


	import org.imixs.workflow.ItemCollection;
	.....
	@EJB
	private org.imixs.workflow.engine.WorkflowService workflowService;
	....
	// create a new workitem assigned to the order workflow model
	ItemCollection workitem=new ItemCollection().model("1.0.0").task(1000).event(10);
	// assign some business data...
	workitem.replaceItemValue("_customer","M. Melman");
	workitem.replaceItemValue("_ordernumber",20051234);
	// process the workitem
	workitem = workflowService.processWorkItem(workitem);


From now on the newly created process instance is fully under the control of your business model. As you can see you can assign custom values from your application to the process instance. This is useful to allow the workflow engine to work with this data.



## Why Should You Use It?

So the first question so far is - why should you use a workflow engine? 
Surely you are also able to implement an order management with a database and the corresponding CRUD operations. But the reason why you should use a workflow engine comes from a different direction. The workflow engine generates a lot of useful business information behind the scene. 

### Status Information
The process instance returned by the method call '_processWorkItem()_' contains several useful new item values. For example to get the order status based on our workflow model we call:

	String status=workitem.getItemValueString(WorkflowKernel.WORKFLOWSTATUS);

another useful item is the so called '_workflow group_', in our example 'Order' it can be asked by: 

	String group=workitem.getItemValueString(WorkflowKernel.WORKFLOWGROUP);
	
### Processing Information
	
Beside this general status information we also get information about who has created the process instance and who was the last editor: 

	String creator=workitem.getItemValueString(WorkflowKernel.CREATOR);
	String editor=workitem.getItemValueString(WorkflowKernel.EDITOR);

And of course you can also check the time points: 

	Date created==workitem.getItemValueDate(WorkflowKernel.CREATED);
	Date modified==workitem.getItemValueDate(WorkflowKernel.MODIFIED);

A complete list of all processing information generated by the workflow engine can be found [here](./workitem.html). 

### Responsibilities and Access 
	
But the more interesting part is who is responsible for the ordering process? You can ask this by checking the owner list of the process instance:

	List<String> owners=workitem.getItemValue("namowner");

The Imixs-Workflow engine in addition adds a custom [Access Control List (ACL)](https://www.imixs.org/doc/engine/acl.html) to each process instance. This ensures that only authorized persons can change the process instance. 	


	List<String> authors=workitem.getItemValue(WorkflowKernel.WRITEACCESS);
	List<String> readers=workitem.getItemValue(WorkflowKernel.READACCESS);	

The owners and ACL settings are part of the modeling process and can be set individually for each task or event:

<img src="./images/bpmn-example02.png" width="500px" />

A shortcut to ask if the current workitem is editable by the current user is:

	boolean editable=workitem.getItemValueBoolean(WorkflowKernel.ISAUTHOR);



## Model Driven Business Logic

All information we got so far from the process instance, controlled by the Imixs-Workflow engine, are based on our workflow model. So you can now easily change the business logic by changing the model. Each time you process the workitem it will be automatically updated with all informations defined by your model. 


Now to control the order item we created in your first example you can assigning the process instance with one of the events defined by your model. You can ask the workflow engine which events are currently available for your process instance:

	List<ItemCollection> events=workflowService.getEvents(workitem);

This method ensures that only events are returned which were previously defined in the model. (Apart from that, an event can also be assigned with an ACL to model highly dynamic process flows.)

The list of events also holds information about the model (e.g. the name or the description of an event). 
If you have an event, you can process the workitem and transfer the order into a new state:

	// process the workitem with a new event
	workitem = workflowService.processWorkItem(workitem, event);


Models can also contain complex business rules to describe how a process should behave under certain circumstances.

<img src="./images/modelling/example_06.png"/>

You can find more information about in the section ['How to Model'](./modelling/howto.html). 

## The Processing History 

During the processing the Imixs-Workflow engine not only add business logic. The Engine also logs all processing steps so that a contiguous log is created. This log can be accessed by process instance and can be displayed in an application:

<img src="./images/modelling/order-02.png" width="500px" />

This Processing History was generated by the [HistoryPlugin(./engine/plugins/historyplugin.html). Plugins are an excellent way to adapt the behavior of the workflow engine to your own needs. Learn more about this interface in section [Plugin API](./engine/plugins/index.html). 

# What's next?

Additional information about how to use the Imixs-Workflow engine can be found here:

 * [How to manage business data](./quickstart/workitem.html)
 * [The Business Process](./quickstart/businessprocess.html)
 * [The Imixs Workflow Engine](./quickstart/workflowengine.html)
 * [The Imixs-BPMN Modeler - User Guide](./modelling/index.html)
 * [The Plugin API](./engine/plugins/index.html)
 
If you have any questions about how Imixs-Worklfow works and how you can use it in your own project, ask your question on the [GitHub Issue Tracker](https://github.com/imixs/imixs-workflow/issues). If you have general questions and your are not sure where to put it, use the [discussion forum on gitter](https://gitter.im/imixs/imixs-workflow). 