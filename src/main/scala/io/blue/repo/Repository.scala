package io.blue.repo

import scala.util.{Try,Success,Failure}
import collection.JavaConverters._
import java.util.HashMap
import java.io.File
import java.sql.DriverManager
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.typesafe.scalalogging.LazyLogging
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import cats.kernel.Hash
import io.blue.repo.data.{ RepositoryData, Connection => ConnectionData, Mapping => MappingData}
import io.blue.helpers.Tabulator

object Repository extends LazyLogging {

  var conn: java.sql.Connection = _
  var config: io.blue.config.Repository = _


  def connect(config: io.blue.config.Repository) {
    this.config = config
    Class.forName(config.className)
    conn = DriverManager.getConnection(config.url, config.username, config.password)
    conn.setAutoCommit(false)
  }

  def disconnect {
    if (conn != null) {
      conn.close
    }
  }

  def commit { conn.commit }
  def rollback { if (conn != null) conn.rollback }


  def printConnections {
    println(Tabulator.formatTable(
      List("Name", "Url", "Username", "Password")::
      findAllConnections.map {c =>
        List(c.name, c.url, c.username, c.password)
      }
    ))
  }

  def printMappings {
    println(Tabulator.formatTable(
      List("Name", "SourceConnection", "TargetConnection", "SourceTable", "TargetTable")::
      findAllMappings.map{ m =>
        List(m.name, m.sourceConnection.name, m.targetConnection.name, m.sourceTable, m.targetTable)
      }
    ))
  }

  def printColumnMappings(mapping: String) {
    println(Tabulator.formatTable(
      List("Source", "Target")::
      findColumnMappings(mapping).map{ cm =>
        List(cm._1, cm._2)
      }
    ))
  }

  def testConnection(name: String): Boolean = {
    Try (findConnection(name).connect) match {
      case Success(conn) => conn.close; true
      case Failure(e) => logger.warn(s"Failed to connect to ${name}", e); false
    }
  }

  def deleteConnection(name: String) {
    val sql = s"delete from connections where name = '${name}'"
    conn.prepareStatement(sql).executeUpdate
    conn.commit
  }

  private def deleteColumnMappings(mapping: String) {
    val sql = s"delete from columns where mapping = '${mapping}'"
    conn.prepareStatement(sql).executeUpdate
  }

  def deleteMapping(name: String) {
    deleteColumnMappings(name)
    val sql = s"delete from mappings where name = '${name}'"
    conn.prepareStatement(sql).executeUpdate
    conn.commit
  }

  def isMappingExists(name: String): Boolean = {
    val query = s"select count(1) mapping_count from mappings where name = '${name}'"
    val rs = conn.createStatement.executeQuery(query)
    rs.next
    val result = rs.getInt("mapping_count") > 0
    rs.close
    result
  }

  def isConnectionExists(name: String): Boolean = {
    val query = s"select count(1) connection_count from connections where name = '${name}'"
    val rs = conn.createStatement.executeQuery(query)
    rs.next
    val result = rs.getInt("connection_count") > 0
    rs.close
    result
  }

  def isTargetColumnExists(mapping: String, name: String): Boolean = {
    val query = s"select count(1) column_count from columns where mapping = '${mapping}' and target = '${name}'"
    val rs = conn.createStatement.executeQuery(query)
    rs.next
    val result = rs.getInt("column_count") > 0
    rs.close
    result
  }


  def findMapping(name: String): Option[io.blue.repo.Mapping] = {

    if (!isMappingExists(name)) {
      return None
    }

    var mapping = new Mapping
    var query = s"""
      select
        source_connection, target_connection,
        source_table, target_table, source_hint, target_hint, filter,
        batch_size, drop_create
      from mappings
      where name = '${name}'
    """

    val rs = conn.createStatement.executeQuery(query)
    rs.next
    mapping.name = name
    val sourceConnection = rs.getString("source_connection")
    val targetConnection = rs.getString("target_connection")
    mapping.sourceTable = rs.getString("source_table")
    mapping.targetTable = rs.getString("target_table")
    mapping.sourceHint = rs.getString("source_hint")
    mapping.targetHint = rs.getString("target_hint")
    mapping.filter = rs.getString("filter")
    mapping.batchSize = rs.getInt("batch_size")
    mapping.dropCreate = rs.getInt("drop_create") > 0
    rs.close
    mapping.sourceConnection = findConnection(sourceConnection)
    mapping.targetConnection = findConnection(targetConnection)
    mapping.columnMappings = findColumnMappings(name)
    Some(mapping)
  }

  def findAllMappings = {
    var query = s"""
      select
        name, source_connection, target_connection,
        source_table, target_table, source_hint, target_hint, filter,
        batch_size, drop_create
      from mappings
    """
    val rs = conn.createStatement.executeQuery(query)
    var mappings: List[Mapping] = List()
    while(rs.next) {
      var mapping = new Mapping
      mapping.name = rs.getString("name")
      val sourceConnection = rs.getString("source_connection")
      val targetConnection = rs.getString("target_connection")
      mapping.sourceTable = rs.getString("source_table")
      mapping.targetTable = rs.getString("target_table")
      mapping.sourceHint = rs.getString("source_hint")
      mapping.targetHint = rs.getString("target_hint")
      mapping.filter = rs.getString("filter")
      mapping.batchSize = rs.getInt("batch_size")
      mapping.dropCreate = rs.getInt("drop_create") > 0
      mapping.sourceConnection = findConnection(sourceConnection)
      mapping.targetConnection = findConnection(targetConnection)
      mapping.columnMappings = findColumnMappings(mapping.name)
      mappings ::= mapping
    }
    rs.close
    mappings.sortBy(_.name)
  }

  def findConnection(name: String): io.blue.repo.Connection = {
    var query = s"""
      select url, username, password, class_name
      from connections
      where name = '${name}'
    """
    logger.debug(query)
    var connection = new Connection
    connection.name = name
    val rs = conn.createStatement.executeQuery(query)
    rs.next
    connection.url = rs.getString("url")
    connection.username = rs.getString("username")
    connection.password = rs.getString("password")
    connection.className= rs.getString("class_name")
    rs.close
    connection
  }

  def findAllConnections = {
    var query = s"""
      select name, url, username, password, class_name
      from connections
    """
    logger.debug(query)
    val rs = conn.createStatement.executeQuery(query)
    var connections: List[Connection] = List()
    while (rs.next) {
      var connection = new Connection
      connection.name = rs.getString("name")
      connection.url = rs.getString("url")
      connection.username = rs.getString("username")
      connection.password = rs.getString("password")
      connection.className= rs.getString("class_name")
      connections ::= connection
    }
    rs.close
    connections.sortBy(_.name)
  }

  def findColumnMappings(mapping: String) = {
    var query = s"""
      select source, target
      from columns
      where mapping = '${mapping}'
    """
    val rs = conn.createStatement.executeQuery(query)
    var columns: List[(String, String)] = List()
    while (rs.next) {
      columns :+=  (rs.getString("source"), rs.getString("target"))
    }
    rs.close
    columns.sortBy(_._2)
  }

  def createRepository {
    config.up.forEach{ migration =>
      logger.debug(migration)
      conn.createStatement.executeUpdate(migration)
    }
    conn.commit
  }

  def dropRepository {
    config.down.forEach{ migration =>
      logger.debug(migration)
      conn.createStatement.executeUpdate(migration)
    }
    conn.commit
  }

  def importRepositoryData(path: String) {
    val rd = RepositoryData.read(path)
    importConnections(rd.connections.asScala.toList)
    importMappings(rd.mappings.asScala.toList)
    conn.commit
  }

  private def importMappings(mappings: List[MappingData]) {
    var stmt = conn.prepareStatement("""
      insert into mappings (name, source_connection, target_connection, source_table,
        target_table, source_hint, target_hint, filter, batch_size, drop_create
      ) values (
        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
      )
    """)
    mappings.foreach{ m =>
      if(!isMappingExists(m.name)) {
        stmt.setString(1, m.name)
        stmt.setString(2, m.sourceConnection)
        stmt.setString(3, m.targetConnection)
        stmt.setString(4, m.sourceTable)
        stmt.setString(5, m.targetTable)
        stmt.setString(6, m.sourceHint)
        stmt.setString(7, m.targetHint)
        stmt.setString(8, m.filter)
        stmt.setInt(9, m.batchSize)
        stmt.setInt(10, if (m.dropCreate == true) 1 else 0)
        stmt.addBatch
      }
      importColumnMappings(m.name, m.sourceColumns.asScala.toList, m.targetColumns.asScala.toList)
    }
    stmt.executeBatch
  }

  private def importColumnMappings(mapping: String, sourceColumns: List[String], targetColumns: List[String]) {
    var stmt = conn.prepareStatement("""
      insert into columns (mapping, source, target) values (
        ?, ?, ?
      )
    """)
    sourceColumns.zipWithIndex.foreach{ case (cm, i) =>
      if(!isTargetColumnExists(mapping, targetColumns(i))) {
        stmt.setString(1, mapping)
        stmt.setString(2, cm)
        stmt.setString(3, targetColumns(i))
        stmt.addBatch
      }
    }
    stmt.executeBatch
  }

  private def importConnections(connections: List[ConnectionData]) {
    var stmt = conn.prepareStatement(s"""
      insert into connections (name, url, username, password, class_name) values (
        ?, ?, ?, ?, ?
      )
    """)
    connections.foreach{ c =>
      if(!isConnectionExists(c.name)) {
        stmt.setString(1, c.name)
        stmt.setString(2, c.url)
        stmt.setString(3, c.username)
        stmt.setString(4, c.password)
        stmt.setString(5, c.className)
        stmt.addBatch
      }
    }
    stmt.executeBatch
  }

}