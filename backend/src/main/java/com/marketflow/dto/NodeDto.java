package com.marketflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeDto {
    private String id;
    private String type;
    private PositionDto position;
    private Map<String, Object> data = new HashMap<>();

    public NodeDto() {}

    public NodeDto(String id, String type, PositionDto position, Map<String, Object> data) {
        this.id = id;
        this.type = type;
        this.position = position;
        if (data != null) {
            this.data = data;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PositionDto getPosition() {
        return position;
    }

    public void setPosition(PositionDto position) {
        this.position = position;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data != null ? data : new HashMap<>();
    }

    public String getLabel() {
        if (data != null && data.containsKey("label") && data.get("label") != null) {
            return data.get("label").toString();
        }
        return id;
    }

    public void setLabel(String label) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put("label", label);
    }
}
