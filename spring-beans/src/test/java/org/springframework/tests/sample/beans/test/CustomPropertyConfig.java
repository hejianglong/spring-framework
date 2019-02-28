package org.springframework.tests.sample.beans.test;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class CustomPropertyConfig extends PropertyPlaceholderConfigurer {

	private Resource[] locations;

	private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

	@Override
	public void setLocations(Resource... locations) {
		this.locations = locations;
	}

	@Override
	public void setLocalOverride(boolean localOverride) {
		this.localOverride = localOverride;
	}

	@Override
	protected String convertProperty(String propertyName, String propertyValue) {
		return super.convertProperty(propertyName, propertyValue);
	}

	@Override
	protected void loadProperties(Properties props) throws IOException {
		if (locations != null) {
			for (Resource location : this.locations) {
				InputStream is = null;
				try {
					String filename = location.getFilename();
					String env = "application-" + System.getProperty("spring.profiles.active", "dev");

					if (filename.contains(env)) {
						is = location.getInputStream();
						this.propertiesPersister.load(props, is);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				} finally {
					if (is != null) {
						is.close();
					}
				}
			}
		}
	}
}
