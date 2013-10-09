package models;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Types;
import play.api.db.DB;
import play.api.Play.current;

import jp.co.flect.sql.DBTool;
import jp.co.flect.sql.DBTool.Creator;
import jp.co.flect.sql.Table;
import jp.co.flect.io.FileUtils;
import jp.co.flect.io.RunProcess;

object Ssh {
  
  private val sshDir = new File(".ssh");
  private val privateKey = new File(sshDir, "id_rsa");
  private val publicKey = new File(sshDir, "id_rsa.pub");
  
  def init = {
    sshDir.mkdirs();
    val config = new File(sshDir, "config");
    if (!config.exists) {
      FileUtils.writeFile(config, "StrictHostKeyChecking no".getBytes("utf-8"));
    }
    if (!privateKey.exists || !publicKey.exists) {
      if (!loadKeys) {
        generateKeys;
        Heroku.adminApi.addKey(readFile(publicKey));
      }
    }
  }
  
  private def readFile(f: File) = FileUtils.readFileAsString(f, "utf-8");
  
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
  
  private def generateKeys() = DB.withConnection { con =>
    con.setAutoCommit(false);
    val rp = new RunProcess();
    rp.run(
      "ssh-keygen",
      "-f",
      sshDir.toString + File.separator + privateKey.getName,
      "-N",
      ""
    );
    val table = new SshKeyTable();
    table.privateKey = readFile(privateKey);
    table.publicKey = readFile(publicKey);
    val db = new DBTool(con);
    db.insert(table);
    db.commit
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
