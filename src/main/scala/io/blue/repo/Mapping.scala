package io.blue.repo

import java.sql.{ResultSet, ResultSetMetaData, JDBCType, Connection=>SqlConnection}
import io.blue.config.Config
import com.typesafe.scalalogging.LazyLogging
import me.tongfei.progressbar._

class Mapping extends LazyLogging {

  var name: String  = _
  var sourceConnection: Connection = _
  var targetConnection: Connection = _
  var sourceTable: String = _
  var targetTable: String = _
  var sourceHint: String = _
  var targetHint: String = _
  var filter: String = _
  var batchSize: Int = 1000
  var dropCreate: Boolean = false
  var columnMappings: List[(String, String)] = List()

  private lazy val sourceConn: SqlConnection = sourceConnection.connect
  private lazy val targetConn: SqlConnection = targetConnection.connect

  def stop {
    logger.debug("Cleaning connections ...")
    if (sourceConn != null) { sourceConn.close}
    if (targetConn != null) { targetConn.close}
  }

  def rollback {
    if (targetConn != null) { targetConn.rollback }
  }

  private def count(conn: SqlConnection, table: String, hint: String = ""): Long = {
    val query = s"""
      select ${if (hint != null) hint else ""}
        count(1) record_count
      from ${table}
      ${ if (filter != null) "where\n" + filter else ""}
    """
    logger.debug(query)
    val rs = conn.createStatement.executeQuery(query)
    rs.next
    val result = rs.getLong("record_count")
    rs.close
    result
  }

  private lazy val sourceSql = s"""
    select ${if (sourceHint != null) sourceHint else ""}
      ${columnMappings.map(_._1).mkString(",\n")}
    from ${sourceTable}
    ${ if (filter != null) "where\n" + filter else ""}
  """

  private lazy val targetSql = s"""
    insert ${if(targetHint != null) targetHint else ""} into ${targetTable} (
      ${columnMappings.map(_._2).mkString(",")}
    ) values (
      ${columnMappings.map(_ => "?").mkString(",")}
    )
  """

  def createTargetTable {
    logger.info(s"Creating target table ${targetTable} ...")
    targetConn.setAutoCommit(false)
    logger.debug(sourceSql)
    val rs = sourceConn.createStatement.executeQuery(sourceSql)
    createTargetTable(rs.getMetaData)
    rs.close
    logger.info("Done")
  }

  def run = {
    targetConn.setAutoCommit(false)
    val recordCount = count(sourceConn, sourceTable, sourceHint)
    logger.debug(s"Source Query: \n${sourceSql}")

    val sourceStmt = sourceConn.createStatement
    sourceStmt.setFetchSize(batchSize)
    var rs = sourceStmt.executeQuery(sourceSql)

    if (dropCreate) {
      dropTargetTable(targetTable)
      createTargetTable(rs.getMetaData)
    } else {
      truncateTargetTable(targetTable)
    }

    logger.debug(s"Target Query: \n${targetSql}")

    val targetStmt = targetConn.prepareStatement(targetSql)

    var batchIndex: Long = 0
    val pb = new ProgressBar(name, recordCount)
    while (rs.next) {
      columnMappings.zipWithIndex.foreach{ case(_, i) =>
        targetStmt.setObject(i+1, rs.getObject(i+1))
      }
      targetStmt.addBatch
      batchIndex += 1
      if (batchIndex >= batchSize) {
        targetStmt.executeBatch
        batchIndex = 0
      }
      pb.step
    }

    if (batchIndex > 0) {
      targetStmt.executeBatch
    }
    pb.stop
    targetConn.commit

    stop
  }

  private def createTargetTable(rsmd: ResultSetMetaData) {
    val columns= columnMappings.map(_._2)
    val vendor = Config.instance.findVendorByUrl(targetConnection.url)
    val colums = 1.to(rsmd.getColumnCount).map { i =>
      var columnJdbcTypeName = JDBCType.valueOf(rsmd.getColumnType(i)).toString
      s"${Config.instance.columnTypetoVendor(vendor, columns(i-1), columnJdbcTypeName, rsmd.getPrecision(i))}"
    }.mkString(",\n")

    var sql = s"""
      create table ${targetTable} (
        ${colums}
      )
    """
    logger.debug(sql)
    targetConn.prepareStatement(sql).executeUpdate
    targetConn.commit
  }

  private def dropTargetTable(table: String) {
    try {
      var stmt = targetConn.prepareStatement(s"drop table ${table}")
      stmt.executeUpdate
      targetConn.commit
    } catch {
      case e: Exception =>
        targetConn.rollback
        logger.debug(s"Failed to drop ${table}, may not exists!")
    }
  }

  private def truncateTargetTable(table: String) {
    logger.debug("Truncating target table ...")
    var sql = s"truncate table ${table}"
    logger.debug(sql)
    targetConn.prepareStatement(sql).executeUpdate
    targetConn.commit
  }

}