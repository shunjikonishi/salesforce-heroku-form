package jp.co.flect.play2.tags;

import jp.co.flect.io.FileUtils;
import java.io.File;

object Include {
  
  def apply(filename: String, enc: String = "utf-8") = 
    FileUtils.readFileAsString(new File(filename), enc);
}