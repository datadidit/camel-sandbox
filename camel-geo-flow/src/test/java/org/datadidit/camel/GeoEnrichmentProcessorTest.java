package org.datadidit.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
@Ignore
public class GeoEnrichmentProcessorTest {
	private static ObjectMapper mapper; 
	
	private GeoEnrichmentProcessor processor;
	
	@BeforeClass 
	public static void setup(){
		mapper = new ObjectMapper(); 
	}
	
	@Test
	public void testStringToJsonMap() throws JsonProcessingException, ConfigurationException {
		processor = new GeoEnrichmentProcessor(null, null, "geometry");

		Map<String, Object> test = new HashMap<>();
		
		test.put("hello", "world");
		test.put("foo", "bar");
		test.put("age", 2);
		
		String json = mapper.writeValueAsString(test);
	
		try {
			Object converted = processor.stringToJson(IOUtils.toInputStream(json, "UTF-8"));
			
			//TODO: Not sure if this comparisson is correct. 
			assertEquals(test, converted);
		} catch (IOException e) {
			fail("Failed to get JSON object from Map "+e.getMessage());
		}
	}
	
	@Test
	public void testStringToList() throws JsonProcessingException, ConfigurationException {
		processor = new GeoEnrichmentProcessor(null, null, "geometry");
		List<Map<String,Object>> testList = new ArrayList<>();
			
		for(int i=0; i<10; i++) {
			Map<String, Object> test = new HashMap<>();
			
			test.put("hello", "world");
			test.put("foo", "bar");
			test.put("age", i);
			
			testList.add(test);
		}
		
		String json = mapper.writeValueAsString(testList);
		System.out.println(json);
		try {
			Object converted = processor.stringToJson(IOUtils.toInputStream(json, "UTF-8"));
			
			//TODO: Not sure if this comparisson is correct. 
			assertEquals(testList, converted);
		} catch (IOException e) {
			fail("Failed to get JSON object from Map "+e.getMessage());
		}
	}
}
