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