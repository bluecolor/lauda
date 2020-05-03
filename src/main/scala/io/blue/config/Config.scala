package io.blue.config

import java.util.HashMap
import java.io.File
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory


object Config {
  def read: Config = read("config.yml")
  def read(file: String = "config.yml"): Config = {
    val mapper = new ObjectMapper(new YAMLFactory)
    mapper.readValue(new File(file), classOf[Config])
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Config {

  @JsonProperty(value="repository")
  var repository: Repository = _

  @JsonProperty(value="vendors")
  var vendors: HashMap[String, HashMap[String, HashMap[String, String]]] = _

}