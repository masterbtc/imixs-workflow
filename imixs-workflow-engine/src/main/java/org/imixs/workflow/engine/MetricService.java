/*******************************************************************************
 * <pre>
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
 *      http://www.imixs.org
 *      http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.workflow.engine;

import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObserverException;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The Imixs MetricSerivce is a monitoring resource for Imixs-Workflow in the prometheus format. The
 * MetricService is based on Microprofile 2.2 and MP-Metric-API 2.2
 * <p>
 * A metric is created each time when a Imixs ProcessingEvent or Imixs DocumentEvent is fired. The
 * service exports metrics in prometheus text format.
 * <p>
 * The service provides counter metrics for document access and processed workitems. A counter will
 * always increase. To extract the values in prometheus use the rate function - Example:
 * <p>
 * <code>rate(http_requests_total[5m])</code>
 * <p>
 * The service expects MP Metrics v2.0. A warning is logged if corresponding version is missing.
 * <p>
 * To enable the metric service the imixs.property ... must be set to true
 * </p>
 * 
 * @See https://www.robustperception.io/how-does-a-prometheus-counter-work
 * @author rsoika
 * @version 1.0
 */
@ApplicationScoped
public class MetricService {

  public static final String METRIC_DOCUMENTS_TOTAL = "documents_total";
  public static final String METRIC_WORKITEMS_TOTAL = "workitems_total";

  @Inject
  @ConfigProperty(name = "metrics.enabled", defaultValue = "false")
  private boolean metricsEnabled;

  @Inject
  @RegistryType(type = MetricRegistry.Type.APPLICATION)
  MetricRegistry metricRegistry;

  boolean mpMetricNoSupport = false;

  private static Logger logger = Logger.getLogger(MetricService.class.getName());

  /**
   * ProcessingEvent listener to generate a metric.
   * 
   * @param processingEvent
   * @throws AccessDeniedException
   */
  public void onProcessingEvent(@Observes ProcessingEvent processingEvent)
      throws AccessDeniedException {

    if (!metricsEnabled) {
      return;
    }
    if (processingEvent == null) {
      return;
    }
    if (mpMetricNoSupport) {
      // missing MP Metric support!
      return;
    }

    // NOTE: Issue #514 - just uncomment this code!
    try {
      Counter counter = buildWorkitemMetric(processingEvent);
      counter.inc();
    } catch (IncompatibleClassChangeError | ObserverException oe) {
      mpMetricNoSupport = true;
      logger.warning("...Microprofile Metrics v2.2 not supported!");
    }
  }

  /**
   * DocumentEvent listener to generate a metric.
   * 
   * @param documentEvent
   * @throws AccessDeniedException
   */
  public void onDocumentEvent(@Observes DocumentEvent documentEvent) throws AccessDeniedException {

    if (!metricsEnabled) {
      return;
    }
    if (documentEvent == null) {
      return;
    }
    if (mpMetricNoSupport) {
      // missing MP Metric support!
      return;
    }

    // NOTE: Issue #514 - just uncomment this code!
    try {
      Counter counter = buildDocumentMetric(documentEvent);
      counter.inc();

    } catch (IncompatibleClassChangeError | ObserverException oe) {
      mpMetricNoSupport = true;
      logger.warning("...Microprofile Metrics v2.0 not supported!");
      oe.printStackTrace();
    }
  }

  /**
   * This method builds a Microprofile Metric for a Counter. The metric contains the tag 'method'.
   * 
   * @return Counter metric
   */

  // NOTE: Issue #514 - just uncomment this code!

  private Counter buildDocumentMetric(DocumentEvent event) {

    // Constructs a Metadata object from a map with the following keys:
    // - name - The name of the metric
    // - displayName - The display (friendly) name of the metric
    // - description - The description of the metric
    // - type - The type of the metric
    // - tags - The tags of the metric - cannot be null
    // - reusable - If true, this metric name is permitted to be used at multiple

    Metadata metadata = Metadata.builder().withName(METRIC_DOCUMENTS_TOTAL)
        .withDescription("Imixs-Workflow count documents").withType(MetricType.COUNTER).build();

    String method = null;
    // build tags...
    if (DocumentEvent.ON_DOCUMENT_SAVE == event.getEventType()) {
      method = "save";
    }

    if (DocumentEvent.ON_DOCUMENT_LOAD == event.getEventType()) {
      method = "load";
    }

    if (DocumentEvent.ON_DOCUMENT_DELETE == event.getEventType()) {
      method = "delete";
    }

    Tag[] tags = {new Tag("method", method)};

    Counter counter = metricRegistry.counter(metadata, tags);

    return counter;
  }

  /**
   * This method builds a Microprofile Metric for a Counter. The metric contains the tags 'task',
   * 'event', 'type', 'workflowgroup', 'worklowstatus', 'modelversion'
   * 
   * @return Counter metric
   */
  private Counter buildWorkitemMetric(ProcessingEvent event) {

    // Constructs a Metadata object from a map with the following keys:
    // - name - The name of the metric
    // - displayName - The display (friendly) name of the metric
    // - description - The description of the metric
    // - type - The type of the metric
    // - tags - The tags of the metric - cannot be null
    // - reusable - If true, this metric name is permitted to be used at multiple

    Metadata metadata = Metadata.builder().withName(METRIC_WORKITEMS_TOTAL)
        .withDescription("Imixs-Workflow count procssed workitems").withType(MetricType.COUNTER)
        .build();

    // build tags...
    Tag[] tags = {new Tag("type", event.getDocument().getType()),
        new Tag("modelversion", event.getDocument().getModelVersion()),
        new Tag("task", event.getDocument().getTaskID() + ""),
        new Tag("event", event.getDocument().getItemValueInteger("$lastevent") + ""),
        new Tag("workflowgroup",
            event.getDocument().getItemValueString(WorkflowKernel.WORKFLOWGROUP)),
        new Tag("workflowstatus",
            event.getDocument().getItemValueString(WorkflowKernel.WORKFLOWSTATUS))};

    Counter counter = metricRegistry.counter(metadata, tags);

    return counter;
  }

}
