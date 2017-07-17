package org.datadidit.camel.flow;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.component.file.FileEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.datadidit.camel.GeoEnrichmentProcessor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.model.GeocodingResult;

import datadidit.helpful.hints.camel.CSVToJsonProcessor;

public class ExampleGeoFlowIT extends CamelTestSupport{
	@EndpointInject(uri = "file://src/test/resources/data")
	protected FileEndpoint beginFileEndpoint;
		
	@EndpointInject(uri = "mock:csvresult")
	protected MockEndpoint csvResultEndpoint;
	
	@EndpointInject(uri = "mock:georesult")
	protected MockEndpoint geoResultEndpoint;
	
	@EndpointInject(uri = "direct:geoenhance")
	protected DirectEndpoint directToGeoEndpoint;
	
	private static String apiKey; 
	
	@BeforeClass
	public static void setupGeo(){
		apiKey = System.getenv("apiKey");
		if(apiKey==null)
			fail("Unable to get Api Key set up environment variable for this test!");
		else
			System.out.println("API Key is "+apiKey);
	}
	
	@Test
	public void testFlow() throws InterruptedException, IOException {
		this.context().getShutdownStrategy().setTimeout(60);
		csvResultEndpoint.expectedMinimumMessageCount(1);
		geoResultEndpoint.expectedMessageCount(1);
		
		csvResultEndpoint.assertIsSatisfied();
		geoResultEndpoint.assertIsSatisfied();
		
		
		List<Exchange> exchanges = geoResultEndpoint.getExchanges();
		
		for(Exchange exchange : exchanges) {
			/*
			 * Analyze exchange
			 */
			ObjectMapper mapper = new ObjectMapper(); 
			List<Map<String,Object>> input = (List<Map<String, Object>>) exchange.getIn().getBody(); 
			
			for(Map<String, Object> entry : input){
				//Each should have a geomentry key
				assertTrue(entry.containsKey("geometry"));
				
				//Each should have a geo entry
				GeocodingResult[] geoArray = (GeocodingResult[]) entry.get("geometry");
				assertTrue("Should be atleast 1 geo entry ", geoArray.length>=1);
				
				GeocodingResult[] resultAddress = (GeocodingResult[]) entry.get("geometry");

				System.out.println(resultAddress[0].formattedAddress);
				System.out.println(resultAddress[0].geometry.location);		
				System.out.println(mapper.writeValueAsString(entry));
			}
		}
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			public void configure() {
				CSVToJsonProcessor csvProcessor = null;
				GeoEnrichmentProcessor geoProcessor = null;
				
				try {
					csvProcessor = new CSVToJsonProcessor(true, null);
					
					String geoKey = "geometry";
					geoProcessor = new GeoEnrichmentProcessor(apiKey, "Birthplace,State,Country", geoKey);
					
					//Need to set default Endpoint
					DefaultProducerTemplate template = new DefaultProducerTemplate(this.getContext(), directToGeoEndpoint);
					template.start();
					csvProcessor.setProducer(template);
				} catch (Exception e) {
					e.printStackTrace();
					fail("Unable to create processor "+e.getMessage());
				}
				
				/*
				 * Turn CSV to JSON
				 */
				from(beginFileEndpoint)
				.process(csvProcessor)
				.to(csvResultEndpoint);
				
				/*
				 * Geo Enhance
				 */
				from(directToGeoEndpoint)
				.process(geoProcessor)
				.to(geoResultEndpoint);
			}
		};
	}
	
	@AfterClass
	public static void cleanup() throws IOException {
		File outputDir = new File("src/test/resources/data/.camel");
		
		for(File file : outputDir.listFiles()) {
			FileUtils.moveFileToDirectory(file, new File("src/test/resources/data"), false);			
		}
		
		outputDir.delete();
	}
}
