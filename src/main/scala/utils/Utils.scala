package utils

import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding

/**
  * Created by taras.beletsky on 8/21/16.
  */
object Utils {

  def encodeBase64(str: String) = BaseEncoding.base64().encode(str.getBytes(Charsets.UTF_8))

  def encodeBasicToken(username: String, password: String) = s"Basic ${encodeBase64(s"$username:$password")}"

  def decodeBase64BasicUsername(token: String) = if (token.startsWith("Basic ")) {
    val base64 = token.substring(6)
    val str = new String(BaseEncoding.base64().decode(base64), "UTF-8")
    if (str.indexOf(":") != -1) Some(str.substring(0, str.indexOf(":"))) else None
  } else None
}
