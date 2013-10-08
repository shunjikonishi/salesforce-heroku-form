package models;

import scala.collection.JavaConversions._

import play.api.cache.Cache;
import play.api.Play.current;
import play.api.mvc.Request;
import play.api.mvc.AnyContent;

import jp.co.flect.play2.utils.Params;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.PicklistEntry;
import jp.co.flect.heroku.platformapi.PlatformApi;
import jp.co.flect.heroku.platformapi.model.Region;
import com.google.gson.Gson;

object AppStatus {

  val INDEX = new StatusValue(1, "index");
  val LOGIN_SALESFORCE = new StatusValue(2, "loginSalesforce");
  val SELECT_OBJECT = new StatusValue(3, "selectObject");
  val SELECT_FIELD = new StatusValue(4, "selectField");
  val LOGIN_HEROKU = new StatusValue(5, "loginHeroku");

  def apply(request: Request[AnyContent]) = new AppStatus(request.host, new Params(request).sessionId);
  
}

sealed case class StatusValue(index: Int, name: String) {
  override def toString = name;
}

class AppStatus(host: String, val sessionId: String) {

  def status = Cache.getOrElse[StatusValue](sessionId + "-status", 7200) { AppStatus.INDEX}
  def status_=(v: StatusValue) { Cache.set(sessionId + "-status", v, 7200)}
  
  def objectName = Cache.getOrElse[String](sessionId + "-objectName", 7200) { ""}
  def objectName_=(v: String) { Cache.set(sessionId + "-objectName", v, 7200)}
  
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
  
  def toJson(obj: SObjectDef) = {
    val fieldList = obj.getFieldList.map(_.getMap);
    fieldList.foreach { map =>
      val pickList = map.get("picklistValues");
      pickList match {
        case x: java.util.List[_] =>
          map.put("picklistValues", asJavaCollection(x.map{ _ match {
            case y: PicklistEntry => y.getMap;
            case z => z;
          }}));
        case _ =>
      }
    }
    println(asJavaCollection(fieldList));
    new Gson().toJson(asJavaCollection(fieldList));
  }
  
  def saveJson(json: String) = {
    Cache.set(sessionId + "-json", json, 7200);
    status = AppStatus.SELECT_FIELD;
  }
  
  def loadJson = {
    Cache.getOrElse[String](sessionId + "-json", 7200) {"{}"};
  }
  
  def herokuLogin(code: String) = {
    val api = Heroku.login(code);
    status = AppStatus.LOGIN_HEROKU;
    Cache.set(sessionId + "-heroku", api, 7200);
  }
  
  def generateApp(appName: String, sfUser: Option[String], sfPass: Option[String], sfToken: Option[String]) = {
    val api = Cache.getAs[PlatformApi](sessionId + "-heroku").get;
    val app = api.createApp(appName, Region.US);
    api.addCollaborator(appName, Heroku.HEROKU_USERNAME);
    val map = if (sfUser.isDefined && sfPass.isDefined && sfToken.isDefined) {
      Map(
        "SALESFORCE_USERNAME" -> sfUser.get,
        "SALESFORCE_PASSWORD" -> sfPass.get,
        "SALESFORCE_TOKEN" -> sfToken.get
      )
    } else {
      Map()
    } + ("SALESFORCE_OBJECT_NAME" -> objectName);
    api.setConfigVars(appName, map);
    
    val adminApi = Heroku.adminApi;
    if (Git.init) {
      val publicKey = Git.publicKeyStr;
      adminApi.addKey(publicKey);
    }
    Git.cloneApp;
    Git.push(app.getGitUrl, loadJson);
  }
}

case class SalesforceLoginInfo(sessionId: String, endpoint: String);

class RedirectException(msg: String, val url: String) extends Exception(msg);
