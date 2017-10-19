package org.wso2.weather.data.interpreter;

public class TokenInfo {
    String accessToken;
    String refreshToken;
    String clientID;
    String clientSecre;

    public TokenInfo(){

    }

    public TokenInfo (String accessToken, String refreshToken, String clientID, String clientSecre){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.clientID = clientID;
        this.clientSecre = clientSecre;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getClientSecre() {
        return clientSecre;
    }

    public void setClientSecre(String clientSecre) {
        this.clientSecre = clientSecre;
    }
}