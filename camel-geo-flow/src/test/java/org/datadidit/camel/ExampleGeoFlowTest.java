package org.datadidit.camel;

import java.io.File;
import java.io.IOException;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Test;

public class ExampleGeoFlowTest extends CamelTestSupport{
	@EndpointInject(uri = "file://src/test/resources/data?delete=true")
	protected FileEndpoint beginFileEndpoint;
	
	@EndpointInject(uri = "file://src/test/resources/data-output")
	protected FileEndpoint endFileEndpoint;
	
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	@Test
	public void testFlow() throws InterruptedException {
		System.out.println("Hello World");
		resultEndpoint.expectedMinimumMessageCount(1);
		
		resultEndpoint.assertIsSatisfied();
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			public void configure() {
				from(beginFileEndpoint)
				.log("Received Data")
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
