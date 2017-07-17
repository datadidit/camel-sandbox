package org.datadidit.camel.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtils {
	private static ObjectMapper mapper = new ObjectMapper();
	
	public static List<Map<String, Object>> convertToGeo(List<String> geos) throws JsonParseException, JsonMappingException, IOException{
		List<Map<String, Object>> t = new ArrayList<>(); 
		Map<String, Object> bah = new HashMap<>();
		for(int i=0; i<geos.size(); i++) {
			 t.add(mapper.readValue(geos.get(i), bah.getClass()));
		}
		
		return t;
	}
}
