package org.wso2.weather.data.interpreter;

import java.util.HashMap;
import java.util.Map;

public class DeviceTokenMappings {
    Map<String, TokenInfo> tokenMappings = new HashMap<String, TokenInfo>();

    public DeviceTokenMappings(){

    }

    public DeviceTokenMappings(Map<String, TokenInfo> tokenMappings){
        this.tokenMappings = tokenMappings;
    }

    public Map<String, TokenInfo> getTokenMappings() {
        return tokenMappings;
    }

    public void setTokenMappings(Map<String, TokenInfo> tokenMappings) {
        this.tokenMappings = tokenMappings;
    }
}
