package models;

import play.api.cache.Cache;
import play.api.Play.current;
import play.api.mvc.Request;
import play.api.mvc.AnyContent;

import jp.co.flect.play2.utils.Params;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObjectDef;

object AppStatus {

  val INDEX = new StatusValue(1, "index");
  val LOGIN_SALESFORCE = new StatusValue(2, "loginSalesforce");
  val SELECT_OBJECT = new StatusValue(3, "selectObject");

  def apply(request: Request[AnyContent]) = new AppStatus(request.host, new Params(request).sessionId);
  
}

sealed case class StatusValue(index: Int, name: String) {
  override def toString = name;
}

class AppStatus(host: String, val sessionId: String) {

  def status = Cache.getOrElse[StatusValue](sessionId + "-status", 7200) { AppStatus.INDEX}
  def status_=(v: StatusValue) { Cache.set(sessionId + "-status", v, 7200)}
  
  def salesforceLoginUrl = Salesforce.getLoginUrl(host);
  
  def salesforceLogin(code: String) = {
    val res = Salesforce.login(host, code);
    val client = new SalesforceClient(Salesforce.wsdl);
    client.login(res);
    status = AppStatus.LOGIN_SALESFORCE;
    Cache.set(sessionId + "-salesforce", SalesforceLoginInfo(client.getSessionId, client.getEndpoint), 7200);
  }
  
  def objectList = {
    val client = createClient;
    client.describeGlobal.getObjectDefList;
  }
  
  def getObjectDef(name: String) = {
    val client = createClient;
    Cache.getOrElse[SObjectDef](sessionId + "-object-" + name, 600) {
      client.describeSObject(name);
    }
  }
  
  private def createClient = {
    Cache.getAs[SalesforceLoginInfo](sessionId + "-salesforce") match {
      case Some(info) =>
        val client = new SalesforceClient(Salesforce.wsdl);
        client.setSessionId(info.sessionId);
        client.setEndpoint(info.endpoint);
        client;
      case None =>
        status = AppStatus.INDEX;
        throw new RedirectException("Not logined", "/");
    }
  }
}

case class SalesforceLoginInfo(sessionId: String, endpoint: String);

class RedirectException(msg: String, val url: String) extends Exception(msg);
