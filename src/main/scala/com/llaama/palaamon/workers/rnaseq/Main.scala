/*
 * Copyright (C) 2020 LLAAMA <https://www.llaama.com>
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
 */

package com.llaama.palaamon.workers.rnaseq

import com.llaama.palaamon.core.transforms.cluster.AddWorkerClusterClient
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import org.slf4j.LoggerFactory

object Main extends App {
  Kamon.init()

  val logger = LoggerFactory.getLogger(this.getClass)
  println(args.mkString(" ; "))
  println(s"config environment file: ${System.getProperty("config.resource")}")

  val conf = ConfigFactory.load()

  val clusterClientAdd = new AddWorkerClusterClient("rnaseq123-actor-system", conf)

  clusterClientAdd.addWorkers(Rnaseq123.definition, 2)
}
                                                