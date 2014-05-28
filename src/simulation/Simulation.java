package simulation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import lob.Order;
import lob.OrderBook;
// TODO BUG null pointer in bestprice when very few NTs
// TODO BUG upward price movement
// Findings:
// 		- NTs and fundamentals are fine
//		- I think MMs are upward biased

public class Simulation {
	
	static DataCollector data;
	
	private static void marketTrial(boolean verbose, String dataDir, int nRuns) {
		System.out.println("Beginning simulation...\n");
	
		data = new DataCollector(dataDir, nRuns);
		
		Properties prop = getProperties("config.properties");
		int timesteps = Integer.parseInt(prop.getProperty("timesteps"));
		
		Market mkt = new Market(prop, data);
		
		for (int i=0; i<nRuns; i++) {
			mkt.run(timesteps, i, verbose);
			//data.writeDaysData("trades", "quotes", "mids", i);
			mkt.reset();
			System.out.println("Run " + i + " DONE");
		}
		data.writeSimData(prop.getProperty("tradesFName"), 
						  prop.getProperty("quotesFName"),
						  prop.getProperty("midsFName"),
						  prop.getProperty("simFName"));;
		
//		try {
//			Runtime.getRuntime().exec("Rscript " + dataDir + "day_in_pictures.r");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		System.out.println("\nFinished simulation...");
//		runStats(nRuns, prop);
	}
	
	
	private static Properties getProperties(String filename) {
		FileInputStream in = null;	
		Properties prop = null;
		try {
			in = new FileInputStream(filename);
			prop = new Properties();
			prop.load(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return prop;
		
	}
	
	public static void runStats(int nRuns, Properties prop) {
		try {
			String cmd = "rscript simSummary.r --dir=" + data.dataDir + 
					" --nRuns=" + nRuns + 
					" --quotesF=" + prop.getProperty("quotesFName") +
					" --midsF=" + prop.getProperty("midsFName");
			System.out.println("r Script is running in the background, be patient!");
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
	        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));  
	        String line = null;  
	        while ((line = in.readLine()) != null) {  
	            System.out.println(line);  
	        } 
		} catch(IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		marketTrial(false, "/Users/user/Dropbox/PhD_ICSS/Research/ABM/output/", 1);
	}
}
