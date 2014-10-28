package app

final class Initializer {
  def run(url: String, user: String, password: String): Unit = {
    scalikejdbc.ConnectionPool.singleton(url, user, password)
  }
}
