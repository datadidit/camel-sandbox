package org.datadidit.camel;

import java.io.File;
import java.io.IOException;

import javax.naming.ConfigurationException;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
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
	
	private static CSVToJsonProcessor csvProcessor;
	
	@BeforeClass
	public static void setupProcessors() {
		try {
			csvProcessor = new CSVToJsonProcessor(true, "");
		} catch (ConfigurationException e) {
			e.printStackTrace();
			fail("Unable to run test failed to initialize processor "+e.getMessage());
		}
	}
	
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
				from(beginFileEndpoint)
				.process(csvProcessor)
				.log("Received Data ")
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