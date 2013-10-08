package controllers

import play.api._
import play.api.mvc._
import play.api.data.Form;
import play.api.data.Forms.tuple;
import play.api.data.Forms.text;
import play.api.data.Forms.optional;
import models.Heroku;
import models.Salesforce;
import models.AppStatus;
import models.RedirectException;
import jp.co.flect.play2.utils.Params;

object Application extends Controller {
  
  def index = Action { implicit request =>
    val app = AppStatus(request);
    app.status = AppStatus.INDEX;
    try {
      Ok(views.html.index(app));
    } catch {
      case e: RedirectException =>
        e.printStackTrace;
        Redirect(e.url);
    }
  }
  
  def selectObject = Action { implicit request =>
    val app = AppStatus(request);
    try {
      Ok(views.html.index(app));
    } catch {
      case e: RedirectException =>
        e.printStackTrace;
        Redirect(e.url);
    }
  }
  
  def selectField(name: String) = Action { implicit request =>
    val app = AppStatus(request);
    if (name == null || name.length == 0 || app.status != AppStatus.LOGIN_SALESFORCE) {
      app.status = AppStatus.INDEX;
      Redirect("/");
    } else {
      app.status = AppStatus.SELECT_OBJECT;
      app.objectName = name;
      try {
        var obj = app.getObjectDef(name);
        
        Ok(views.html.index(app, Some(obj)));
      } catch {
        case e: RedirectException =>
          e.printStackTrace;
          Redirect(e.url);
      }
    }
  }
  
  def login(code: String) = Action { implicit request =>
    val app = AppStatus(request);
    app.salesforceLogin(code);
    Redirect("/selectObject");
  }
  
  def fieldList(name: String) = Action { implicit request =>
    val app = AppStatus(request);
    try {
      val objectDef = app.getObjectDef(name);
      Ok(views.html.fieldList(app, objectDef));
    } catch {
      case e: RedirectException =>
        Redirect(e.url);
    }
  }

  def postJson = Action { implicit request =>
    val app = AppStatus(request);
    Params(request).get("json") match {
      case Some(json) => 
        app.saveJson(json);
        Ok(Heroku.oauthUrl);
      case None => 
        BadRequest;
    }
  }
    
  def herokuLogin(code: String) = Action { implicit request =>
    val app = AppStatus(request);
    app.herokuLogin(code);
    Redirect("/herokuSetting");
  }
  
  def herokuSetting = Action { implicit request =>
    val app = AppStatus(request);
    try {
      Ok(views.html.index(app));
    } catch {
      case e: RedirectException =>
        e.printStackTrace;
        Redirect(e.url);
    }
  }
  
  
  private val generateForm = Form(tuple(
    "appName" -> text,
    "salesforceUser" -> optional(text),
    "salesforcePass" -> optional(text),
    "salesforceToken" -> optional(text)
  ));
  
  def generateApp = Action { implicit request =>
    val data = generateForm.bindFromRequest;
    if (data.hasErrors) {
      BadRequest;
    } else {
      val app = AppStatus(request);
      val (appName, sfUser, sfPass, sfToken) = data.get;
      app.generateApp(appName, sfUser, sfPass, sfToken);
      Redirect("http://" + appName + ".herokuapp.com/");
    }
  }
  
  def gitInit = Action { implicit request =>
    models.Git.init;
    Ok("OK");
  }
  
}