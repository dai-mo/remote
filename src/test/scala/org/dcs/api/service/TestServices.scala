package org.dcs.api.service
import java.util.UUID

import org.dcs.api.processor.{ProcessorDefinition, RemoteProcessor, StateManager, StatefulRemoteProcessor}

import scala.collection.mutable.Map

/**
  * Created by cmathew on 04.04.17.
  */

trait TestServiceImpl {
  def testMethod() {}
}

trait MultiImplTestService {
  def testMethod()
}

trait SingleImplTestService {
  def testMethod()
}


class StatelessTestService extends RemoteProcessorService {
  def testMethod() {}

  override def getDef(processor: RemoteProcessor): ProcessorDefinition = null

  override def initialise(): RemoteProcessor = null
}

trait MockStateManager extends StateManager {
  val processorStateMap: Map[String, StatefulRemoteProcessor] = Map()

  override def put(processor: StatefulRemoteProcessor): String = ""

  override def get(processorStateId: String): Option[StatefulRemoteProcessor] = None

  override def remove(processorStateId: String): Boolean = true
}

class StatefulTestService extends StatefulRemoteProcessorService
  with MockStateManager {

  def testMethod() {}

  override def init(): String = null

  override def initialise(): RemoteProcessor = null

  override def getDef(processor: RemoteProcessor): ProcessorDefinition = null
}

