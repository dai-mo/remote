/*
 * Copyright (c) 2017-2018 brewlabs SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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

