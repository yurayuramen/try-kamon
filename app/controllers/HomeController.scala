package controllers

import javax.inject._
import play.api._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(right:Option[String]) = Action {
    
    
    if( right.map(_ == "yes").getOrElse(false) )
      Ok(views.html.index())
    else
      Ok("page failed!!")
  }
  def sleep(sec:Option[Int])=Action{
    val sleepMsec = sec.getOrElse(1) * 1000
    Thread.sleep(sleepMsec)
    Ok(s"${sleepMsec}ミリ秒経過...")
  }

  
}
