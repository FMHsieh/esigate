/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.esigate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import org.esigate.impl.IndexedInstances;
import org.esigate.impl.UriMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class used to configure and retrieve {@linkplain Driver} INSTANCIES.
 * 
 * @author Stanislav Bernatskyi
 * @author Francois-Xavier Bonnet
 * @author Nicolas Richeton
 */
public class DriverFactory {
	private static IndexedInstances INSTANCES = new IndexedInstances(new HashMap<String, Driver>());
	private static final String DEFAULT_INSTANCE_NAME = "default";
	private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);

	static {
		// Load default settings
		configure();
	}

	private DriverFactory() {

	}

	/** Loads all instances according to default configuration file */
	public final static void configure() {
		InputStream inputStream = null;
		InputStream extInputStream = null;

		try {
			// Load from environment
			String envPath = System.getProperty("esigate.config");
			if (envPath != null) {
				try {
					LOG.info("Scanning configuration {}", envPath);
					inputStream = new FileInputStream(new File(envPath));
				} catch (FileNotFoundException e) {
					LOG.error("Can't read file {} (from -Desigate.config)", envPath, e);
				}
			}

			if (inputStream == null) {
				LOG.info("Scanning configuration {}", "/esigate.properties");
				inputStream = DriverFactory.class.getResourceAsStream("/esigate.properties");
			}

			// For backward compatibility
			if (inputStream == null) {
				LOG.info("Scanning configuration /{}/{}", DriverFactory.class.getPackage().getName().replace(".", "/"),
						"driver.properties");
				inputStream = DriverFactory.class.getResourceAsStream("driver.properties");
			}
			if (inputStream == null) {
				LOG.info("Scanning configuration {}", "/net/webassembletool/driver.properties");
				inputStream = DriverFactory.class.getResourceAsStream("/net/webassembletool/driver.properties");
			}

			if (inputStream == null)
				throw new ConfigurationException("esigate.properties configuration file was not found in the classpath");

			// load driver-ext.properties if exists
			LOG.info("Scanning configuration {}", "/esigate-ext.properties");
			extInputStream = DriverFactory.class.getClassLoader().getResourceAsStream("/esigate-ext.properties");

			// For backward compatibility
			if (extInputStream == null) {
				LOG.info("Scanning configuration {}", "/driver-ext.properties");
				extInputStream = DriverFactory.class.getResourceAsStream("/driver-ext.properties");
			}
			if (extInputStream == null) {
				LOG.info("Scanning configuration /{}/{}", DriverFactory.class.getPackage().getName().replace(".", "/"),
						"driver-ext.properties");
				extInputStream = DriverFactory.class.getResourceAsStream("driver-ext.properties");
			}

			Properties merged = new Properties();
			if (inputStream != null) {
				Properties props = new Properties();
				props.load(inputStream);
				merged.putAll(props);
			}

			if (extInputStream != null) {
				Properties extProps = new Properties();
				extProps.load(extInputStream);
				merged.putAll(extProps);
			}

			configure(merged);
		} catch (IOException e) {
			throw new ConfigurationException("Error loading configuration", e);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}

				if (extInputStream != null) {
					extInputStream.close();
				}
			} catch (IOException e) {
				throw new ConfigurationException("failed to close stream", e);
			}
		}
	}

	/**
	 * Loads all instancies according to the properties parameter
	 * 
	 * @param props
	 *            properties to use for configuration
	 */
	public final static void configure(Properties props) {
		Properties defaultProperties = new Properties();
		HashMap<String, Properties> driversProps = new HashMap<String, Properties>();
		for (Enumeration<?> enumeration = props.propertyNames(); enumeration.hasMoreElements();) {
			String propertyName = (String) enumeration.nextElement();
			String value = props.getProperty(propertyName);
			int idx = propertyName.lastIndexOf('.');
			if (idx < 0) {
				defaultProperties.put(propertyName, value);
			} else {
				String prefix = propertyName.substring(0, idx);
				String name = propertyName.substring(idx + 1);
				Properties driverProperties = driversProps.get(prefix);
				if (driverProperties == null) {
					driverProperties = new Properties();
					driversProps.put(prefix, driverProperties);
				}
				driverProperties.put(name, value);
			}
		}
		// Merge with default properties
		synchronized (INSTANCES) {
			INSTANCES.getInstances().clear();
			for (Entry<String, Properties> entry : driversProps.entrySet()) {
				String name = entry.getKey();
				Properties properties = new Properties();
				properties.putAll(defaultProperties);
				properties.putAll(entry.getValue());
				configure(name, properties);
			}
			if (INSTANCES.getInstances().get(DEFAULT_INSTANCE_NAME) == null
					&& Parameters.REMOTE_URL_BASE.getValueString(defaultProperties) != null) {
				configure(DEFAULT_INSTANCE_NAME, defaultProperties);
			}

		}
	}

	/**
	 * Registers new {@linkplain Driver} under provided name with specified
	 * properties.
	 * 
	 * @param name
	 * @param props
	 */
	public static void configure(String name, Properties props) {
		INSTANCES.getInstances().put(name, new Driver(name, props));

		// Update mapping index.
		INSTANCES = new IndexedInstances(INSTANCES.getInstances());
	}

	/**
	 * Retrieve the Driver instance which should process the request as
	 * described in the parameters, based on the mappings declared in
	 * configuration.
	 * 
	 * @param scheme
	 *            The scheme of the request : http or https
	 * @param host
	 *            The host of the request, as provided in the Host header of the
	 *            HTTP protocol
	 * @param url
	 *            The requested url
	 * @return
	 * @throws HttpErrorPage
	 */
	public static Driver getInstanceFor(String scheme, String host, String url) throws HttpErrorPage {
		for (UriMapping mapping : INSTANCES.getUrimappings().keySet()) {
			if (mapping.matches(scheme, host, url)) {
				return getInstance(INSTANCES.getUrimappings().get(mapping));
			}
		}

		// If no match, return default instance.
		throw new HttpErrorPage(404, "Not found", "No mapping defined for this url.");
	}

	/**
	 * Retrieves the default instance of this class that is configured according
	 * to the properties file (driver.properties)
	 * 
	 * @param instanceName
	 *            The name of the instance (corresponding to the prefix in the
	 *            driver.properties file)
	 * 
	 * @return the named instance
	 */
	public final static Driver getInstance(String instanceName) {
		synchronized (INSTANCES) {
			if (instanceName == null)
				instanceName = DEFAULT_INSTANCE_NAME;
			if (INSTANCES.getInstances().isEmpty()) {
				throw new ConfigurationException(
						"Driver has not been configured and driver.properties file was not found");
			}
			Driver instance = INSTANCES.getInstances().get(instanceName);
			if (instance == null) {
				throw new ConfigurationException("No configuration properties found for factory : " + instanceName);
			}
			return instance;
		}
	}

	/**
	 * Retrieves the default instance of this class that is configured according
	 * to the properties file (driver.properties)
	 * 
	 * @return the default instance
	 */
	public final static Driver getInstance() {
		return getInstance(DEFAULT_INSTANCE_NAME);
	}

	/**
	 * Method used to inject providers. Usefull mainly for unit testing purpose
	 * 
	 * @param instanceName
	 *            The name of the provider
	 * @param instance
	 *            The instance
	 */
	public final static void put(String instanceName, Driver instance) {
		synchronized (INSTANCES) {
			INSTANCES.getInstances().put(instanceName, instance);
			// Update mapping index.
			INSTANCES = new IndexedInstances(INSTANCES.getInstances());
		}
	}

	/**
	 * Ensure configuration has been loaded at least once. Helps to prevent
	 * delay on first call because of initialization.
	 */
	public static void ensureConfigured() {
		// Just trigger static init.
	}

}
