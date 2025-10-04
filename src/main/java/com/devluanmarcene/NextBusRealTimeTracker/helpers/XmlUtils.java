package com.devluanmarcene.NextBusRealTimeTracker.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class XmlUtils {

    private static final XmlMapper XML_MAPPER = XmlMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES, true)
            .build();

    static {
        XML_MAPPER.registerModule(new ParameterNamesModule());
    }

    /**
     * Map a string containing a xml object to the class received as a parameter
     * 
     * @param <T>
     * @param xmlString
     * @param desiredClass
     * @return <T>.class
     */
    public static <T> T doXmlMapping(String xmlString, Class<T> desiredClass) {
        try {
            return XML_MAPPER.readValue(xmlString, desiredClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
