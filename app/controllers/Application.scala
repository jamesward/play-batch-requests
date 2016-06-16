package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.{WSClient, WSResponse}

class Application @Inject() (ws: WSClient)(implicit ec: ExecutionContext) extends Controller {

  def index = Action.async { implicit request =>

    // ints 1 to 10000
    val numbers: Seq[Int] = 1 to 10000

    // turn each number into a url
    val urls: Seq[String] = numbers.map(routes.Application.echo(_).absoluteURL())

    // split into groups of 256
    val batches: Iterator[Seq[String]] = urls.grouped(256)

    // futures for all of the response strings
    val futureResponses = batches.foldLeft(Future.successful(Seq.empty[String]))(processBatch)

    // render the list of responses when they are all complete
    futureResponses.map { responses =>
      Ok(responses.toString)
    }

  }

  private def processBatch(results: Future[Seq[String]], batch: Seq[String]): Future[Seq[String]] = {
    // when the results for the previous batches have been completed, start a new batch
    results.flatMap { responses =>
      // create the web requests for this batch
      val batchFutures: Seq[Future[String]] = batch.map(ws.url(_).get().map(_.body))

      // sequence the futures for this batch into a singe future
      val batchFuture: Future[Seq[String]] = Future.sequence(batchFutures)

      // when this batch is complete, append the responses to the existing responses
      batchFuture.map { batchResponses =>
        Logger.info("Finished a batch")
        responses ++ batchResponses
      }
    }
  }

  def echo(num: Int) = Action {
    Logger.info(s"num=$num")
    Ok(num.toString)
  }

}
