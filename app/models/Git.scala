package models;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Types;
import play.api.db.DB;
import play.api.Play.current;

import jp.co.flect.sql.DBTool;
import jp.co.flect.sql.DBTool.Creator;
import jp.co.flect.sql.Table;
import jp.co.flect.io.FileUtils;

object Git {
  
  private val BASE_APP = "enq-app";
  private val APP_GITHUB = "git@github.com:shunjikonishi/enq-app.git";
  
  private val dir = new File(".ssh");
  private val privateKey = new File(dir, "id_rsa");
  private val publicKey = new File(dir, "id_rsa.pub");
  
  def init = {
    val workDir = new File("work");
    workDir.mkdirs();
    dir.mkdirs();
    if (!privateKey.exists || !publicKey.exists) {
      if (!loadKeys) {
        generateKeys;
        true;
      } else {
        false;
      }
    } else {
      false;
    }
  }
  
  def publicKeyStr = FileUtils.readFileAsString(publicKey, "utf-8");
  
  private def loadKeys = DB.withConnection { con =>
    con.setAutoCommit(false);
    val db = new DBTool(con);
    val cnt = db.getCount("SELECT COUNT(*) FROM SSH_KEYS");
    if (cnt == 0) {
      false;
    } else {
      val key = db.create("SELECT PRIVATE_KEY, PUBLIC_KEY FROM SSH_KEYS", new KeyCreator());
      FileUtils.writeFile(privateKey, key.privateKey.getBytes("utf-8"));
      FileUtils.writeFile(publicKey, key.publicKey.getBytes("utf-8"));
      true;
    }
  }
  
  private def runCommand(commands: Array[String], dir: File = new File(".")) = {
    val p = Runtime.getRuntime().exec(commands, null, dir);
    new ReadThread("out", p.getInputStream()).start();
    new ReadThread("err", p.getErrorStream()).start();
    try {
      p.waitFor();
    } catch {
      case e: InterruptedException =>
        e.printStackTrace;
    }
  }
  
  private def generateKeys() = DB.withConnection { con =>
    con.setAutoCommit(false);
    val commands = Array(
      "ssh-keygen",
      "-f",
      dir.toString + File.separator + privateKey.getName,
      "-N",
      "\"\""
    );
    runCommand(commands);
    val table = new SshKeyTable();
    table.privateKey = FileUtils.readFileAsString(privateKey, "utf-8");
    table.publicKey = FileUtils.readFileAsString(publicKey, "utf-8");
    val db = new DBTool(con);
    db.insert(table);
    db.commit
  }
  
  def cloneApp = {
    val workDir = new File("work");
    val appDir = new File(workDir, BASE_APP);
    if (!appDir.exists) {
      val commands = Array(
        "git",
        "clone",
        APP_GITHUB
      );
      runCommand(commands, workDir);
    }
  }
  
  def push(gitUri: String, json: String) {
    synchronized {
      val appDir = new File("work", BASE_APP);
      val jsonFile = new File("work/" + BASE_APP + "/app/data/form.json");
      FileUtils.writeFile(jsonFile, json.getBytes("utf-8"));
      runCommand(Array(
        "git",
        "commit",
        "-a",
        "-m",
        "updateJson"
      ), appDir);
      runCommand(Array(
        "git",
        "push",
        gitUri,
        "master"
      ), appDir);
    }
  }
  
  case class SshKey(privateKey: String, publicKey: String)
  
  class KeyCreator extends Creator[SshKey] {
    def create(rs: ResultSet) = {
      if (!rs.next) {
        throw new IllegalStateException();
      }
      SshKey(rs.getString(1), rs.getString(2));
    }
  }

  class ReadThread(name: String, is: InputStream) extends Thread {
    
    override def run {
      try {
        val reader = new BufferedReader(new InputStreamReader(is));
        try {
          var line = reader.readLine();
          while (line != null) {
            System.out.println(name + ": " + line);
            line = reader.readLine();
          }
        } finally {
          reader.close();
        }
      } catch {
        case e: IOException =>
        e.printStackTrace();
      }
    }
  }
  
  class SshKeyTable extends Table("SSH_KEYS", true) {
    
    override def init {
      addField("PRIVATE_KEY", Types.VARCHAR, false);
      addField("PUBLIC_KEY", Types.VARCHAR, false);
    }
    def privateKey = doGetString("PRIVATE_KEY");
    def privateKey_=(v: String) = set("PRIVATE_KEY", v);
    
    def publicKey = doGetString("PUBLIC_KEY");
    def publicKey_=(v: String) = set("PUBLIC_KEY", v);
 }
}
