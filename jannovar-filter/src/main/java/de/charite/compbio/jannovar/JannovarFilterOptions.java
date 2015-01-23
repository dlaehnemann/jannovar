package de.charite.compbio.jannovar;

import java.io.PrintStream;

import de.charite.compbio.jannovar.pedigree.ModeOfInheritance;

/**
 * Configuration for the jannovar-filter app.
 *
 * @author Manuel Holtgrewe <manuel.holtgrewe@charite.de>
 */
public class JannovarFilterOptions {

	/** verbosity level */
	int verbosity = 1;

	/** path to input file */
	String inputPath = null;

	/** path to output file */
	String outputPath = null;

	/** selected mode of inheritance */
	ModeOfInheritance inheritanceMode = ModeOfInheritance.UNINITIALIZED;

	/**
	 * Print option values to stderr.
	 */
	public void print(PrintStream out) {
		out.println("verbosity: " + verbosity);
		out.println("input path: " + inputPath);
		out.println("output path: " + outputPath);
		out.println("inheritanceMode: " + inheritanceMode);
	}

}
