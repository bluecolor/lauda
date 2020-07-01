package io.blue.config

import collection.JavaConverters._
import java.util.HashMap
import java.io.File
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.typesafe.scalalogging.LazyLogging


object Config {

  var _instance: Config = _

  def instance: Config = {
    if(_instance != null) return _instance
    read("config.yml")
  }

  def read: Config = read("config.yml")
  def read(file: String = "config.yml"): Config = {
    if (_instance != null) {
      return _instance
    } else {
      val mapper = new ObjectMapper(new YAMLFactory)
      _instance = mapper.readValue(new File(file), classOf[Config])
      return _instance
    }
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Vendor extends LazyLogging{
  @JsonProperty(value="name")
  var name: String = _

  @JsonProperty(value="column_map")
  var columnMap: HashMap[String, String] = _

  def columnTypetoVendor(typeName: String, precision: Int): String = {

    val default = s"${typeName} ${if (precision > 0) "("+precision+")" else ""}"

    try {
      var elements = columnMap.get(typeName.toLowerCase).split(" ").take(2).toList
      if (elements.length == 1) {
        elements ::= precision.toString
      }
      if (elements(0) == "_") { elements = elements.patch(0, List(typeName), 1) }
      if (elements(1) == "_") { elements = elements.patch(1, List(precision.toString), 1) }

      return s"${elements(0)} ${if (elements(1).toInt > 0) "("+elements(1)+")" else ""}"
    } catch {
      case e: Exception =>
        logger.debug(s"Unable to convert column type: ${typeName.toLowerCase}")
        return default
    }

    return default
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Config {

  @JsonProperty(value="repository")
  var repository: Repository = _

  @JsonProperty(value="vendors")
  var vendors: java.util.List[Vendor] = _

  def getVendor(name: String) : Option[Vendor] = {
    vendors.asScala.find(_.name == name)
  }

  def columnTypetoVendor (vendor: Option[String], name: String, typeName: String, precision: Int) : String = {
    val default = s"${name} ${typeName} ${if (precision > 0) "("+precision+")" else ""}"

    if (!vendor.isDefined) {
      return default
    }
    val v = getVendor(vendor.get)
    if (!v.isDefined) {
      return default
    } else {
      return s"${name} ${v.get.columnTypetoVendor(typeName, precision)}"
    }
  }

  def findVendorByUrl(url: String): Option[String] = {
    if (vendors == null) {
      return None
    }
    vendors.asScala.map(_.name).find(url.contains)
  }
}