package io.blue.repo

import collection.JavaConverters._
import java.sql.DriverManager

class Connection {

  var name: String = _

  var url: String = _

  var username: String = _

  var password: String = _

  var className: String = _

  def connect = {
    Class.forName(className)
    DriverManager.getConnection(url, username, password)
  }

}