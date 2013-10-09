package models;

import java.io.File;
import jp.co.flect.heroku.platformapi.PlatformApi;
import jp.co.flect.util.Git;
import jp.co.flect.util.Git.RefSpec;
import jp.co.flect.io.FileUtils;

object Heroku {
  
  private val AUTH_URL = "https://login.salesforce.com/services/oauth2/authorize";
  private val TOKEN_URL = "https://login.salesforce.com/services/oauth2/token";

  private val BASE_APP = "enq-app";
  private val APP_GITHUB = "https://github.com/shunjikonishi/enq-app.git";

  private val HEROKU_APPID = System.getenv("HEROKU_APPID");
  private val HEROKU_SECRET = System.getenv("HEROKU_SECRET");
  
  val HEROKU_USERNAME = System.getenv("HEROKU_USERNAME");
  val HEROKU_APITOKEN = System.getenv("HEROKU_APITOKEN");
  
  def oauthUrl = PlatformApi.getOAuthUrl(HEROKU_APPID, PlatformApi.Scope.Global);
  
  def login(code: String) = PlatformApi.fromOAuth(HEROKU_SECRET, code);
  
  def adminApi = PlatformApi.fromApiToken(HEROKU_USERNAME, HEROKU_APITOKEN);
  
  def cloneApp(app: AppStatus, gitUrl: String, json: String) = {
    app.status = AppStatus.GIT_WAIT;
    synchronized {
      val workDir = new File("work");
      workDir.mkdirs();
      val appDir = new File(workDir, BASE_APP);
      if (!appDir.exists) {
        val cloneGit = new Git(workDir);
        cloneGit.clone(APP_GITHUB);
        println(cloneGit.getStdOut);
        println(cloneGit.getStdErr);
      }
      val jsonFile = new File(appDir, "app/data/form.json");
      FileUtils.writeFile(jsonFile, json.getBytes("utf-8"));
      
      app.status = AppStatus.GIT_PUSH;
      val git = new Git(appDir);
      
      git.commit(true, "update json");
      println(git.getStdOut);
      println(git.getStdErr);
      
      git.push(gitUrl, new RefSpec("master"));
      println(git.getStdOut);
      println(git.getStdErr);
      
      app.status = AppStatus.END_GENERATE;
      
    }
  }
  
}