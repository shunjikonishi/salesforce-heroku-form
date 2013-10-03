package controllers

import play.api._
import play.api.mvc._
import models.Salesforce;
import models.AppStatus;
import models.RedirectException;

object Application extends Controller {
  
  def index = Action { implicit request =>
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
  
}