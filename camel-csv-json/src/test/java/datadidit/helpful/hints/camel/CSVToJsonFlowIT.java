package datadidit.helpful.hints.camel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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
	private final String dataDir = "src/test/resources/camel/data";
	
	private final String outputDir = "src/test/resources/camel/data-output";

	@EndpointInject(uri = "file://"+dataDir)
	protected FileEndpoint beginFileEndpoint;
	
	@EndpointInject(uri = "file://"+outputDir)
	protected FileEndpoint endFileEndpoint;
	
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	@Test
	public void testSimple() throws InterruptedException, IOException {
		resultEndpoint.expectedMinimumMessageCount(1);
		
		resultEndpoint.assertIsSatisfied();
		
		//See Data 
		for(File file : new File(outputDir).listFiles()) {
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
		File camelDir = new File("src/test/resources/camel/data/.camel");
		
		for(File file : camelDir.listFiles()) {
			FileUtils.moveFileToDirectory(file, new File("src/test/resources/camel/data"), false);			
		}
		
		//Finish clean up by deleting unnecessary directories
		outputDir.delete();
		camelDir.delete();
	}
}
