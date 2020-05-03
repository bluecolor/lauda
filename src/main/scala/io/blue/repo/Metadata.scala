package io.blue.repo

import java.util.HashMap
import collection.JavaConverters._

object Metadata {

  var vendors: HashMap[String, HashMap[String, HashMap[String, String]]] = _
  var sourceVendor: Option[String] = _
  var targetVendor: Option[String] = _

  def findVendorByUrl(url: String) = vendors.asScala.keys.find(url.contains)

  def getColumnExpression(vendor: Option[String], name: String, jdbcTypeName: String, precision: Int): String = {
    var expression = s"""${name} ${jdbcTypeName} ${if (precision > 0) "("+precision+")" else ""}"""

    if (!vendor.isDefined) {
      expression
    } else {
      vendors.asScala.get(vendor.get) match {
        case Some(vendor) =>
          vendor.asScala get "jdbc_type" match {
            case Some(jdbc_type) =>
              jdbc_type.asScala get jdbcTypeName.toLowerCase match {
                case Some(matchingType) =>
                  s"""${name} ${matchingType} ${if (precision > 0) "("+precision+")" else ""}"""
                case None => expression
              }
            case None => expression
          }
        case None => expression
      }
    }
  }

}

