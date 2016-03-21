package org.dcs.remote.config;

public class Configuration {
	
	private String zookeeperServers = "localhost:2181";

	public String getZookeeperServers() {
		return zookeeperServers;
	}

	public void setZookeeperServers(String zookeeperServers) {
		this.zookeeperServers = zookeeperServers;
	}

}
