package simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lob.*;

public class DataCollector {
	
	class MidPrice {
		public int time;
		public double price;
		public MidPrice(int time, double price) {
			this.time = time;
			this.price = price;
		}
	}
	
	class Impact {
		public int time;
		public double impact;
		public int volume;
		public Impact(int time, double impact, int volume) {
			this.time = time;
			this.impact = impact;
			this.volume = volume;
		}
	}
	
	class DaySummary {
		public int nBuyOrders;
		public int nSellOrders;
		public int buyVolume;
		public int sellVolume;
		
		public DaySummary(int nBuyOrders, int nSellOrders, 
						  int buyVolume, int sellVolume) {
			this.nBuyOrders = nBuyOrders;
			this.nSellOrders = nSellOrders;
			this.buyVolume = buyVolume;
			this.sellVolume = sellVolume;
		}
	}
	
	public String dataDir;
	public String dataExt = ".csv";
	
	public List<DaySummary> daySummaries = new ArrayList<DaySummary>();
	
	public ArrayList<ArrayList<Order>> quoteCollector = new ArrayList<ArrayList<Order>>();
	public ArrayList<ArrayList<MidPrice>> midPrices = new ArrayList<ArrayList<MidPrice>>();
	public ArrayList<ArrayList<Impact>> impacts = new ArrayList<ArrayList<Impact>>();
	public ArrayList<List<Trade>> trades = new ArrayList<List<Trade>>();
	
	public ArrayList<Integer> dailyVolumes = new ArrayList<Integer>();
	
	public DataCollector(String dataDir, int nRuns) {
		this.dataDir = dataDir;
		
		for (int i=0;i<nRuns;i++) {
			quoteCollector.add(new ArrayList<Order>());
			midPrices.add(new ArrayList<MidPrice>());
			impacts.add(new ArrayList<Impact>());
		}
	}
	
	public void addMidPrice(int time, double price, int runNumber) {
		midPrices.get(runNumber).add(new MidPrice(time, price));
	}
	
	
	/*
	 * Impact is measured as the log difference before and after the market
	 * order arrives.
	 * 
	 * At the end of the day, the volumes must be normalised by the total day's 
	 * volume.
	 */
	public void addImpact(int time, 
						  double before, double after, 
						  int volume, int runNumber) {
		double impact = Math.log(after) - Math.log(before);
		impacts.get(runNumber).add(new Impact(time, impact, volume));
	}
	
	public void addDaysVolume (int vol) {
		dailyVolumes.add(vol);
	}
	
	public void endOfDay(int runNumber, OrderBook lob, int todaysVol) {
		int buyVol = 0;
		int nBuys = 0;
		int sellVol = 0;
		int nSells = 0;
		ArrayList<Order> dayInQuestion = quoteCollector.get(runNumber);
		for (Order q : dayInQuestion) {
			if (q.getSide()=="bid") {
				buyVol+=q.getQuantity();
				nBuys+=1;
			} else {
				sellVol+=q.getQuantity();
				nSells+=1;
			}
		}
		//add tape to trades DB
		daySummaries.add(new DaySummary(nBuys, nSells, buyVol, sellVol));
		trades.add(new ArrayList<Trade>(lob.getTape()));
		addDaysVolume(todaysVol);
		// TODO normalise all impact quantities by daily vol
		ArrayList<Impact> todaysImpacts = impacts.get(runNumber);
		for(Impact i : todaysImpacts) {
			i.volume /= todaysVol;
		}
	}
	
	/**************************************************************************
	 *************************** Writing and Printing *************************
	 **************************************************************************/
	
	public void dayQuotesToCSV(String fName, int idx) {
		try {
			File dumpFile = new File(dataDir+fName+dataExt);
			BufferedWriter output = new BufferedWriter(new FileWriter(dumpFile));
			output.write("time, type, side, quantity, price, tId\n");
			ArrayList<Order> dayInQuestion = quoteCollector.get(idx);
			for (Order q : dayInQuestion) {
				String quoteString = (q.getTimestamp() + ", " +
										(q.isLimit() ? "limit" : "market") + ", " + 
										((q.getSide()=="bid") ? "1" : "-1") + ", " +
										q.getQuantity() + ", ");
				if (q.isLimit()) {
					quoteString += q.getPrice();
				} else {
					quoteString += "\t";
				}
				quoteString += (", " + q.gettId() + "\n");
				output.write(quoteString);
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void dayMidsToCSV(String fName, int idx) {
		try {
			File dumpFile = new File(dataDir+fName+dataExt);
			BufferedWriter output = new BufferedWriter(new FileWriter(dumpFile));
			output.write("time, price\n");
			ArrayList<MidPrice> dayInQuestion = midPrices.get(idx);
			for (MidPrice m : dayInQuestion) {
				String l = (m.time + ", " + m.price +"\n");
				output.write(l);
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void dayTradesToCSV(String fName, int idx) {
		try {
			File dumpFile = new File(dataDir+fName+dataExt);
			BufferedWriter output = new BufferedWriter(new FileWriter(dumpFile));
			output.write("time, price, quantity, provider, taker, buyer, seller\n");
			List<Trade> dayInQuestion = trades.get(idx);
			for (Trade t : dayInQuestion) {
				output.write(t.toCSV());
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void dayImpactsToCSV(String fName, int idx) {
		try {
			File dumpFile = new File(dataDir+fName+dataExt);
			BufferedWriter output = new BufferedWriter(new FileWriter(dumpFile));
			output.write("volume, impact\n");
			ArrayList<Impact> dayInQuestion = impacts.get(idx);
			for (Impact i : dayInQuestion) {
				String l = (i.volume + ", " + i.impact+"\n");
				output.write(l);
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeDaysData(String tradeDataName, 
							  String quoteDataName,
							  String midDataName, 
							  String impactDataName,
							  int idx) {
		dayTradesToCSV(tradeDataName, idx);
		dayQuotesToCSV(quoteDataName, idx);
		dayMidsToCSV(midDataName, idx);
		dayImpactsToCSV(impactDataName, idx);
	}
	
	public void writeSimQuotes(String fName) {
		for (int i=0; i<quoteCollector.size(); i++) {
			dayQuotesToCSV(fName+i, i);
		}
	}
	
	public void writeSimMids(String fName) {
		for (int i=0; i<midPrices.size(); i++) {
			dayMidsToCSV(fName+i, i);
		}
	}
	
	public void writeSimTrades(String fName) {
		for (int i=0; i<trades.size(); i++) {
			dayTradesToCSV(fName+i, i);
		}
	}
	
	public void writeSimImpacts(String fName) {
		for (int i=0; i<trades.size(); i++) {
			dayImpactsToCSV(fName+i, i);
		}
	}
	
	public void writeSimData(String tradeDataName, 
							 String quoteDataName,
			    			 String midDataName, 
			    			 String impactDataName,
			    			 String simFile) {
		
		writeSimQuotes(quoteDataName);
		writeSimMids(midDataName);
		writeSimTrades(tradeDataName);
		writeSimImpacts(impactDataName);
		try {
			File dumpFile = new File(dataDir+simFile+dataExt);
			BufferedWriter output = new BufferedWriter(new FileWriter(dumpFile));
			output.write("day, nBids, nOffers, bidVol, offerVol\n");
			int i =1;
			for(DaySummary d : daySummaries) {
				output.write(i + ", " + 
							 d.nBuyOrders + ", " + 
							 d.nSellOrders + ", " + 
							 d.buyVolume + ", " + 
							 d.sellVolume + "\n");
				i+=1;
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
