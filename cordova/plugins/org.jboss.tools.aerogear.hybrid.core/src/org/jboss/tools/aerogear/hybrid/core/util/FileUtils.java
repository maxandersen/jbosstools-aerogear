/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.aerogear.hybrid.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
/**
 * Utilities for working with files on the file system and inside bundles.
 * 
 * @author Gorkem Ercan
 *
 */
public final class FileUtils {
	private FileUtils(){
		//No instances
	}	
	
	/**
	 * Copies the contents for source directory to destination directory. 
	 * Source can be a directory on the file system or a jar file.
	 * Destination is a directory on the files system 
	 * 
	 * @param source - directory on the file system or jar file
	 * @param destination - a directory on the file system
	 */
	public static void directoryCopy (URL source, URL destination ) throws IOException{
		if( source == null || destination == null )
			return;
		source = getFileURL(source);
		destination = getFileURL(destination);
		if (!isFile(destination))
			return;
		

		File dstFile = new File(destination.getFile());
		if(!dstFile.exists() && !dstFile.mkdir() ){
				return;
		}
		
		if("file".equals(source.getProtocol())){
			File srcFile = new File(source.getFile());
			copyFile(srcFile, dstFile);
			
		}else if("jar".equals(source.getProtocol())){
			ZipFile zipFile = getZipFile(source);
			String file = source.getFile();
			int exclamation = file.indexOf('!');
			String jarLocation = file.substring(exclamation + 2); // "/some/path/"
			copyFromZip(zipFile, jarLocation, dstFile);
		}	
		
	}
	
	/**
	 * Copies the contents of source file to the destination file.
	 * Source can be a file on the file system or a jar file.
	 * Destination is a file on the file system.
	 * 
	 * @param source - file on the file system or jar file
	 * @param destination - a file on the file system
	 */
	public static void fileCopy(URL source, URL destination) throws IOException {
		if( source == null || destination == null )
			return;
		source = getFileURL(source);
		destination = getFileURL(destination);
		if(!isFile(destination))
			return;
		
		File dstFile = new File(destination.getFile());
		if("file".equals(source.getProtocol())){
			File srcFile = new File(source.getFile());
			if( !dstFile.exists() && !dstFile.createNewFile()){
				return;
			}
			copyFile(srcFile, dstFile);
			
		}else if("jar".equals(source.getProtocol())){
			ZipFile zipFile = getZipFile(source);
			String file = source.getFile();
			int exclamation = file.indexOf('!');
			String jarLocation = file.substring(exclamation + 2); // remove jar separator !/ 
			copyFromZip(zipFile, jarLocation, dstFile);
		}	

	}
	

	/**
	 * Convenience method to turn a file to a URL.
	 * May return null if it can not create a URL from the file passed or file is null.
	 * 
	 * @param file
	 * @return
	 */
	public static URL toURL(File file ){
		if (file == null )
			return null;
		try {
			return file.toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	
	private static boolean isFile(URL url){
		return "file".equals(url.getProtocol());
	}
	
	private static URL getFileURL( URL url ){
		try{
			url = FileLocator.resolve(url);
			return FileLocator.toFileURL(url);
		}
		catch(IOException e){
			return null;
		}
	}
	private static ZipFile getZipFile( URL url ){
		if(!"jar".equals(url.getProtocol()))
			return null;
		String file = url.getFile();
		int exclamation = file.indexOf('!');
		if (exclamation < 0)
			return null;
		URL fileUrl = null;
		try {
			fileUrl = new URL(file.substring(0, exclamation));
		} catch (MalformedURLException mue) {
			return null;
		}
		File pluginJar = new File(fileUrl.getFile());
		if (!pluginJar.exists())
			return null;
		
		try{
			ZipFile zipFile = new ZipFile(pluginJar);
			return zipFile;
		}
		catch(IOException e){
			return null;
		}
		
	}
	
	
	private static void copyFromZip(ZipFile zipFile, String locationInBundle,
			File destination) throws IOException{

	       Enumeration<? extends ZipEntry> entries = zipFile.entries();
	        while (entries.hasMoreElements()) {
	            ZipEntry zipEntry = entries.nextElement();
	            if (zipEntry.getName().startsWith(locationInBundle)) {
	            	
	            	File file = null;
	            	if(destination.isDirectory()){
	            		if(zipEntry.isDirectory()){
	            			file = destination;
	            		}else{
	            			IPath path = new Path(zipEntry.getName());
	            			file = new File(destination, path.lastSegment());
	            		}	            		
	            	}else {
	            		Assert.isTrue(!zipEntry.isDirectory(), "Can not copy a directory to a file");
	            		file = destination;
	            	}
	            	
	                if (!zipEntry.isDirectory()) {					
	                	createFileFromZipFile(file, zipFile, zipEntry);
					} else {
						if( !file.exists() ){
							file.mkdir();
						}
					}
	            }
	        }
	
		
	}

	private static void createFileFromZipFile(File file, ZipFile zipFile,
			ZipEntry zipEntry) throws IOException {
	
		file.createNewFile();
		FileOutputStream fout = null;
		FileChannel out = null;
		InputStream in = null;
		try {
			fout = new FileOutputStream(file);
			out = fout.getChannel();
			in = zipFile.getInputStream(zipEntry);
			out.transferFrom(Channels.newChannel(in), 0, Integer.MAX_VALUE);
		} finally {
			if (out != null)
				out.close();
			if (in != null)
				in.close();
			if (fout != null)
				fout.close();
		}
	}

	private static void copyFile(File source, File target) throws IOException {
		
	    File file = null;
	    if (source.isDirectory() && source.exists() && target.isDirectory() && target.exists()) {	      
	    	
	    	File[] children = source.listFiles();
	        for (File child : children) {
	        	file = target;
	        	if(child.isDirectory()){
	        		file = new File(target, child.getName());
	        		if(!file.exists())
	        			file.mkdir();
	        	}
	            copyFile(child, file);
	        }
	    } else {// source is a file
	    	if(target.isFile()){
	    		file = target;
	    	}else{
	    		file = new File(target, source.getName());
	    	}
	
	    	FileChannel out = null;
	        FileChannel in = null;
	        try {
	        	if(!file.exists()){
	        		file.createNewFile();
	        	}
	            
	        	 out = new FileOutputStream(file).getChannel();
	        	 in = new FileInputStream(source).getChannel();
	        	in.transferTo(0, in.size(), out);
	        }
	        catch ( IOException e){
	        	e.printStackTrace();
	        	throw e;
	        }
	        finally {
	        	if(out != null )
	        		out.close();
	        	if(in != null )
	        		in.close();
	        }
	    }
	}
}
