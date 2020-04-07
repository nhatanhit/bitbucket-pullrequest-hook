package io.jenkins.plugins.api;

import com.google.gson.annotations.SerializedName;

public class BitbucketAccessToken {
    @SerializedName("access_token")
    String accessToken;

    @SerializedName("scopes")
    String scopes;

    @SerializedName("expires_in")
    Integer expiresIn;

    @SerializedName("refresh_token")
    String refreshToken;

    @SerializedName("token_type")
    String tokenType;

    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    public String getScopes()
    {
        return scopes;
    }

    public void setScopes(String scopes)
    {
        this.scopes = scopes;
    }

    public Integer getExpiresIn()
    {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn)
    {
        this.expiresIn = expiresIn;
    }
    public String getRefreshToken()
    {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }

    public String getTokenType()
    {
        return tokenType;
    }

    public void setTokenType(String tokenType)
    {
        this.tokenType = tokenType;
    }
}