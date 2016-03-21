package org.dcs.remote.config;

import java.io.File;
import java.io.InputStream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Created by cmathew on 29/01/16.
 */

public class ConfigurationFacade {

	private static final Logger logger = LoggerFactory.getLogger(ConfigurationFacade.class);
	private ObjectMapper mapper;
	private static final String DEFAULT_CONFIG_FILE_NAME = "/config.yaml";
	private static final String CONFIG_FILE_KEY = "config";

	private Configuration configuration;

	private static ConfigurationFacade configurationFacade;

	private String configFilePath;

	private ConfigurationFacade() throws Exception {
		loadDataConfiguration();
	}

	public static ConfigurationFacade getInstance() {
		if(configurationFacade == null) {
			try {
				configurationFacade = new ConfigurationFacade();
			} catch (Exception e) {
				logger.warn("Could not load configuration due to " + e.getMessage() + " - loading defaults");
			}
		}		
		return configurationFacade;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public static Configuration getCurrentConfiguration() {
		return getInstance().getConfiguration();
	}


	public void loadDataConfiguration() throws Exception {
		try {
			mapper = new ObjectMapper(new YAMLFactory());

			configFilePath = System.getProperty(CONFIG_FILE_KEY);

			if(configFilePath == null) {
				InputStream inputStream = this.getClass().getResourceAsStream(DEFAULT_CONFIG_FILE_NAME);				
				if(inputStream == null) {
					throw new IllegalStateException("Could not load config file");
				} else {
					configuration = mapper.readValue(inputStream, Configuration.class);    
				}
			} else {
				File configFile = new File(configFilePath);
				logger.warn("Config file path : " + configFilePath);
				configuration = mapper.readValue(configFile, Configuration.class);      
			}    			
		} catch (Exception e) {
			throw e;
		}
	}



}
