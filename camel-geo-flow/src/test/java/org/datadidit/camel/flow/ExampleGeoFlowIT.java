package org.datadidit.camel.flow;

import java.io.File;
import java.io.IOException;

import javax.naming.ConfigurationException;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.component.file.FileEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import datadidit.helpful.hints.camel.CSVToJsonProcessor;

public class ExampleGeoFlowIT extends CamelTestSupport{
	@EndpointInject(uri = "file://src/test/resources/data?delete=true")
	protected FileEndpoint beginFileEndpoint;
	
	@EndpointInject(uri = "file://src/test/resources/data-output")
	protected FileEndpoint endFileEndpoint;
		
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	@EndpointInject(uri = "direct:geoenhance")
	protected DirectEndpoint directToGeoEndpoint;
	
	@Test
	public void testFlow() throws InterruptedException, IOException {
		resultEndpoint.expectedMinimumMessageCount(1);
		
		File output = new File("src/test/resources/data-output");
		
		resultEndpoint.assertIsSatisfied();
		for(File file : output.listFiles()) {
			System.out.println(FileUtils.readFileToString(file));
		}
		
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			public void configure() {
				CSVToJsonProcessor processor = null;
				try {
					processor = new CSVToJsonProcessor(true, "");
					
					//Need to set default Endpoint
					DefaultProducerTemplate template = new DefaultProducerTemplate(this.getContext(), directToGeoEndpoint);
					template.start();
					processor.setProducer(template);
				} catch (Exception e) {
					e.printStackTrace();
					fail("Unable to create processor "+e.getMessage());
				}
				
				/*
				 * Turn CSV to JSON
				 */
				from(beginFileEndpoint)
				.process(processor)
				.log("Received Data ");
				
				/*
				 * Geo Enhance
				 */
				from(directToGeoEndpoint)
				 .to(endFileEndpoint)
				 .to(resultEndpoint);
			}
		};
	}
	
	@AfterClass
	public static void cleanup() throws IOException {
		File outputDir = new File("src/test/resources/data-output");
		
		for(File file : outputDir.listFiles()) {
			FileUtils.moveFileToDirectory(file, new File("src/test/resources/data"), false);			
		}
		
		outputDir.delete();
	}
}
