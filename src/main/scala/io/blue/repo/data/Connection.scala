package io.blue.repo.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Connection {

  @JsonProperty(value="name")
  var name: String = _

  @JsonProperty(value="url")
  var url: String = _

  @JsonProperty(value="username")
  var username: String = _

  @JsonProperty(value="password")
  var password: String = _

  @JsonProperty(value="class_name")
  var className: String = _
}