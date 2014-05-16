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
	
	public ArrayList<ArrayList<Order>> quoteCollector = new ArrayList<ArrayList<Order>>();
	public ArrayList<ArrayList<MidPrice>> midPrices = new ArrayList<ArrayList<MidPrice>>();
	public ArrayList<List<Trade>> trades = new ArrayList<List<Trade>>();
	
	public List<DaySummary> daySummaries = new ArrayList<DaySummary>();
	
	public DataCollector(String dataDir, int nRuns) {
		this.dataDir = dataDir;
		
		for (int i=0;i<nRuns;i++) {
			quoteCollector.add(new ArrayList<Order>());
			midPrices.add(new ArrayList<MidPrice>());
		}
	}
	
	public void addMidPrice(int time, double price, int idx) {
		midPrices.get(idx).add(new MidPrice(time, price));
	}
	
	public void endOfDay(int idx, OrderBook lob) {
		int buyVol = 0;
		int nBuys = 0;
		int sellVol = 0;
		int nSells = 0;
		ArrayList<Order> dayInQuestion = quoteCollector.get(idx);
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
		trades.add(lob.getTape());
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
	
	public void writeDaysData(String tradeDataName, String quoteDataName,
							  String midDataName, int idx) {
		dayTradesToCSV(tradeDataName, idx);
		dayQuotesToCSV(quoteDataName, idx);
		dayMidsToCSV(midDataName, idx);
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
	
	public void writeSimData(String tradeDataName, String quoteDataName,
			    			 String midDataName, String simFile) {
		
		writeSimQuotes(quoteDataName);
		writeSimMids(midDataName);
		writeSimTrades(tradeDataName);
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
