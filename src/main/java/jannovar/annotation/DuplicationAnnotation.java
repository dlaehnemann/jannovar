/**
 * 
 */
package jannovar.annotation;


import jannovar.common.VariantType;
import jannovar.exception.AnnotationException;
import jannovar.reference.TranscriptModel;
import jannovar.reference.Translator;

/**
 * This class is intended to provide a static method to generate annotations for
 * duplication mutations.
 * TODO currently the handling of tandem-repeats is not as recommended <br>
 * e.g. these cases:<br>
 * g.7_8dup (or g.7_8dupTG, not g.5_6dup, not g.8_9insTG) denotes a TG duplication in 
 * the TG-tandem repeat sequence changing ACTTTGTGCC to ACTTTGTGTGCC <br>
 * g.7_8[4] (or g.5_6[4], or g.5TG[4], not g.7_10dup) is the preferred description of  
 * the addition of two extra TG's to the variable TG repeated sequence changing ACTTTGTGCC to ACTTTGTGTGTGCC
 * 
 * @author mjaeger
 * @version 0.04 (26-12-2013)
 */
public class DuplicationAnnotation {
    /**
     * Annotates an insertion variant that is an duplication. 
     * The methods of this class are called from 
     * {@link jannovar.annotation.InsertionAnnotation InsertionAnnotation} if that
     * class determines that the insertion is equal to the preceding nucleotides
     * in the reference sequence.
     * That is, in addition to the conditions for a insertion variant, the
     * duplication variant requires a similar sequence to the insertion (before
     * or) after the insertion.
     * <P>
     * There are two possible duplication insertions with or without frameshift
     * causation. e.g. inserting an additional 'C' in the sequence 'ACC,GAG' at
     * position 2 would cause a frameshift, whereas insertion of 'CCG' at
     * position 2 just inserts an additional triple 'ACC GCC GAG'.
     * <P>
     * 
     * if (var.length() % 3 == 0) { /* ORF CONSERVING 
     * 	if(startPosMutationInCDS.length() % 3 == 0){ /* SIMPLE DUPLICATION OF CODONS
     * 	} else { /* substitution from original AA to AAs
     * 	if(wtaa.equals("*")) { /* Mutation affects the wildtype stop codon
     * 	int idx = varaa.indexOf("*");
     * 	if (idx < 0) {
     * 	}
     * 	/* Substitution
     * 	}
     * }else { /* FRAMESHIFT
     *   * short p.(Arg97fs)) denotes a frame shifting change with Arginine-97 as the first affected amino acid
     * }
     * 
     * 
     * @param trmdl  The transcriptmodel / gene in which the current mutation is contained
     * @param frame_s the location within the frame (0,1,2) in which mutation occurs
     * @param wtnt3  The three nucleotides of codon affected by start of mutation
     * @param wtnt3_after  the three nucleotides of the codon following codon affected by  mutation
     * @param ref reference nucleotide sequence ("-")
     * @param var alternate nucleotide sequence (the duplication)
     * @param refvarstart  The nucleotide position just upstream of the variant with respect to the cDNA (one-based)
     * @param exonNumber    Number (one-based) of affected exon.
     * @return an {@link jannovar.annotation.Annotation Annotation} object representing the current variant
     * @throws AnnotationException
     */
    public static Annotation getAnnotation(TranscriptModel trmdl, int frame_s, String wtnt3,
					   String wtnt3_after, String var, 
					   int startpos, int endpos, int exonNumber) throws AnnotationException 
    {
	String annot;
	Annotation ann = null;
	Translator translator = Translator.getTranslator(); /* Singleton */
	/* For the '-'-strand fix the position in the reference */
	//if(trmdl.isMinusStrand()){
	//  refvarstart = refvarstart + var.length()+1;
	//}

	int refcdsstart = trmdl.getRefCDSStart();

	/* Since refvarstart is the position just upstream of the duplication, if we substract the 
	 * length of the variant we need to add back one.
	 * -----ACAC-- (the duplication)
	 * -----AC---  (the wildtype seq)
	 * ----- *---  (* shows refvarstart for "+" strand variant)
	 * Assume that the start codon begins in position 3 (§)
	 * --§-- *---
	 * Then, refcdsstart=3, and refvarstart=5 (i.e., last position of wildtype seq that is duplicated is 5 in the CDS)
	 * Mutation is thus c.4_5dup or c.4_5dupAC
	 */

	int cdsEndPos = endpos - trmdl.getRefCDSStart() + 1;
	int cdsStartPos = cdsEndPos - var.length() + 1;

	/** aavarpos is now the FIRST position (one-based) of the amino-acid sequence
	    that was duplicated.*/
	int aaVarStartPos =  cdsStartPos % 3 == 0 ? 
	    (int) Math.floor(cdsStartPos / 3) : 
	    (int) Math.floor(cdsStartPos / 3) +1;

	//		if(trmdl.isMinusStrand())
	//			aavarpos += startPosMutationInCDS % 3;
	//debugDuplication(trmdl,frame_s, wtnt3, wtnt3_after,var, startpos, exonNumber,aaVarStartPos);
	/* get coding DNA HGVS string */
	String canno;
	if(var.length() == 1)
	    canno = String.format("c.%ddup%s", cdsStartPos, var); 
	else
	    canno = String.format("c.%d_%ddup%s", cdsStartPos,cdsEndPos, var);
	
	/* now the protein HGVS string */
	
	/* generate in-frame snippet for translation and correct for '-'-strand */
	if (trmdl.isMinusStrand()) {
	    /* Re-adjust the wildtype nucleotides for minus strand */
	    wtnt3 = trmdl.getWTCodonNucleotides(startpos-1+((3 - (var.length() % 3)) % 3), frame_s);
	}
	String varnt3 =  getVarNt3(trmdl,wtnt3,var,frame_s);
	
	String wtaa = translator.translateDNA(wtnt3);		
	String varaa = translator.translateDNA(varnt3);
	
	if (var.length() % 3 == 0) { /* ORF CONSERVING */
	    if((cdsStartPos-1) % 3 == 0){ /* SIMPLE DUPLICATION OF CODONS, e.g., nucleotide position 4 starts a
					   codon, and (4-1)%3==0.*/ 
		String wtaaDupStart = translator.translateDNA(var.substring(0,3));
		String wtaaDupEnd   = translator.translateDNA(var.substring(var.length()-3));
		if (var.length() == 3) {
		    // Three nucleotides affected, inframe, single aminoacid duplication.
		    annot=singleAminoAcidInframeDuplication(trmdl.getName(),exonNumber,canno,wtaaDupStart,
							    aaVarStartPos);
		} else {
		    //System.out.println("B aarvarpos=" + aaVarStartPos + " varaa.length()="+varaa.length() );
		    int aaEndPos=aaVarStartPos + (var.length()/3) - 1; /* last amino acid of duplicated WT seq. */
		    annot =  multipleAminoAcidInframeDuplication(trmdl.getName(), exonNumber,
								 canno, wtaaDupStart,aaVarStartPos,
								 wtaaDupEnd,aaEndPos);
		    
		    //System.out.println("annot="+annot);
		}
		ann = new Annotation(trmdl, annot, VariantType.NON_FS_DUPLICATION, cdsStartPos);
	    } else { /* substitution from original AA to AAs */
		if(wtaa.equals("*")) { /* Mutation affects the wildtype stop codon */
		    int idx = varaa.indexOf("*");
		    if (idx < 0) {
			annot = String.format("%s:exon%d:%s:p.*%d%sext*?", trmdl.getName(), exonNumber,
					      canno, aaVarStartPos, varaa);
		    }else{
			
			annot = String.format("%s:exon%d:%s:p.*%ddelins%s", trmdl.getName(), exonNumber,
					      canno, aaVarStartPos, varaa.substring(0, idx+1));
		    }
		}else{
		    annot = String.format("%s:exon%d:%s:p.%s%ddelins%s", trmdl.getName(), exonNumber,
					  canno, wtaa, aaVarStartPos, varaa);
		}
		/* Substitution */
		ann = new Annotation(trmdl, annot, VariantType.NON_FS_DUPLICATION, cdsStartPos);
	    }
	}else { /* FRAMESHIFT 
		 * short p.(Arg97fs)) denotes a frame shifting change with Arginine-97 as the first affected amino acid */ 
	    
	    annot = String.format("%s:exon%d:%s:p.%s%dfs", trmdl.getName(), exonNumber,
				  canno, wtaa, aaVarStartPos);
	    ann = new Annotation(trmdl, annot, VariantType.FS_DUPLICATION, cdsStartPos);
	}
	return ann;
    }


  
    /**
     * This function retrieves the codon that contains the start of the duplication
     * in the variant sequence. If L is the length of the duplication, this
     * method will return a string of length L+3 with the three nucleotides surrounding
     * the duplication.
     */
    private static String getVarNt3(TranscriptModel trmdl,String wtnt3, String var, int frame_s)
    {
	String varnt3 = null;
	if (trmdl.isPlusStrand()) {
	    frame_s = 2 - frame_s;
	    if (frame_s == 0) { /* duplication located at 0-1-INS-2 part of codon */
		varnt3 = String.format("%c%c%s%c", wtnt3.charAt(0), wtnt3.charAt(1), var, wtnt3.charAt(2));
	    } else if (frame_s == 2) {
		varnt3 = String.format("%s%s", var, wtnt3);
	    } else { /* i.e., frame_s == 0, duplication located at 0-INS-1-2 part of codon */
		varnt3 = String.format("%c%s%c%c", wtnt3.charAt(0), var, wtnt3.charAt(1), wtnt3.charAt(2));
	    }
	} else {
	    frame_s = 2 - frame_s;
	    if (frame_s == 2) {
		varnt3 = String.format("%c%s%c%c", wtnt3.charAt(0), var, wtnt3.charAt(1), wtnt3.charAt(2));
	    } else if (frame_s == 1) {
		varnt3 = String.format("%c%c%s%c", wtnt3.charAt(0), wtnt3.charAt(1), var, wtnt3.charAt(2));
	    } else { /* i.e., frame_s == 0 */
		varnt3 = String.format("%s%s", wtnt3, var);
	    }
	    //			frame_s = 2 - frame_s;
	}
	return varnt3;
    }


    /**
     * This function is for a duplication that affects one triplet of nucleotides and thus changes one aminoacid.
     * for example
     * OR2T3(uc001iel.1:exon1:c.769_771dupTTC:p.F257dup)
     * @param transcriptID The accession number of the affected transcript, e.g., uc001iel.1
     * @param exonNumber One-based number of the exon
     * @param cDNAannot Annotation of the cDNA, e.g., c.769_771dupTTC
     * @param wtAA Wildtype amino acid that has been duplicated, e.g., F
     * @param aavarpos The position of the wildtype amino acid that has been duplicated.
     */
    private static String singleAminoAcidInframeDuplication(String transcriptID, 
						     int exonNumber, 
						     String cDNAannot,
						     String wtAA,
						     int aavarpos)
    {
	String a = String.format("%s:exon%d:%s:p.%s%ddup", transcriptID,exonNumber,cDNAannot,wtAA,aavarpos);
	return a;
    }


    /**
     * HGVS: p.Gly4_Gln6dup in the sequence MKMGHQQQCC denotes a duplication 
     * of amino acids Glycine-4 (Gly, G) to Glutamine-6 (Gln, Q) (i.e. MKMGHQGHQQQCC)
     * @param transcriptID The accession number of the affected transcript, e.g., uc001iel.1
     * @param exonNumber One-based number of the exon
     * @param cDNAannot Annotation of the cDNA, e.g., c.769_771dupTTC
     * @param firstWtAA First Wildtype amino acid that has been duplicated, e.g., F
     * @param firstaavarpos The position of the first wildtype amino acid that has been duplicated.
     * @param lastWtAA Last Wildtype amino acid that has been duplicated, e.g., F
     * @param lastaavarpos The position of the last wildtype amino acid that has been duplicated.
     */
    private static String multipleAminoAcidInframeDuplication(String transcriptID, 
						       int exonNumber, 
						       String cDNAannot,
						       String firstWtAA,
						       int firstaavarpos,
						       String lastWtAA,
						       int lastaavarpos)
    {
	String a = String.format("%s:exon%d:%s:p.%s%d_%s%ddup", transcriptID, exonNumber,
				 cDNAannot,firstWtAA ,firstaavarpos,lastWtAA, lastaavarpos);
	return a;
    }



   
    

    /**
     * A convenience method for printing out information about duplication annotations.
     * Hopefully useful for checking/debugging.
     */
    private static void debugDuplication(TranscriptModel trmdl, int frame_s, String wtnt3,
					   String wtnt3_after, String var, 
					 int refvarstart, int exonNumber,int aavarpos)
    {
	System.err.println("#--------------- DuplicationAnnotation.java: DEBUG --------------------#");
	int cdsEndPos = refvarstart - trmdl.getRefCDSStart() + 1;
	int cdsStartPos = cdsEndPos - var.length() + 1;
	System.out.println("cdsStartPos="+cdsStartPos);
	System.out.println("cdsEndPos="+cdsEndPos);
	System.out.println("refvarstart="+refvarstart);
	System.out.println("var.length()="+var.length());
	System.out.println("wtnt3="+wtnt3);
	System.out.println("wtnt3_after="+wtnt3_after);
	System.out.println("frame_s="+frame_s);
	System.out.println("exonNumber="+exonNumber);
	System.out.println("aavarpos="+aavarpos);
	trmdl.debugPrint();
	trmdl.debugPrintCDS();
    }

}
/* eof */