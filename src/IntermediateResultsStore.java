/*
 * A structure for storing intermediate results.  
 * It is backed by a file which lists VCF IDs and the refined sequence/position
 * The format of each line is (tab-separated) ID, Sequence, Position, CompletionCertificate
 * 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class IntermediateResultsStore {
	String fileName;
	PrintWriter out;
	Set<String> set;
	IntermediateResultsStore(String s, boolean resume) throws Exception
	{
		fileName = s;
		HashSet<String> ids = new HashSet<String>();
		set = Collections.synchronizedSet(ids);
		// If we are resuming, read in results so far and open appending output stream
		if(new File(s).exists() && resume)
		{
			Scanner input = new Scanner(new FileInputStream(new File(s)));
			while(input.hasNext())
			{
				String line = input.nextLine().trim();
				String[] tokens = line.split("\t");
				if(tokens.length != 5)
				{
					continue;
				}
				set.add(tokens[0]);
			}
			input.close();
			FileWriter fileWriter = new FileWriter(s, true);
		    out = new PrintWriter(fileWriter);

		}
		else
		{
			out = new PrintWriter(s);
		}
	}
	
	void addVariant(String key, String seq, long pos)
	{
		out.println(key+"\t"+seq+"\t"+pos+"\t"+System.currentTimeMillis()+"\t"+"$");
		out.flush();
		set.add(key);
	}
	
	void addNullVariant(String key)
	{
		addVariant(key, "X", -1);
	}
	
	void fillMapFromStore(NewSequenceMap nsm) throws Exception
	{
		out.close();
		Scanner input = new Scanner(new FileInputStream(new File(fileName)));
		while(input.hasNext())
		{
			String line = input.nextLine().trim();
			String[] tokens = line.split("\t");
			if(tokens.length != 5)
			{
				continue;
			}
			String key = tokens[0];
			if(!nsm.containsKey(key))
			{
				String seq = tokens[1];
				long pos = Long.parseLong(tokens[2]);
				if(pos != -1)
				{
					nsm.add(key, seq, pos);
				}
			}
		}
		input.close();
	}
}
