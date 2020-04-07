package io.jenkins.plugins;

import java.util.List;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import com.google.gson.Gson;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.StatusLine;
import org.apache.commons.httpclient.HttpStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.HttpResponse;
import org.apache.commons.io.IOUtils;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.Base64;

import org.apache.http.NameValuePair;
import java.util.ArrayList;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import java.net.URLEncoder;
import org.apache.http.client.methods.HttpGet;
import io.jenkins.plugins.api.BitbucketAccessToken;

public class PullRequestTrigger extends  Trigger<Job<?, ?>> {
	private List<BranchVariable> destinationBranches;
	private String comsumerClientId;
	private String comsumerClientPassword;
	private String appUsername;
	private String appPassword;
	private String repoName;
	private String workspaceName;
	private String copyFromBranch;
	private static final Logger LOGGER = Logger.getLogger(PullRequestTrigger.class.getName());

	@DataBoundConstructor
    public PullRequestTrigger(List<BranchVariable> branches, 
							String comsumerClientId,
							String comsumerClientPassword,
							String appUsername,
							String appPassword,
							String repoName,
							String workspaceName,
							String copyFromBranch) {
        this.destinationBranches = branches;
		this.comsumerClientId = comsumerClientId;
		this.comsumerClientPassword = comsumerClientPassword;
		this.appUsername = appUsername;
		this.appPassword = appPassword;
		this.repoName = repoName;
		this.workspaceName = workspaceName;
		this.copyFromBranch = copyFromBranch;
	}
	
	public List<BranchVariable> getDestinationBranches() {

	    return this.destinationBranches;
	}
	
	public String getComsumerClientId() {
		return this.comsumerClientId;
	}

	public String getComsumerClientPassword() {
		return this.comsumerClientPassword;
	}

	public String getAppUsername() {
		return this.appUsername;
	}

	public String getAppPassword() {
		return this.appPassword;
	}

	public String getRepoName() {
		return this.repoName;
	}

	public String getWorkspaceName() {
		return this.workspaceName;
	}

	public String getCopyFromBranch() {
		return this.copyFromBranch;
	}

	@DataBoundSetter
	public void setDestinationBranches(List<BranchVariable> destinationBranches) {
	    this.destinationBranches = destinationBranches;
	}
	
	@DataBoundSetter
	public String setComsumerClientId(String comsumerClientId) {
		return this.comsumerClientId = comsumerClientId;
	}

	@DataBoundSetter
	public String setComsumerClientPassword(String comsumerClientPassword) {
		return this.comsumerClientPassword = comsumerClientPassword;
	}

	@DataBoundSetter
	public String setAppUsername(String appUsername) {
		return this.appUsername = appUsername;
	}

	@DataBoundSetter
	public String setAppPassword(String appPassword) {
		return this.appPassword = appPassword;
	}

	@DataBoundSetter
	public String setRepoName(String repoName) {
		return this.repoName = repoName;
	}

	@DataBoundSetter
	public String setWorkspaceName(String workspaceName) {
		return this.workspaceName = workspaceName;
	}

	@DataBoundSetter
	public String setCopyFromBranch(String copyFromBranch) {
		return this.copyFromBranch = copyFromBranch;
	}

	
	@Extension @Symbol("PullRequestTrigger")
	public static class PullRequestTriggerDescriptor extends TriggerDescriptor {
		private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Hudson.MasterComputer.threadPoolForRemoting);
		@Override
	    public boolean isApplicable(final Item item) {
	      return Job.class.isAssignableFrom(item.getClass());
	    }

	    @Override
	    public String getDisplayName() {
	      return "Copy Pull Request";
	    }
	    
	}	

	@Override
    public PullRequestTriggerDescriptor getDescriptor() {
        return (PullRequestTriggerDescriptor)super.getDescriptor();
    }

	public void onPost(String triggeredByUser, final String payload) {
        JSONObject payloadJsonObject = new JSONObject(payload);
		String sourceBranch = payloadJsonObject.getJSONObject("pullrequest").getJSONObject("source").getJSONObject("branch").getString("name");
		String destinationBranch = payloadJsonObject.getJSONObject("pullrequest").getJSONObject("destination").getJSONObject("branch").getString("name");
		String titlePullRequest = payloadJsonObject.getJSONObject("pullrequest").getString("title");
		
		boolean duplicatedPr = false;
		for (int i = 0; i < this.destinationBranches.size(); i ++) {
			String branchName = this.destinationBranches.get(i).getBranchName();
			if (branchName.equals(destinationBranch)) {
				duplicatedPr = true;
				break;
			}
		}
		
		if (!duplicatedPr && destinationBranch.equals(this.copyFromBranch)) {
			final String pushBy = triggeredByUser;
			try {
				String authorizationHeaderCredential = this.getComsumerClientId() + ":" + this.getComsumerClientPassword();
				String authorizationHeader = "Basic " +  Base64.getEncoder().encodeToString(authorizationHeaderCredential.getBytes());

				Gson gson = new Gson();
				String url = "https://bitbucket.org/site/oauth2/access_token";
				HttpPost post = new HttpPost(url);
				post.addHeader("Authorization", authorizationHeader);


				List<NameValuePair> urlParameters = new ArrayList<>();
				urlParameters.add(new BasicNameValuePair("username", this.getAppUsername()));
				urlParameters.add(new BasicNameValuePair("password", this.getAppPassword()));
				urlParameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
				
				//username anhnguyenapacpro 
				//password LfBQcPRghdXT87xjHJgK
				//grant_type client_credentials

				/*JSONObject accessTokenBody = new JSONObject();
				accessTokenBody.put("username", this.getAppUsername());
				accessTokenBody.put("password", this.getAppPassword());
				accessTokenBody.put("grant_type","client_credentials");
				post.addHeader("Content-Type","application/json");*/
				//post.setEntity(new StringEntity(accessTokenBody.toString()));
				post.setEntity(new UrlEncodedFormEntity(urlParameters));

				CloseableHttpClient httpClient = HttpClientBuilder.create().build();
				HttpResponse  response = httpClient.execute(post);
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK || response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					String responseString = out.toString();
					out.close();
					
					BitbucketAccessToken bitbucketAccessToken = gson.fromJson(responseString, BitbucketAccessToken.class);
					LOGGER.log(Level.FINE, "access token is {0}",bitbucketAccessToken.getAccessToken() );
					String accessToken = bitbucketAccessToken.getAccessToken();
					if (!accessToken.isEmpty()) {
						
						//check PR if it's not existed and create PR 
						for (int i = 0; i < this.destinationBranches.size(); i ++) {
							String destinationBranchName = this.destinationBranches.get(i).getBranchName();
							
							Integer numberExistedPr = checkCopyPullRequestNotExisted(accessToken, sourceBranch, destinationBranchName);
							if (numberExistedPr == 0) {
								createPullRequest(accessToken, titlePullRequest, sourceBranch, destinationBranchName);
							} else {
								LOGGER.log(Level.INFO,"PR existed or Error with branch " + destinationBranchName + ", number PR is " + numberExistedPr.toString());		
							}
						}
					} else {
						LOGGER.log(Level.SEVERE,"Empty Access Token");	
					}
					
				} else {
					InputStream responseStream = response.getEntity().getContent();
					String errorStr = IOUtils.toString(responseStream, StandardCharsets.UTF_8); 
					LOGGER.log(Level.SEVERE,"Response Error {0}", errorStr);
					response.getEntity().getContent().close();
					throw new IOException(statusLine.getReasonPhrase());
				}
			
			} catch(UnsupportedEncodingException e) {
				LOGGER.log(Level.SEVERE,"Exception is {0}", e.getStackTrace());
			} catch(ClientProtocolException e) {
				LOGGER.log(Level.SEVERE,"Exception is {0}", e.getStackTrace());
			} catch(IOException e) {
				LOGGER.log(Level.SEVERE ,"Exception is {0}", e.getStackTrace());
			}
		}
	}

	private void createPullRequest(String accessToken, String pullRequestTitle, String srcBranch, String desBranch) {
		try {
			String authorizationHeader = "Bearer " +  accessToken;
		
			String url = "https://api.bitbucket.org/2.0/repositories/" +  this.getWorkspaceName() + "/" + this.getRepoName() + "/pullrequests";
			HttpPost post = new HttpPost(url);
			post.addHeader("Authorization", authorizationHeader);
			post.addHeader("Content-Type", "application/json");

			JSONObject pullRequestBody = new JSONObject();
			pullRequestBody.put("title", pullRequestTitle);
			
			JSONObject sourceBranch = new JSONObject();
			sourceBranch.put("branch", new JSONObject().put("name", srcBranch));
			pullRequestBody.put("source", sourceBranch);

			JSONObject destBranch = new JSONObject();
			destBranch.put("branch", new JSONObject().put("name", desBranch));
			pullRequestBody.put("destination", destBranch);

			post.addHeader("Content-Type","application/json");
			post.setEntity(new StringEntity(pullRequestBody.toString()));

			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			HttpResponse  response = httpClient.execute(post);
			StatusLine statusLine = response.getStatusLine();

			if (statusLine.getStatusCode() == HttpStatus.SC_OK || response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				String responseString = out.toString();
				out.close();
				LOGGER.log(Level.FINE, "Resonse is {0}", responseString);
			} else {
				InputStream responseStream = response.getEntity().getContent();
				String errorStr = IOUtils.toString(responseStream, StandardCharsets.UTF_8); 
				LOGGER.log(Level.SEVERE,"Response Error {0}", errorStr);
				response.getEntity().getContent().close();
				throw new IOException(statusLine.getReasonPhrase());
			}
		} catch(UnsupportedEncodingException e) {
			LOGGER.log(Level.SEVERE,"Exception is {0}", e.getStackTrace());
		} catch(ClientProtocolException e) {
			LOGGER.log(Level.SEVERE,"Exception is {0}", e.getStackTrace());
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE ,"Exception is {0}", e.getStackTrace());
		}
	}
	private Integer checkCopyPullRequestNotExisted(String accessToken, String sourceBranch, String targetBranch) {
		try { 
			String authorizationHeader = "Bearer " +  accessToken;
		
			String url = "https://api.bitbucket.org/2.0/repositories/" +  this.getWorkspaceName() + "/" + this.getRepoName() + "/pullrequests";
			String query="source.branch.name=" + "\"" + sourceBranch + "\" AND destination.branch.name=\"" + targetBranch + "\" AND state=\"OPEN\"";
			String encodedQuery=URLEncoder.encode( query, "UTF-8" );
			url += "?q=" + encodedQuery;

			HttpGet request = new HttpGet(url);
			request.addHeader("Authorization", authorizationHeader);
			request.addHeader("Content-Type", "application/json");

			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			HttpResponse  response = httpClient.execute(request);
			StatusLine statusLine = response.getStatusLine();

			if (statusLine.getStatusCode() == HttpStatus.SC_OK || response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				String responseString = out.toString();
				out.close();
				LOGGER.log(Level.FINE, "Resonse is {0}", responseString);
				JSONObject jsonResponse = new JSONObject(responseString);
				Integer numberPr = jsonResponse.getInt("size");
				return numberPr;
			} else {
				InputStream responseStream = response.getEntity().getContent();
				String errorStr = IOUtils.toString(responseStream, StandardCharsets.UTF_8); 
				LOGGER.log(Level.SEVERE,"Response Error {0}", errorStr);
				response.getEntity().getContent().close();
				throw new IOException(statusLine.getReasonPhrase());
			}
		} catch(UnsupportedEncodingException e) {
			LOGGER.log(Level.SEVERE,"Exception is {0}", e.getStackTrace());
			return -1;
		} catch(ClientProtocolException e) {
			LOGGER.log(Level.SEVERE,"Exception is {0}", e.getStackTrace());
			return -1;
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE,"Exception is {0}", e.getStackTrace());
			return -1;
		}
		
	}
}
