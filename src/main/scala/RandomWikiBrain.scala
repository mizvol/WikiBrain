import ch.epfl.lts2.Utils.suppressLogs
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by volodymyrmiz on 17.01.18.
  */
object RandomWikiBrain {
  def main(args: Array[String]): Unit = {
    suppressLogs(List("org", "akka"))

    val log: Logger = LoggerFactory.getLogger(this.getClass)
  }
}
