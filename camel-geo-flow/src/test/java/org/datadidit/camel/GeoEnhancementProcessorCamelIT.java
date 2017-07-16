package org.datadidit.camel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GeoEnhancementProcessorCamelIT extends CamelTestSupport{
    @Produce(uri = "direct:start")
    protected ProducerTemplate template;
	
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	private ObjectMapper mapper = new ObjectMapper(); 
	
	@Test
	public void testProcessorIncomingJsonMap() throws JsonProcessingException, InterruptedException {
		/*
		 * Create input 
		 */
		Map<String, Object> geoInfo = new HashMap<>();
		
		geoInfo.put("State", "MD");
		geoInfo.put("City", "Ocean City");
		geoInfo.put("Country", "US");
		
		String input = mapper.writeValueAsString(geoInfo);
		resultEndpoint.expectedMinimumMessageCount(1);
		
		template.sendBody(input);
			
		resultEndpoint.assertIsSatisfied();
		List<Exchange> exchanges = resultEndpoint.getExchanges();
		
		for(Exchange exchange : exchanges) {
			System.out.println("Outgoing "+exchange.getIn().getBody());
		}
	}
	
	@Test
	public void testProcessorIncomingJsonList() throws JsonProcessingException, InterruptedException {
		String[] mdLocations = new String[] {"Forestville", "Largo", "Poolesville", "Annapolis"};
		List<Map<String, Object>> incomingData = new ArrayList<>();
		
		for(String city : mdLocations) {
			Map<String, Object> geoInfo = new HashMap<>();

			geoInfo.put("State", "MD");
			geoInfo.put("City", city);
			geoInfo.put("Country", "US");
			
			incomingData.add(geoInfo);
		}
		
		String input = mapper.writeValueAsString(incomingData);
		
		resultEndpoint.expectedMinimumMessageCount(1);
		
		template.sendBody(input);
			
		resultEndpoint.assertIsSatisfied();
		List<Exchange> exchanges = resultEndpoint.getExchanges();
		
		for(Exchange exchange : exchanges) {
			System.out.println("Outgoing "+exchange.getIn().getBody());
		}
	}
	
	/*
	 * Create Camel Route for Processor 
	 */
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			public void configure() {
				String apiKey = System.getenv("apiKey");
				GeoEnhancementProcessor processor = new GeoEnhancementProcessor(apiKey, "City,State,Country", "geometry");
				
				from("direct:start")
				 .process(processor)
				  .to(resultEndpoint);
			}
		};
	}
}
