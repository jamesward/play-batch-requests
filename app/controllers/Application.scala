package controllers

import play.api._
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.ws.{WS, Response}
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action.async { implicit request =>
    
    // ints 1 to 10000
    val numbers: Seq[Int] = 1 to 10000

    // split into groups of 256
    val batches: Iterator[Seq[Int]] = numbers.grouped(256)
    
    // futures for all of the response strings
    val futureResponses = batches.foldLeft(Future.successful(Seq.empty[String]))(processBatch)

    futureResponses.map { responses =>
      Ok(responses.toString())
    }
    
  }

  private def processBatch(results: Future[Seq[String]], batch: Seq[Int])(implicit request: Request[AnyContent]): Future[Seq[String]] = {
    results.flatMap { responses =>
      // create the web requests for this batch
      val batchFutures: Seq[Future[Response]] = batch.map(num => WS.url(routes.Application.echo(num).absoluteURL()).get())
      // sequence the futures for this batch into a singe future
      val batchFuture: Future[Seq[Response]] = Future.sequence(batchFutures)
      // when this batch is complete, append the responses to the existing responses
      batchFuture.map { batchResponses =>
        Logger.info("Finished a batch")
        responses ++ batchResponses.map(_.body)
      }
    }
  }
  
  def echo(num: Int) = Action {
    Logger.info(s"num=$num")
    Ok(num.toString)
  }
  
}