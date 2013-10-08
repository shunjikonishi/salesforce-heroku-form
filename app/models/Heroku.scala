package models;

import jp.co.flect.heroku.platformapi.PlatformApi;

object Heroku {
  private val AUTH_URL = "https://login.salesforce.com/services/oauth2/authorize";
  private val TOKEN_URL = "https://login.salesforce.com/services/oauth2/token";

  private val HEROKU_APPID = System.getenv("HEROKU_APPID");
  private val HEROKU_SECRET = System.getenv("HEROKU_SECRET");
  
  def oauthUrl = PlatformApi.getOAuthUrl(HEROKU_APPID, PlatformApi.Scope.Global);
  
  def login(code: String) = PlatformApi.fromOAuth(HEROKU_SECRET, code);
}