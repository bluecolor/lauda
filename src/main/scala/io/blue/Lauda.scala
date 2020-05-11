package io.blue

import collection.JavaConverters._
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.util.concurrent.Callable
import me.tongfei.progressbar._

import io.blue.config.Config
import io.blue.repo.{Repository, Metadata, Mapping}
import com.typesafe.scalalogging.LazyLogging
import io.blue.config.{Repository => RepositoryConfig}

@Command(
  name = "lauda",
  mixinStandardHelpOptions = true,
  version = Array("lauda 0.1"),
  description = Array("Loads data between databases https://github.com/bluecolor/lauda")
)
class Lauda extends Callable[Long] with LazyLogging {

  @Parameters(index = "0", description = Array(
    "Command to exeucte",
    "See https://github.com/bluecolor/lauda#command-line-arguments"
  ))
  var command: String = _

  @Option(names = Array("-m"), description = Array("Name of the mapping"))
  var mappingName: String = _

  @Option(names = Array("-c"), description = Array("Name of the connection"))
  var connectionName: String = _

  @Option(names = Array("-r"), description = Array("Path to repository data file"))
  var repositoryDataFile: String = _


  override def call: Long = {
    val config = Config.read
    Metadata.vendors = config.vendors

    command match {
      case "repository.import" =>
        importRepositoryData(repositoryDataFile, config.repository)
      case "repository.up" =>
        repositoryUp(config.repository)
      case "repository.down" =>
        repositoryDown(config.repository)
      case "repository.print.connections" =>
        printConnections(config.repository)
      case "repository.print.mappings" =>
        printMappings(config.repository)
      case "repository.print.columns" =>
        printColumnMappings(mappingName, config.repository)
      case "mapping.delete" =>
        deleteMapping(mappingName, config.repository)
      case "mapping.exists" =>
        isMappingExists(mappingName, config.repository)
      case "mapping.run" =>
        runMapping(mappingName, config)
      case "mapping.create" =>
        createTable(mappingName, config)
      case "connection.delete" =>
        deleteConnection(connectionName, config.repository)
      case "connection.test" =>
        testConnection(connectionName, config.repository)
      case _ =>
        println("Unknown command")
    }

    return 0
  }

  private def repositoryUp(config: RepositoryConfig) {
    logger.info("Creating repository ...")
    try {
      Repository.connect(config)
      Repository.createRepository
    } catch {
      case e: Exception =>
        logger.error("Failed to create repository", e)
    } finally {
      Repository.rollback
      Repository.disconnect
    }
  }

  private def repositoryDown(config: RepositoryConfig) {
    logger.info("Dropping repository ...")
    try {
      Repository.connect(config)
      Repository.dropRepository
    } catch {
      case e: Exception =>
        logger.error("Failed to drop repository!", e)
    } finally {
      Repository.rollback
      Repository.disconnect
    }
  }

  private def printConnections(config: RepositoryConfig) {
    logger.info("Printing connections ...")
    try {
      Repository.connect(config)
      Repository.printConnections
    } catch {
      case e: Exception =>
        logger.error("Failed to print connections!", e)
    } finally {
      Repository.rollback
      Repository.disconnect
    }
  }

  private def printMappings(config: RepositoryConfig) {
    logger.info("Printing mappings ...")
    try {
      Repository.connect(config)
      Repository.printMappings
    } catch {
      case e: Exception =>
        logger.error("Failed to print mappings!", e)
    } finally {
      Repository.rollback
      Repository.disconnect
    }
  }

  private def printColumnMappings(mapping: String, config: RepositoryConfig) {
    logger.info("Printing column mappings ...")
    try {
      Repository.connect(config)
      Repository.printColumnMappings(mapping)
    } catch {
      case e: Exception =>
        logger.error("Failed to print column mappings!", e)
    } finally {
      Repository.rollback
      Repository.disconnect
    }
  }

  private def importRepositoryData(path: String, config: RepositoryConfig) {
    logger.info("Importing to repository ...")
    try {
      Repository.connect(config)
      Repository.importRepositoryData(path)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to import repository data ${path}", e)
    } finally {
      Repository.rollback
      Repository.disconnect
    }
  }

  private def deleteMapping(name: String, config: RepositoryConfig) {
    logger.info(s"Deleting mapping ${name}...")
    try {
      Repository.connect(config)
      Repository.deleteMapping(name)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to delete mapping ${name}", e)
    } finally {
      Repository.rollback
      Repository.disconnect
    }
  }

  private def isMappingExists(name: String, config: RepositoryConfig) {
    logger.info(s"Checking mapping ${name}...")
    try {
      Repository.connect(config)
      Repository.isMappingExists(name) match {
        case true => logger.info(s"Mapping exists")
        case _ => logger.info(s"Mapping does NOT exist")
      }
    } catch {
      case e: Exception =>
        logger.error(s"Failed to delete mapping ${name}", e)
    } finally {
      Repository.rollback
      Repository.disconnect
    }
  }

  private def deleteConnection(name: String, config: RepositoryConfig) {
    logger.info(s"Deleting connection ${name}...")
    try {
      Repository.connect(config)
      Repository.deleteConnection(name)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to delete connection ${name}", e)
    } finally {
      Repository.rollback
      Repository.disconnect
    }
  }

  private def testConnection(name: String, config: RepositoryConfig) {
    logger.info(s"Deleting connection ${name}...")
    try {
      Repository.connect(config)
      Repository.testConnection(name) match {
        case true => logger.info("Success")
        case _ => logger.info("Error")
      }
    } catch {
      case e: Exception =>
        logger.error(s"Failed to test connection ${name}", e)
    } finally {
      Repository.disconnect
    }
  }

  private def createTable(mappingName: String, config: Config) {
    var mapping: scala.Option[Mapping] = None

    try {
      Repository.connect(config.repository)
      mapping = Repository.findMapping(mappingName)
      mapping match {
        case Some(m) => m.createTargetTable
        case None => println(s"Mapping not found ${mapping}")
      }
    } catch {
      case e: Exception =>
        mapping match { case Some(m) => m.rollback case None => }
        logger.error(s"Failed to create table", e)
    } finally {
      Repository.disconnect
      mapping match {
        case Some(m) => m.stop
        case None =>
      }
    }
  }

  private def runMapping(name: String, config: Config) {
    var mapping: scala.Option[Mapping] = None

    try {
      Repository.connect(config.repository)
      mapping = Repository.findMapping(name)
      mapping match {
        case Some(m) => m.run
        case None => println(s"Mapping not found ${name}")
      }
    } catch {
      case e: Exception =>
        mapping match { case Some(m) => m.rollback case None => }
        logger.error(s"Failed to run mapping ${name}", e)
    } finally {
      Repository.disconnect
      mapping match {
        case Some(m) => m.stop
        case None =>
      }
    }
  }

}