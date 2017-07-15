package datadidit.helpful.hints.camel;

import java.io.File;
import java.io.IOException;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Test;

public class CSVToJsonFlowIT extends CamelTestSupport{
	@EndpointInject(uri = "file://src/test/resources/camel/data?delete=true")
	protected FileEndpoint beginFileEndpoint;
	
	@EndpointInject(uri = "file://src/test/resources/camel/data-output")
	protected FileEndpoint endFileEndpoint;
	
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	@Test
	public void testSimple() throws InterruptedException {
		resultEndpoint.expectedMinimumMessageCount(1);
		
		resultEndpoint.assertIsSatisfied();
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			public void configure() {
				CSVToJsonProcessor processor = null;
				try {
					processor = new CSVToJsonProcessor(true, "");
					
					//Need to set default Endpoint
					DefaultProducerTemplate template = new DefaultProducerTemplate(this.getContext(), endFileEndpoint);
					template.start();
					processor.setProducer(template);
				} catch (Exception e) {
					e.printStackTrace();
					fail("Unable to create processor "+e.getMessage());
				}

				from(beginFileEndpoint)
				.process(processor)
				.log("Received Data!!!!")
				 .to(endFileEndpoint)
				 .to(resultEndpoint);
			}
		};
	}
	
	@AfterClass
	public static void cleanup() throws IOException {
		File outputDir = new File("src/test/resources/camel/data-output");
		
		for(File file : outputDir.listFiles()) {
			FileUtils.moveFileToDirectory(file, new File("src/test/resources/camel/data"), false);			
		}
		
		outputDir.delete();
	}
}
