package mesosphere.marathon
package core.launchqueue

import akka.Done
import mesosphere.marathon.core.instance.update.InstanceChange
import mesosphere.marathon.core.launcher.OfferMatchResult
import mesosphere.marathon.core.launchqueue.LaunchQueue.QueuedInstanceInfoWithStatistics
import mesosphere.marathon.state.{PathId, RunSpec, Timestamp}
import mesosphere.mesos.NoOfferMatchReason

import scala.collection.immutable.Seq
import scala.concurrent.Future

object LaunchQueue {

  /**
    * @param runSpec the associated runSpec
    * @param inProgress true if the launch queue currently tries to launch more instances
    * @param instancesLeftToLaunch number of instances to launch
    * @param finalInstanceCount the final number of instances currently targeted
    * @param backOffUntil timestamp until which no further launch attempts will be made
    */
  case class QueuedInstanceInfo(
      runSpec: RunSpec,
      inProgress: Boolean,
      instancesLeftToLaunch: Int,
      finalInstanceCount: Int,
      backOffUntil: Timestamp,
      startedAt: Timestamp)

  case class QueuedInstanceInfoWithStatistics(
      runSpec: RunSpec,
      inProgress: Boolean,
      instancesLeftToLaunch: Int,
      finalInstanceCount: Int,
      backOffUntil: Timestamp,
      startedAt: Timestamp,
      rejectSummaryLastOffers: Map[NoOfferMatchReason, Int],
      rejectSummaryLaunchAttempt: Map[NoOfferMatchReason, Int],
      processedOffersCount: Int,
      unusedOffersCount: Int,
      lastMatch: Option[OfferMatchResult.Match],
      lastNoMatch: Option[OfferMatchResult.NoMatch],
      lastNoMatches: Seq[OfferMatchResult.NoMatch]
  )
}

/**
  * The LaunchQueue contains requests to launch new instances for a run spec. For every method returning T
  * there is a corresponding async method which returns a Future[T]. Async methods should be preferred
  * where synchronous methods will be deprecated and gradually removed.
  */
trait LaunchQueue {

  /** Returns all entries of the queue with embedded statistics */
  def listWithStatistics: Future[Seq[QueuedInstanceInfoWithStatistics]]

  /** Update the run spec in a task launcher actor. **/
  def sync(spec: RunSpec): Future[Done]

  /** Remove all instance launch requests for the given PathId from this queue. */
  def purge(specId: PathId): Future[Done]

  /** Add delay to the given RunnableSpec because of a failed instance */
  def addDelay(spec: RunSpec): Unit

  /** Reset the backoff delay for the given RunnableSpec. */
  def resetDelay(spec: RunSpec): Unit

  /** Advance the reference time point of the delay for the given RunSpec */
  def advanceDelay(spec: RunSpec): Unit

  /** Notify queue about InstanceUpdate */
  def notifyOfInstanceUpdate(update: InstanceChange): Future[Done]
}
