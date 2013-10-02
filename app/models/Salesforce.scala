package models;

import jp.co.flect.net.OAuth2;
import jp.co.flect.net.OAuthResponse;
import jp.co.flect.soap.WSDL;
import java.io.File;

object Salesforce {
  
  private val AUTH_URL = "https://login.salesforce.com/services/oauth2/authorize";
  private val TOKEN_URL = "https://login.salesforce.com/services/oauth2/token";

  private val SALESFORCE_APPID = System.getenv("SALESFORCE_APPID");
  private val SALESFORCE_SECRET = System.getenv("SALESFORCE_SECRET");
  
  lazy val wsdl = new WSDL(new File("conf/partner.wsdl"));
  
  private def createOAuth2(host: String) = {
    val redirectUrl = if (host.indexOf("localhost") == -1) {
      "https://" + host + "/login";
    } else {
      "http://" + host + "/login";
    }
    new OAuth2(
      AUTH_URL,
      TOKEN_URL,
      SALESFORCE_APPID,
      SALESFORCE_SECRET,
      redirectUrl
    );
  }
  
  def getLoginUrl(host: String) = {
    createOAuth2(host).getLoginUrl();
  }
  
  def login(host: String, code: String) = {
    createOAuth2(host).authenticate(code);
  }
}

class Salesforce {
  
}
