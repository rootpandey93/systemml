/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2013
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 */


package com.ibm.bi.dml.runtime.matrix.mapred;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TextInputFormat;

/**
 * <p>Genereates a single mapper for each input line.</p>
 * <p>Should not be used for large files, as everything is read to create the splits.</p>
 */
public class MapPerLineTextInputFormat extends TextInputFormat
{
	@SuppressWarnings("unused")
	private static final String _COPYRIGHT = "Licensed Materials - Property of IBM\n(C) Copyright IBM Corp. 2010, 2013\n" +
                                             "US Government Users Restricted Rights - Use, duplication  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.";
	
	public InputSplit[] getSplits(JobConf job, int numSplits) throws IOException
	{
		FileStatus[] files = listStatus(job);
		ArrayList<FileSplit> splits = new ArrayList<FileSplit>(numSplits);
		
		for(FileStatus file : files)
		{
			Path path = file.getPath();
			FileSystem fs = path.getFileSystem(job);
			FSDataInputStream fsIn = fs.open(path);
			BufferedInputStream in = new BufferedInputStream(fsIn);
			long pos = 0;
			long startPos = 0;
			int b = in.read();
			int lastChar;
			
			// A line is considered to be terminated by any one of a line feed ('\n'), a carriage return ('\r'), or a
			// carriage return followed immediately by a linefeed.
			while(b != -1)
			{
				lastChar = b;
				b = in.read();
				pos++;
				
				if(lastChar != '\n' && lastChar != '\r')
					continue;
				
				if(b == '\n')
				{
					b = in.read();
					pos++;
				}
				
				splits.add(new FileSplit(path, startPos, (pos - startPos), job));
				startPos = pos;
			}
			
			in.close();
		}
		
		//TODO: Leo need to figure out where to log
		LOG.debug("Total # of splits: " + splits.size());
		return splits.toArray(new FileSplit[splits.size()]);
	}
}