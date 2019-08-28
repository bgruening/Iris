import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/*
 * A wrapper around Ngmlr for aligning assembled sequences back to the reference
 */
public class AlignConsensus {
	
	/*
	 * Given the assembled sequences, align them to a region of the reference with ngmlr
	 */
	static ArrayList<String> getConsensusAlignmentRecords(String id, ArrayList<String> consensusSequences, GenomeQuery gq) throws Exception
	{
		String ngmlrInFn = id + ".ngmlr.in";
		String ngmlrOutFn = id + ".ngmlr.out";
		String genomeSampleFn = id + ".region.fa";
		writeNgmlrInput(consensusSequences, ngmlrInFn);
		writeGenomeSample(id, genomeSampleFn, gq);
		executeNgmlr(ngmlrInFn, genomeSampleFn, ngmlrOutFn);
		ArrayList<String> res = getNgmlrAlignmentStrings(ngmlrOutFn);
		if(Settings.CLEAN_INTERMEDIATE_FILES)
		{
			new File(ngmlrInFn).delete();
			new File(ngmlrOutFn).delete();
			new File(genomeSampleFn).delete();
			new File(genomeSampleFn + "-enc.2.ngm").delete();
			
			// Note: 13 here is the default kmer length used in ngmlr's indexing - change if default is not used
			new File(genomeSampleFn + "-ht-13-2.2.ngm").delete();
			
		}
		return res;
	}
	
	static void writeGenomeSample(String id, String gsFn, GenomeQuery gq) throws Exception
	{
		String chr = VcfEntry.getChrFromKey(id);
		long pos = VcfEntry.getPosFromKey(id);
		long start = Math.max(1, pos - Settings.GENOME_REGION_BUFFER);
		long end = pos + Settings.GENOME_REGION_BUFFER;
		String sample = gq.genomeSubstring(chr, start, end);
		
		PrintWriter out = new PrintWriter(new File(gsFn));
		out.println(String.format(">%s\n%s", chr, sample));
		out.close();
	}
	
	/*
	 * Given the assembled sequences, write them in FASTA format so they can be processed by ngmlr
	 */
	static void writeNgmlrInput(ArrayList<String> seqs, String ngmlrInputFileName) throws Exception
	{
		PrintWriter out = new PrintWriter(new File(ngmlrInputFileName));
		int n = seqs.size();
		for(int i = 0; i<n; i++)
		{
			out.println(String.format(">consensus%d\n%s", i, seqs.get(i)));
		}
		out.close();
	}
	
	/*
	 * Run ngmlr through the command line using parameters from Settings
	 */
	static void executeNgmlr(String ngmlrIn, String genomeSample, String ngmlrOut) throws Exception
	{
		String ngmlrCommand = String.format(
				 "%s -t %d -r %s -q %s -o %s", 
				 Settings.NGMLR_PATH, Settings.NGMLR_THREADS,
				 genomeSample, ngmlrIn, ngmlrOut);
		ArrayList<String> fullNgmlrCommand = new ArrayList<>();
		for(String s : ngmlrCommand.split(" ")) fullNgmlrCommand.add(s);
		Process child = new ProcessBuilder()
				.command(fullNgmlrCommand)
				.start();
		int p = child.waitFor();
		if(p != 0)
		{
			throw new Exception("error running ngmlr on " + ngmlrIn);
		}
	}
	
	/*
	 * Given the output produced from ngmlr, get all of the alignment records
	 */
	static ArrayList<String> getNgmlrAlignmentStrings(String ngmlrOutputFileName) throws Exception
	{
		File toRead = new File(ngmlrOutputFileName);
		if(!toRead.exists())
		{
			throw new Exception("could not find ngmlr output file: " + ngmlrOutputFileName);
		}
		
		ArrayList<String> alignmentRecords = new ArrayList<>();
				
		Scanner input = new Scanner(new FileInputStream(toRead));
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(!line.startsWith("@"))
			{
				//System.out.println(line);
				alignmentRecords.add(line);
			}
		}
		
		input.close();
		
		return alignmentRecords;
	}

}