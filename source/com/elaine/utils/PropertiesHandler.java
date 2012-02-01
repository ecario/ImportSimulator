package com.elaine.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.commons.lang.text.StrSubstitutor;


/*
 * In order to get this to work in Tomcat, needed to modify catalina.properties sharedLoader:
 * shared.loader=${catalina.home}/webapps/solr-import/WEB-INF/properties
 */
public class PropertiesHandler
{
	public static final String APPLICATION_PROPERTIES = "Application.properties";
	public static final String PRODUCT_PROPERTIES = "Product.properties";
	public static final String ENVIRONMENT_PROPERTIES = "Environment.properties";
	public static final String SECURE_PROPERTIES = "Secure.properties";
	public static final String OVERRIDE_PROPERTIES = "Override.properties";

	protected static Properties props = new Properties();

	public static Logger log = Logger.getLogger("com.wk.atlas");

	private static PropertiesHandler handler = null;
	//private String matchPath;

	public static PropertiesHandler getInstance()
	{
		if (handler == null)
			handler = new PropertiesHandler();
		return handler;
	}

	public static PropertiesHandler getInstance( List<String> matchpath )
	throws Exception
	{
		return ( new PropertiesHandler( matchpath ) );
	}

	private PropertiesHandler()
	{
		try
		{
			Class c = Class.forName(this.getClass().getName());

			InputStream is = c.getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES);
			if (is == null)
				throw new IOException("Application.properties does not exist!!");
			props.load(is);
			log.info("Application Properties loaded successfully!");
			is.close();

			is = c.getClassLoader().getResourceAsStream(PRODUCT_PROPERTIES);
			if (is == null)
				throw new IOException("Product.properties does not exist!!");
			props.load(is);
			log.info("Product Properties loaded successfully!");
			is.close();

			is = c.getClassLoader().getResourceAsStream(ENVIRONMENT_PROPERTIES);
			if (is == null)
				log.warn("No Environment Properties exist...continuing without it");
			else
			{
				props.load(is);
				log.info("Environment Properties loaded successfully!");
				is.close();
			}

			is = c.getClassLoader().getResourceAsStream(SECURE_PROPERTIES);
			if (is == null)
				log.warn("No Secure Properties exist...continuing without it");
			else
			{
				props.load(is);
				log.info("Secure Properties loaded successfully!");
				is.close();
			}

			is = c.getClassLoader().getResourceAsStream(OVERRIDE_PROPERTIES);
			if (is == null)
				log.debug("No Override.properties file exists...continuing without it");
			else
			{
				props.load(is);
				log.info("Override Properties loaded successfully!");
				is.close();
			}

			applyMacroSubstitution();

		}
		catch (Exception e)
		{
			log.error("Exception while trying to read properties files: " + e.toString());
			throw new RuntimeException(e.toString());
		}
	}

	private PropertiesHandler( List<String> paths )
	{
		try
		{
			InputStream is = null;
			Class c = Class.forName(this.getClass().getName());

			Enumeration<URL> enum1 = c.getClassLoader().getResources( APPLICATION_PROPERTIES );

			is = getInputStream( enum1, paths );
			if (is == null)
				throw new IOException("Application.properties does not exist!!");
			props.load(is);
			log.info("Application Properties loaded successfully!");
			is.close();


			enum1 = c.getClassLoader().getResources( PRODUCT_PROPERTIES );
			is = getInputStream( enum1, paths );
			if (is == null)
				throw new IOException("Product.properties does not exist!!");
			props.load(is);
			log.info("Product Properties loaded successfully!");
			is.close();

			enum1 = c.getClassLoader().getResources( ENVIRONMENT_PROPERTIES );
			is = getInputStream( enum1, paths );
			if (is == null)
				throw new IOException("Environment.properties does not exist!!");
			props.load(is);
			log.info("Environment Properties loaded successfully!");
			is.close();

			enum1 = c.getClassLoader().getResources( SECURE_PROPERTIES );
			is = getInputStream( enum1, paths );
			if (is == null)
				throw new IOException("Secure.properties does not exist!!");
			props.load(is);
			log.info("Secure Properties loaded successfully!");
			is.close();

			enum1 = c.getClassLoader().getResources( OVERRIDE_PROPERTIES );
			is = getInputStream( enum1, paths );
			if (is == null)
				log.debug("No Override.properties file exists...continuing without it");
			else
			{
				props.load(is);
				log.info("Override Properties loaded successfully!");
				is.close();
			}

			applyMacroSubstitution();
		}
		catch (Exception e)
		{
			log.error("Exception while trying to read properties files: " + e.toString());
			throw new RuntimeException(e.toString());
		}

	}

	private InputStream getInputStream( Enumeration<URL> enum1, List<String> paths ) 
		throws Exception
	{
		InputStream is = null;
		while( enum1.hasMoreElements() )
		{
			URL url = enum1.nextElement();
			String path = url.getPath();
			boolean foundpath = false;

			for( String s : paths )
			{
				if( path.contains( s ) )
				{
					is = url.openStream();
					foundpath = true;
					break;
				}
			}
			if( foundpath )
				break;
		}

		return is;
	}

	private void applyMacroSubstitution()
	{
		String OS_DRIVE = "";
		String OS_SLASH = "//";

		String OS = System.getProperty("os.name");
		OS = OS.toLowerCase();

		if (OS.startsWith("win"))
		{
			OS_SLASH = "///";
			OS_DRIVE="C:";		// override from properties, if exists
		}

		String OS_URI = OS_SLASH + OS_DRIVE;

		if (props.containsKey("OS_DRIVE"))
			OS_DRIVE = props.getProperty("OS_DRIVE");

		if (props.containsKey("OS_URI"))
			OS_URI = props.getProperty("OS_URI");

		Map valuesMap = new HashMap();
		valuesMap.put("OS_DRIVE", OS_DRIVE);
		valuesMap.put("OS_URI", OS_URI);

		StrSubstitutor sub = new StrSubstitutor(valuesMap);

		Iterator i = props.keySet().iterator();
		while (i.hasNext())
		{
			String key = (String)i.next();
			String value = props.getProperty(key);
			props.put(key, sub.replace(value));
		}

	}

	public Properties getAllProperties()
	{
		return props;
	}

	public Properties getAllPropertiesWithPrefix(String prefix)
	{
		Properties p = new Properties();
		Iterator i = props.keySet().iterator();
		while (i.hasNext())
		{
			String key = (String)i.next();
			if (key.startsWith(prefix))
				p.put(key, props.get(key));
		}
		return p;

	}

	public Properties getJNDIProperties()
	{
		Properties p = new Properties();
		Iterator i = props.keySet().iterator();
		while (i.hasNext())
		{
			String key = (String)i.next();
			if (key.startsWith("JNDI."))
			{
				String newKey = key.substring(5);		// remove "JNDI."
				p.put(newKey, props.get(key));
			}
		}
		return p;
	}

	public String getProperty(String key)
	{
		return props.getProperty(key);
	}

	public String getProperty(String key, String defaultValue)
	{
		if (props.getProperty(key) == null)
			return defaultValue;
		else
			return props.getProperty(key);
	}

	public String[] getPropertyArray(String key)
	{
		String value = getProperty(key);

		if (value == null)
			return null;

		Vector values = new Vector();
		StringTokenizer st = new StringTokenizer(value, ",");
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			values.add(token);
		}

		return (String[])values.toArray(new String[0]);

	}	

	protected String resolvePlaceholder(String placeholder, Properties props) 
	{
		// ignoring local props - use instance variable
		return this.props.getProperty(placeholder);					
	} 


	public PropertiesHandler reload()
	{
		handler = null;
		props = new Properties();
		return PropertiesHandler.getInstance();         
	} 


	public static void main(String[] args)
	throws Exception
	{
		PropertiesHandler handler = PropertiesHandler.getInstance();
		Properties props = handler.getAllProperties();

		Iterator i = props.keySet().iterator();
		while (i.hasNext())
		{
			String key = (String)i.next();
			String value = props.getProperty(key);
			System.out.println(key + "=" + value);
		}

		System.out.println("\nJNDI PROPERTIES\n");

		Properties jndiProperties = handler.getAllPropertiesWithPrefix("JNDI");
		Iterator i2 = jndiProperties.keySet().iterator();
		while (i2.hasNext())
		{
			String key = (String)i2.next();
			String value = props.getProperty(key);
			System.out.println(key + "=" + value);
		}


	}
}
