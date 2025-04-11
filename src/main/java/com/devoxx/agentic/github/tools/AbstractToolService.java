package com.devoxx.agentic.github.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class AbstractToolService {

    protected static final String SUCCESS = "success";
    protected static final String ERROR = "error";

    protected final ObjectMapper mapper = new ObjectMapper();

    protected String errorMessage(String errorMessage) {
        Map<String, Object> result = new HashMap<>();
        result.put(SUCCESS, false);
        result.put(ERROR, errorMessage);
        try {
            return mapper.writeValueAsString(result);
        } catch (Exception ex) {
            return "{\"success\": false, \"error\": \"Failed to serialize error result\"}";
        }
    }

    protected String successMessage(Map<String, Object> result) {
        result.put(SUCCESS, true);
        try {
            return mapper.writeValueAsString(result);
        } catch (Exception ex) {
            return "{\"success\": false, \"error\": \"Failed to serialize success result\"}";
        }
    }
}
