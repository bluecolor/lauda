package io.blue

import collection.JavaConverters._
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.util.concurrent.Callable
import me.tongfei.progressbar._

import io.blue.config.Config
import io.blue.repo.{Repository, Mapping}
import com.typesafe.scalalogging.LazyLogging
import io.blue.config.{Repository => RepositoryConfig}

@Command(
  name = "lauda",
  mixinStandardHelpOptions = true,
  version = Array("lauda 0.1"),
  description = Array(
    "Loads data between databases https://github.com/bluecolor/lauda"
  )
)
class Lauda extends Callable[Int] with LazyLogging {

  val SUCCESS = 0
  val ERROR = 1

  @Parameters(
    index = "0",
    description = Array(
      "Command to exeucte",
      "See https://github.com/bluecolor/lauda#command-line-arguments"
    )
  )
  var command: String = _

  @Parameters(
    index = "1..*",
    arity = "0..1",
    hidden = true,
    description = Array(
      "command parameters"
    )
  )
  var params: Array[String] = _

  @Option(names = Array("--source-table"), description = Array("Source table name. can be schema.table_name or just table_name"))
  var sourceTable: String = _

  @Option(names = Array("--target-table"), description = Array("Target table name. can be schema.table_name or just table_name"))
  var targetTable: String = _

  @Option(names = Array("--source-connection"), description = Array("Source connection name"))
  var sourceConnection: String = _

  @Option(names = Array("--target-connection"), description = Array("Target connection name"))
  var targetConnection: String = _

  @Option(names = Array("--columns"), description = Array("Comma seperated list of columns. Optional"))
  var columns: String = _

  def p(index: Int): String = {
    var param: String = ""
    try {
      param = params(index)
    } catch {
      case e: NullPointerException =>
        logger.error("Missing parameter")
        System.exit(1)
    }
    param
  }

  override def call: Int = {
    val config = Config.read

    val result = command match {
      case "repository.import" =>
        importRepositoryData(p(0), config.repository)
      case "repository.up" =>
        repositoryUp(config.repository)
      case "repository.down" =>
        repositoryDown(config.repository)
      case "print.connections" =>
        printConnections(config.repository)
      case "print.mappings" =>
        printMappings(config.repository)
      case "print.columns" =>
        printColumnMappings(p(0), config.repository)
      case "mapping.generate" =>
        generateMapping(p(0), sourceTable, targetTable, sourceConnection, targetConnection, columns, config.repository)
      case "mapping.delete" =>
        deleteMapping(p(0), config.repository)
      case "mapping.exists" =>
        isMappingExists(p(0), config.repository)
      case "mapping.run" =>
        runMapping(p(0), config)
      case "mapping.create" =>
        createTable(p(0), config)
      case "connection.delete" =>
        deleteConnection(p(0), config.repository)
      case "connection.test" =>
        testConnection(p(0), config.repository)
      case _ =>
        println("Unknown command")
        ERROR
    }

    return result
  }

  private def repositoryUp(config: RepositoryConfig) = {
    var result = ERROR
    logger.info("Creating repository ...")
    try {
      Repository.connect(config)
      Repository.createRepository
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error("Failed to create repository", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def repositoryDown(config: RepositoryConfig) = {
    var result = ERROR
    logger.info("Dropping repository ...")
    try {
      Repository.connect(config)
      Repository.dropRepository
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error("Failed to drop repository!", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def printConnections(config: RepositoryConfig) = {
    var result = ERROR
    logger.info("Printing connections ...")
    try {
      Repository.connect(config)
      Repository.printConnections
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error("Failed to print connections!", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def printMappings(config: RepositoryConfig) = {
    var result = ERROR
    logger.info("Printing mappings ...")
    try {
      Repository.connect(config)
      Repository.printMappings
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error("Failed to print mappings!", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def printColumnMappings(mapping: String, config: RepositoryConfig) = {
    var result = ERROR
    logger.info("Printing column mappings ...")
    try {
      Repository.connect(config)
      Repository.printColumnMappings(mapping)
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error("Failed to print column mappings!", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def importRepositoryData(path: String, config: RepositoryConfig) = {
    var result = ERROR
    logger.info("Importing to repository ...")
    try {
      Repository.connect(config)
      Repository.importRepositoryData(path)
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error(s"Failed to import repository data ${path}", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def generateMapping(
    name: String,
    sourceTable: String,
    targetTable: String,
    sourceConnection: String,
    targetConnection: String,
    columns: String,
    config: RepositoryConfig
  ) = {
    var result = ERROR
    try {
      Repository.connect(config)
      Repository.generateMapping(name, sourceTable, targetTable, sourceConnection, targetConnection, columns)
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error(s"Failed to generate mapping for ${name}", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def deleteMapping(name: String, config: RepositoryConfig) = {
    var result = ERROR
    logger.info(s"Deleting mapping ${name}...")
    try {
      Repository.connect(config)
      Repository.deleteMapping(name)
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error(s"Failed to delete mapping ${name}", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def isMappingExists(name: String, config: RepositoryConfig) = {
    var result = ERROR
    logger.info(s"Checking mapping ${name}...")
    try {
      Repository.connect(config)
      Repository.isMappingExists(name) match {
        case true => logger.info(s"Mapping exists")
        case _    => logger.info(s"Mapping does NOT exist")
      }
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error(s"Failed to delete mapping ${name}", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def deleteConnection(name: String, config: RepositoryConfig) = {
    var result = ERROR
    logger.info(s"Deleting connection ${name}...")
    try {
      Repository.connect(config)
      Repository.deleteConnection(name)
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error(s"Failed to delete connection ${name}", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def testConnection(name: String, config: RepositoryConfig) = {
    var result = ERROR
    logger.info(s"Deleting connection ${name}...")
    try {
      Repository.connect(config)
      Repository.testConnection(name) match {
        case true => logger.info("Success")
        case _    => logger.info("Error")
      }
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        logger.error(s"Failed to test connection ${name}", e)
        Repository.rollback
    } finally {
      Repository.disconnect
    }
    result
  }

  private def createTable(mappingName: String, config: Config) = {
    var result = ERROR
    var mapping: scala.Option[Mapping] = None

    try {
      Repository.connect(config.repository)
      mapping = Repository.findMapping(mappingName)
      mapping match {
        case Some(m) => m.createTargetTable
        case None    => println(s"Mapping not found ${mapping}")
      }
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        mapping match {
          case Some(m) => m.rollback
          case None    =>
        }
        logger.error(s"Failed to create table", e)
        Repository.rollback
    } finally {
      Repository.disconnect
      mapping match {
        case Some(m) => m.stop
        case _       =>
      }
    }
    result
  }

  private def runMapping(name: String, config: Config) = {
    var result = ERROR
    var mapping: scala.Option[Mapping] = None

    try {
      Repository.connect(config.repository)
      mapping = Repository.findMapping(name)
      mapping match {
        case Some(m) => m.run
        case None    => println(s"Mapping not found ${name}")
      }
      Repository.commit
      result = SUCCESS
    } catch {
      case e: Exception =>
        mapping match {
          case Some(m) => m.rollback
          case None    =>
        }
        logger.error(s"Failed to run mapping ${name}", e)
        Repository.rollback
        System.exit(1)
    } finally {
      Repository.disconnect
      mapping match {
        case Some(m) => m.stop
        case None    =>
      }
    }
    result
  }

}
