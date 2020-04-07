package io.jenkins.plugins;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;


public class BitbucketPayloadProcessor {
    private final BitbucketJobProbe probe;

    public BitbucketPayloadProcessor(BitbucketJobProbe probe) {
        this.probe = probe;
    }

    public BitbucketPayloadProcessor() {
        this(new BitbucketJobProbe());
    }
    public void processPayload(JSONObject payload, HttpServletRequest request) {
        HashMap<String, String> requestHeaders =  getRequestHeaderParams(request);
        String requestHeaderString = mapToString(requestHeaders);
        LOGGER.log(Level.INFO, "Header " + requestHeaderString);

        if ("Bitbucket-Webhooks/2.0".equals(request.getHeader("user-agent"))) {
            if ("pullrequest:created".equals(request.getHeader("x-event-key")) || "pullrequest:updated".equals(request.getHeader("x-event-key"))) {
                processPullRequetPayload(payload);
            }
        } else if (payload.has("actor") && payload.has("repository") && payload.getJSONObject("repository").has("links")) {
            if ("repo:push".equals(request.getHeader("x-event-key"))) {
                LOGGER.log(Level.INFO, "Processing new Webhooks payload");
            }
        } else if (payload.has("actor")) {
        	// we assume that the passed hook was from bitbucket server https://confluence.atlassian.com/bitbucketserver/managing-webhooks-in-bitbucket-server-938025878.html
        	LOGGER.log(Level.INFO, "Processing webhook for self-hosted bitbucket instance");
        } else {
            LOGGER.log(Level.INFO, "Processing old POST service payload");
        }
    }

    private void processPullRequetPayload(JSONObject payload) {
        JSONObject pullRequestPayload = payload.getJSONObject("pullrequest");

        JSONObject source = pullRequestPayload.getJSONObject("source");
        JSONObject destination = pullRequestPayload.getJSONObject("destination");
        
        String sourceBranch = source.getJSONObject("branch").getString("name");
        String desBranch = destination.getJSONObject("branch").getString("name");

        //LOGGER.log(Level.INFO, "Source Branch Name {0} Destination Branch Name {1}", new Object[] { sourceBranch, desBranch } );

        JSONObject repo = payload.getJSONObject("repository");
        String user = getUser(payload, "actor");
        String url = repo.getJSONObject("links").getJSONObject("html").getString("href");
        String scm = repo.has("scm") ? repo.getString("scm") : "git";
        probe.triggerPullRequestJobs(user, url, scm, payload.toString());

    }
    private String mapToString(HashMap<String, String> map) {  
        StringBuilder stringBuilder = new StringBuilder();  
        for (String key : map.keySet()) {  
            if (stringBuilder.length() > 0) {  
                stringBuilder.append("&");  
            }  
            String value = map.get(key);  
            try {  
                stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));  
                stringBuilder.append("=");  
                stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");  
            } catch (UnsupportedEncodingException e) {  
                throw new RuntimeException("This method requires UTF-8 encoding support", e);  
            }  
        }  
        return stringBuilder.toString();  
    }

    private HashMap<String, String> getRequestHeaderParams(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        HashMap<String, String> requestHeaders = new HashMap<String, String>();
        while (headerNames.hasMoreElements()) {
 
            String headerName = headerNames.nextElement();
            Enumeration<String> headers = request.getHeaders(headerName);
            String headerValue = "";
            while (headers.hasMoreElements()) {
                headerValue = headers.nextElement();
                headerValue += ",";
            }
            requestHeaders.put(headerName, headerValue);           
        }
        return requestHeaders;
        
    }

    private String getUser(JSONObject payload, String jsonObject) {
        String user;
        try {
            user = payload.getJSONObject(jsonObject).getString("username");
        } catch (JSONException e1) {
            try {
                user = payload.getJSONObject(jsonObject).getString("nickname");
            } catch (JSONException e2) {
                user = payload.getJSONObject(jsonObject).getString("display_name");
            }
        }
        return user;
    }
    
    private static final Logger LOGGER = Logger.getLogger(BitbucketPayloadProcessor.class.getName());

}