package com.elaine.importer;

import java.io.File;
import java.io.FileFilter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;

import com.elaine.utils.PropertiesHandler;

public class ImportSimulator
{
	HttpClient httpclient = new DefaultHttpClient();
	long totalTime = 0;
	public static final int BATCH_SIZE = 2000;
	public static PropertiesHandler props = PropertiesHandler.getInstance();
	
	public ImportSimulator()
	{	}
	
	protected String indexDocuments(String requestXML)
		throws Exception
	{
		String importFEURL = props.getProperty("importfe.url");
		HttpPost post = new HttpPost(importFEURL);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("data", requestXML));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
		post.setEntity(entity);
		
		System.out.println("executing request " + post.getURI());

		// Create a response handler
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		
		long startTime = System.currentTimeMillis();
		
		String responseBody = httpclient.execute(post, responseHandler);
		
		long endTime = System.currentTimeMillis();
		System.out.println("Indexing time: " + (endTime - startTime) + " ms");
		totalTime = totalTime + (endTime - startTime);
		
		return responseBody;
		
	}
	
	private File[] getFiles(String directory)
	{
		File dir = new File(directory);
		FileFilter fileFilter = new WildcardFileFilter("*.xml");
		File[] files = dir.listFiles(fileFilter);
		return files;
	}

	// essentially, we're sending a bunch of documents into solr

	private String buildBatchRequest(String core, File[] files, int firstDoc, int lastDoc)
		throws Exception
	{
		/*
		<Docs>
			<Doc URI="/abs/dec/xyz.xml" />
			<Doc URI="/abs/dec/xyz.xml" />
		</Docs>
		 */
		StringWriter writer = new StringWriter();
		XMLOutputFactory2 factory = (XMLOutputFactory2)XMLOutputFactory.newInstance();
		XMLStreamWriter2 xmlWriter = (XMLStreamWriter2)factory.createXMLStreamWriter(writer);
	
		xmlWriter.writeStartDocument("UTF-8", "1.0");
		xmlWriter.writeCharacters("\n");
		xmlWriter.writeStartElement("Docs");
		xmlWriter.writeAttribute("core", core);
		xmlWriter.writeCharacters("\n");

		for (int i = firstDoc; i < lastDoc + 1; i++)
		{
			File file = files[i];
			xmlWriter.writeStartElement("Doc");
			xmlWriter.writeAttribute("uri", file.getAbsolutePath());
			xmlWriter.writeEndElement();		//</Doc>
			xmlWriter.writeCharacters("\n");
		}

		xmlWriter.writeEndElement();			// </Docs>
		xmlWriter.writeEndDocument();
	
		xmlWriter.flush();
	
		System.out.println("Num docs in batch = " + (lastDoc - firstDoc));
		return writer.toString();
	}

	public void importDA(String core, String dir)
		throws Exception
	{
		File[] documents = getFiles(dir);

		int i = 0;

		while (i < documents.length)
		{
			int firstDoc = i;
			int lastDoc = i + (BATCH_SIZE - 1) ;
			
			if (lastDoc > documents.length)
				lastDoc = documents.length - 1;

			String requestXML = buildBatchRequest(core, documents, firstDoc, lastDoc);

			try
			{
				String responseBody = indexDocuments(requestXML);
				
				System.out.println("----------------------------------------");
				System.out.println(responseBody);
				System.out.println("----------------------------------------");

				i += BATCH_SIZE;
			}
			catch (Exception e)
			{
				System.out.println(e.toString());
			}
		}

		System.out.println("Total Indexing time: " + totalTime + " ms");
		
	}
	
	public static void main(String[] args)
		throws Exception
	{
		//index all documents in a directory
		ImportSimulator sim = new ImportSimulator();

		// get the documents
		String docLocation =
			props.getProperty("document.location") + "/" +
			props.getProperty("segment.id") + "/" +
			props.getProperty("collection.id") + "/";
		
		if (args == null || args.length < 2)
		{
			System.err.println("Usage: ImportSimilator <<core>> <<list of DA IDs>>");
			System.exit(0);
		}
			
		String core = args[0];

		for (int i = 1; i < args.length; i++)
		{
			String dir = docLocation + args[i];
			sim.importDA(core, dir);
		}
		
		// When HttpClient instance is no longer needed,
		// shut down the connection manager to ensure
		// immediate deallocation of all system resources			
		sim.httpclient.getConnectionManager().shutdown();

	}		
}
