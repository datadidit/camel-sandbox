package org.datadidit.camel;

import java.io.IOException;
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
import org.datadidit.camel.util.TestUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.model.GeocodingResult;

/**
 * Camel Tests for GeoEnrichment Processor
 *
 */
public class GeoEnrichmentProcessorCamelIT extends CamelTestSupport{
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
	public void testProcessorIncomingJsonList() throws InterruptedException, IOException {
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
			List<Map<String,Object>> output = (List<Map<String, Object>>) exchange.getIn().getBody(); 
		
			for(Map<String, Object> entry : output) {
				//List<Map<String,Object>> resultAddress = TestUtils.convertToGeo((List<String>)entry.get("geometry"));
								
				for(Map.Entry<String, Object> geoEntry : entry.entrySet()) {
					System.out.println("Key: "+geoEntry.getKey()+" Value: "+geoEntry.getValue());
				}
			}
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
				GeoEnrichmentProcessor processor = new GeoEnrichmentProcessor(apiKey, "City,State,Country", "geometry");
				
				from("direct:start")
				 .process(processor)
				  .to(resultEndpoint);
			}
		};
	}
}
