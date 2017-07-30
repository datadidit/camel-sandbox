package datadidit.helpful.hints.camel;

import javax.naming.ConfigurationException;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class CSVToJsonOutputIT extends CamelTestSupport{
	@Produce(uri="direct:start")
	private ProducerTemplate template; 
	
	private Integer entriesPerJson = 10;
	
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	@Test
	public void testMuliOutput() throws InterruptedException {
		template.sendBody(this.generateCSV(100));
		
		resultEndpoint.expectedMessageCount(10);
		
		resultEndpoint.assertIsSatisfied();
	}
	
	public String generateCSV(Integer lines) {
		StringBuilder build = new StringBuilder(); 
		
		//Add header
		build.append("name,state,year\n");
		
		for(int i=0; i<lines; i++) {
			build.append("name"+i+",state"+i+",year"+i+"\n");
		}
		
		return build.toString();
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			public void configure() throws Exception {
				CSVToJsonProcessor processor = new CSVToJsonProcessor(true, null, entriesPerJson);
				DefaultProducerTemplate template = DefaultProducerTemplate.newInstance(this.getContext(), "direct:toresult");
				template.start();
				
				processor.setProducer(template);
				
				from("direct:start")
					.process(processor);
				
				from("direct:toresult")
					.log("Message ${body}")
					.to(resultEndpoint);
			}
		};
	}
}
