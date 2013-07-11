package app.models.test

object Spec{

  def before(driver: String, url: String, user: String, pass: String){
    Class.forName(driver)
    scalikejdbc.ConnectionPool.singleton(url, user, pass)
  }

  def after(){
    scalikejdbc.ConnectionPool.closeAll()
  }

}
