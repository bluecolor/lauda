package io.blue.config

import collection.JavaConverters._
import java.sql.DriverManager
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
class Repository {

  @JsonProperty(value="url")
  var url: String = _

  @JsonProperty(value="username")
  var username: String = _

  @JsonProperty(value="password")
  var password: String = _

  @JsonProperty(value="class_name")
  var className: String = _

  @JsonProperty(value="up")
  var up: java.util.List[String] = _

  @JsonProperty(value="down")
  var down: java.util.List[String] = _

}