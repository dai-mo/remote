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

package org.dcs.remote

import javax.servlet.ServletContextListener
import javax.servlet.ServletContextEvent
import java.util.logging.LogManager
import java.util.logging.Level

class StartupListener extends ServletContextListener {   

    def contextInitialized(event: ServletContextEvent) = {
        // The atmosphere framework (included in vaadin)
        // uses java.util.logging, which can be tricky to control
        // a an app (like dcs) which uses slf4j. For this reason,
        // jul is reset below to the FINEST log level
        // TODO: Investigate the possibility and the performance cost in using http://www.slf4j.org/legacy.html
        LogManager.getLogManager().reset();
        java.util.logging.Logger.getLogger("global").setLevel(Level.FINEST);              
    }

    def contextDestroyed(event: ServletContextEvent) = {
    	ZkRemoteService.close
    }
  
}