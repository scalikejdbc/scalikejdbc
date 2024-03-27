package scalikejdbc.orm.settings

object ORMEnv extends ORMEnv

/**
 * ScalikeJDBC Env value (key: "scalikejdbc.env")
 */
trait ORMEnv {

  /**
   * Key for env variables
   */
  val PropertyKey = "scalikejdbc.env"
  val PropertyAppEnvKey = "app.env"
  val EnvKey = "SCALIKEJDBC_ENV"
  val AppEnvKey = "APP_ENV"

  val Development = "development"
  val Dev = "dev"

  val Test = "test"

  val Staging = "staging"
  val QA = "qa"

  val Production = "production"
  val Prod = "prod"

  /**
   * Env value from system property (you can pass by JVM options or on runtime) or environment value.
   *
   * @return env string such as "production"
   */
  def get(): Option[String] = {
    Option(System.getProperty(PropertyKey))
      .orElse(Option(System.getProperty(PropertyAppEnvKey)))
      .orElse(Option(System.getProperty(EnvKey)))
      .orElse(Option(System.getProperty(AppEnvKey)))
      .orElse(Option(System.getenv(EnvKey)))
      .orElse(Option(System.getenv(AppEnvKey)))
  }

  def getOrElse(default: String): String = get().getOrElse(default)

  def getOrDevelopment(): String = get().getOrElse(Development)

  /**
   * Predicates current env is "development" or "dev".
   *
   * @return true/false
   */
  def isDevelopment(env: Option[String] = get()): Boolean =
    env.isEmpty || env.exists(e => e == Development || e == Dev)

  /**
   * Predicates current env is "test".
   *
   * @return true/false
   */
  def isTest(env: Option[String] = get()): Boolean = env.exists(e => e == Test)

  /**
   * Predicates current env is "staging" or "qa".
   *
   * @return true/false
   */
  def isStaging(env: Option[String] = get()): Boolean =
    env.exists(e => e == Staging || e == QA)

  /**
   * Predicates current env is "production" or "prod".
   *
   * @return true/false
   */
  def isProduction(env: Option[String] = get()): Boolean =
    env.exists(env => env == Production || env == Prod)

}
