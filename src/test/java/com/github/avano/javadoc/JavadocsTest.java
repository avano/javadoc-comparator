package com.github.avano.javadoc;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.python.util.PythonInterpreter;

/**
 * Test class for comparing two javadoc .jar files
 * 
 * @author avano
 */
@RunWith(Parameterized.class)
public class JavadocsTest {
	private final static String JAVADOCS_PATH = System
			.getProperty("javadocDir");
	private final static String RESOURCES_PATH = "src/test/resources";
	private final static Logger LOGGER = Logger.getLogger(JavadocsTest.class
			.getName());

	/**
	 * Iterate through the folders in JAVADOCS_PATH and add those folders into
	 * the test files collection
	 * 
	 * @return collection of the folders to test with
	 */
	@Parameters(name = "{0}")
	public static Collection<Object[]> getFiles() {
		Collection<Object[]> params = new ArrayList<Object[]>();

		// If no folder is specified output a warning
		if (JAVADOCS_PATH == null) {
			LOGGER.warning("*** No javadocs folder specified!");
			return params;
		}
		for (File f : new File(JAVADOCS_PATH).listFiles()) {
			if (f.isDirectory()) {
				Object[] arr = new Object[] { f };
				params.add(arr);
			}
		}
		return params;
	}

	// Folder to be tested
	private File workDir;

	/**
	 * Constructor
	 * 
	 * @param file
	 *            Current working directory
	 */
	public JavadocsTest(File file) {
		this.workDir = file;

		// Remove directories (for example after previous test runs)
		removeUnnecessaryFiles();
	}

	/**
	 * Compares two javadoc files
	 */
	@Test
	public void compareJavadocs() {
		LOGGER.info("*** Starting test with folder '" + workDir.getName() + "'");

		// Get the all files in the folder - as we are dynamically generating
		// more files in this folder that we dont want to process with the
		// cycle
		File[] filesList = workDir.listFiles();

		// For every .jar file in the current workDir
		for (int i = 0; i < filesList.length; i++) {
			// Decide the destination folder name
			String destination = (i % 2 == 0) ? "first" : "second";
			// Get the .jar file
			File f = filesList[i];
			// Unzip the .jar file
			String tempPath = unzipFile(f, destination);
			// Remove dates in the new created folder
			removeDates(new File(tempPath));
		}

		// Create a raw diff
		doDiff();

		// Remove moved blocks from the raw diff and return the output file
		File finalDiff = executePythonScript("removeMovedWithoutHeaders");

		// Produce human-readable output to manually verify the diff
		executePythonScript("removeMovedWithHeaders");

		LOGGER.info("*******************************************************");

		// Assert that there are no differences in the final diff file
		assertEquals("Final diff file size", 0, finalDiff.length());
	}

	/**
	 * Unzips the jar file
	 * 
	 * @param f
	 *            file to be unzipped
	 * @param dirName
	 *            destination directory
	 * @return destination directory path
	 */
	public String unzipFile(File f, String dirName) {
		String path = workDir.getAbsolutePath() + File.separator + dirName;
		try {
			ZipFile zf = new ZipFile(f);
			zf.extractAll(path);
		} catch (ZipException e) {
			LOGGER.severe("Exception thrown during unzipping");
			e.printStackTrace();
			Assert.fail("Exception thrown during unzipping");
		}
		LOGGER.info("*** Jar file unzipped to '" + path + "'");
		return path;
	}

	/**
	 * Calls a simple bash script that is used to remove the dates from
	 * generated javadocs
	 * 
	 * @param folder
	 *            folder with extracted javadoc
	 */
	public void removeDates(File folder) {
		try {
			Runtime r = Runtime.getRuntime();
			Process p = r.exec(new File(RESOURCES_PATH).getAbsolutePath()
					+ File.separator + "remove_dates "
					+ folder.getAbsolutePath());

			// Log the script output to the FINEST logging
			BufferedReader in = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				LOGGER.finest(inputLine);
			}
			in.close();
		} catch (Exception e) {
			LOGGER.severe("Exception thrown during executing bash script");
			e.printStackTrace();
			Assert.fail("Exception thrown during executing bash script");
		}
		LOGGER.info("*** Removed dates from javadoc");
	}

	/**
	 * Calls the diff utility and exports the output to the file
	 */
	public void doDiff() {
		String diffCmd = "diff -ur -x *.MF -x *.LIST -x *.css -x *.gif "
				+ workDir + File.separator + "first " + workDir
				+ File.separator + "second";
		String s = "";
		Process p = null;
		try {
			p = Runtime.getRuntime().exec(diffCmd);
		} catch (IOException e) {
			LOGGER.severe("Exception thrown during executing diff utility");
			e.printStackTrace();
			Assert.fail("Exception thrown during executing diff utility");
		}

		// Print the diff output to file
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		try {
			PrintWriter writer = new PrintWriter(new File(
					workDir.getAbsolutePath() + File.separator + "raw.diff"));
			while ((s = stdInput.readLine()) != null) {
				writer.println(s);
			}
			writer.close();
		} catch (IOException e) {
			LOGGER.severe("Exception thrown during printing diff output to file");
			e.printStackTrace();
			Assert.fail("Exception thrown during executing diff output to file");
		}
		LOGGER.info("*** Raw diff file successfully created");
	}

	/**
	 * Removes moved blocks from a diff file
	 * 
	 * @param scriptName
	 *            name of the python script file
	 * @return file with the output of the python script
	 */
	public File executePythonScript(String scriptName) {
		PythonInterpreter interp = new org.python.util.PythonInterpreter();
		FileInputStream fis = null;
		File outputFile = new File(workDir + File.separator + scriptName
				+ ".diff");
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(new File(workDir + File.separator
					+ "raw.diff"));
			fos = new FileOutputStream(outputFile);
		} catch (Exception e) {
			LOGGER.severe("Exception thrown during opening streams");
			e.printStackTrace();
			Assert.fail("Exception thrown during opening streams");
		}
		interp.setIn(fis);
		interp.setOut(fos);
		interp.execfile(RESOURCES_PATH + File.separator + scriptName + ".py");
		try {
			fis.close();
			fos.close();
		} catch (IOException ex) {
			LOGGER.severe("Exception thrown during closing streams");
			ex.printStackTrace();
		}
		LOGGER.info("*** Final diff file successfully created");
		return outputFile;
	}

	/**
	 * Removes every file/directory in workDir except *.jar files
	 */
	public void removeUnnecessaryFiles() {
		for (File f : workDir.listFiles()) {
			if (f.isDirectory()) {
				recursiveDelete(f);
			}
			if (!f.getName().endsWith(".jar")) {
				f.delete();
			}
		}
		LOGGER.info("*** Removed unnecessary files from folder '"
				+ workDir.getName() + "'");
	}

	/**
	 * Deletes all files from directory recursively
	 * 
	 * @param file
	 *            file/folder to delete
	 */
	public void recursiveDelete(File file) {
		// to end the recursive loop
		if (!file.exists())
			return;

		// if directory, go inside and call recursively
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				// call recursively
				recursiveDelete(f);
			}
		}
		// call delete to delete files and empty directory
		file.delete();
	}
}
