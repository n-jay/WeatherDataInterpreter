package org.wso2.weather.data.interpreter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Set;

public class WeatherDataMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(WeatherDataMediator.class);
    private static final String URL_PREFIX =
            "http://localhost:8280/api/device-mgt/v1.0/device/agent/events/publish" +
                    "/weather-station/";

    private static final String TOKEN_EP = "http://localhost:8280/token";

    private ObjectMapper mapper = new ObjectMapper();


    public boolean mediate(MessageContext messageContext) {
        Set<String> keySet = messageContext.getPropertyKeySet();

        ///weatherstation/updateweatherstation.php?ID=station1&PASSWORD=fa937a96-b9c7-3d17-94c4-3a979c6&tempf=74.7
        // &humidity=63&dewptf=61.3&windchillf=74.7&winddir=282&windspeedmph=0.00&windgustmph=0.00
        // &rainin=0.00&dailyrainin=0.00&weeklyrainin=0.00&monthlyrainin=0.00
        // &yearlyrainin=0.00&solarradiation=0.00&UV=0&indoortempf=76.6&indoorhumidity=58
        // &baromin=29.92&lowbatt=0&dateutc=2017-10-17%205:7:3&softwaretype=WH2600GEN_V2.2.5&action=updateraw&realtime=1&rtfreq=5

        for(String key : keySet){
            log.info(key + " -> "+messageContext.getProperty(key));
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("temp-farenhite", messageContext.getProperty("query.param.tempf"));
        jsonObject.put("humidity", messageContext.getProperty("query.param.humidity"));
        jsonObject.put("pressure", messageContext.getProperty("query.param.baromin"));
        jsonObject.put("dewpt-farenhite", messageContext.getProperty("query.param.dewptf"));
        jsonObject.put("windchill-farenhite", messageContext.getProperty("query.param.windchillf"));
        jsonObject.put("winddir", messageContext.getProperty("query.param.winddir"));
        jsonObject.put("windspeedmph", messageContext.getProperty("query.param.windspeedmph"));
        jsonObject.put("windgustmph", messageContext.getProperty("query.param.windgustmph"));
        jsonObject.put("rainin", messageContext.getProperty("query.param.rainin"));
        jsonObject.put("dailyrainin", messageContext.getProperty("query.param.dailyrainin"));
        jsonObject.put("weeklyrainin", messageContext.getProperty("query.param.weeklyrainin"));
        jsonObject.put("monthlyrainin", messageContext.getProperty("query.param.monthlyrainin"));
        jsonObject.put("yearlyrainin", messageContext.getProperty("query.param.yearlyrainin"));
        jsonObject.put("solarradiation", messageContext.getProperty("query.param.solarradiation"));
        jsonObject.put("UV", messageContext.getProperty("query.param.UV"));
        jsonObject.put("indoortemp-farenhite", messageContext.getProperty("query.param.indoortempf"));
        jsonObject.put("indoorhumidity", messageContext.getProperty("query.param.indoorhumidity"));
        jsonObject.put("lowbatt", messageContext.getProperty("query.param.lowbatt"));
        jsonObject.put("dateutc", messageContext.getProperty("query.param.dateutc"));

        String payload = jsonObject.toJSONString();

        String password = messageContext.getProperty("query.param.PASSWORD").toString();
        String deviceID = messageContext.getProperty("query.param.ID").toString();
        String carbonHome = System.getProperty("carbon.home");

        //POST https://localhost:8243/api/device-mgt/v1.0/device/agent/events/publish/weather-station/station1
        // -H 'authorization: Bearer %accessToken%' -H 'content-type: application/json'
        // -d '{"temp-farenhite":"string","humidity":"string","pressure":"string","dewpt-farenhite":"string",
        // "windchill-farenhite":"string","winddir":"string","windspeedmph":"string","windgustmph":"string",
        // "rainin":"string","dailyrainin":"string","weeklyrainin":"string","monthlyrainin":"string",
        // "yearlyrainin":"string","solarradiation":"string","UV":"string","indoortemp-farenhite":"string",
        // "indoorhumidity":"string","lowbatt":"string","dateutc":"string"}'

        try {

            DeviceTokenMappings readMappings = mapper.readValue(new File(carbonHome +
                            "/repository/conf/device-key-mappings.json"),
                    DeviceTokenMappings.class);

            TokenInfo tokenInfo = readMappings.getTokenMappings().get(deviceID);

            if(tokenInfo == null){
                log.error("No keys found against device ID");
                return false;
            }

            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(URL_PREFIX + deviceID);
            httppost.addHeader("Authorization", "Bearer "+tokenInfo.getAccessToken());
            httppost.addHeader("Content-Type", "application/json");


            StringEntity stringEntity = new StringEntity(payload);
            httppost.getRequestLine();
            httppost.setEntity(stringEntity);
            HttpResponse response = httpclient.execute(httppost);

            if(response.getStatusLine().getStatusCode() == Response.Status.UNAUTHORIZED.getStatusCode()){

                //curl -k -d "grant_type=refresh_token&refresh_token=<retoken>"
                // -H "Authorization: Basic SVpzSWk2SERiQjVlOFZLZFpBblVpX2ZaM2Y4YTpHbTBiSjZvV1Y4ZkM1T1FMTGxDNmpzbEFDVzhh"
                // -H "Content-Type: application/x-www-form-urlencoded" https://localhost:8243/token

                //Make call to Token Endpoint and persist the new token info
                httppost = new HttpPost(TOKEN_EP);
                httppost.addHeader("Authorization", "Basic "+ new String(Base64.encodeBase64
                        ((tokenInfo.getClientID() + ":"+ tokenInfo.getClientSecre()).getBytes())));
                httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");

                StringEntity requestBody = new StringEntity
                        ("grant_type=refresh_token&refresh_token="+tokenInfo.getRefreshToken());
                httppost.getRequestLine();
                httppost.setEntity(requestBody);
                HttpResponse tokenResponse = httpclient.execute(httppost);

                JSONObject json = (JSONObject) JSONValue.parseWithException(tokenResponse
                        .getEntity().getContent());

                if((tokenResponse.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode())
                        &&  json != null) {
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    readMappings.getTokenMappings().get(deviceID).setAccessToken(json.get
                            ("access_token").toString());
                    readMappings.getTokenMappings().get(deviceID).setRefreshToken(json.get
                            ("refresh_token").toString());
                    mapper.writeValue(new File(carbonHome +
                            "/repository/conf/device-key-mappings.json"), readMappings);
                } else {
                    return false;
                }
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
