package scalikejdbc

import scala.util.matching.Regex

/**
 * These constants allow us to generate RegExp once, and use them multiple times. 
 * Analysis with AWS Code Guru revealed that the continues construction of RegExp statements 
 * is a minor drain on resources, which can be easily mitigated by creating a 
 * RegExp once and storing it for future us.
 * 
 * Any RegExp that is used in multiple modules, making it easier to maintain and validate them
 */
object RegExpConstants {

  // This regex removes trailing $, as well as anything until the first $ or .
  val classNameRegExp: Regex = "\\$$|^.*[.$](?=.+)".r

}
