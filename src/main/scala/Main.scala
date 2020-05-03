import collection.mutable._
import collection.JavaConverters._
import picocli.CommandLine

import io.blue.Lauda

object Main extends App {
  new CommandLine(new Lauda).execute(args: _*)
}