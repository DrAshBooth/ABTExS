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
	
	public String dataDir;
	private OrderBook lob;
	public List<Order> quoteCollector = new ArrayList<Order>();
	public List<MidPrice> midPrices = new ArrayList<MidPrice>();
	
	public DataCollector(String dataDir, OrderBook lob) {
		this.dataDir = dataDir;
		this.lob = lob;
	}
	
	public void addMidPrice(int time, double price) {
		midPrices.add(new MidPrice(time, price));
	}
	
	/**************************************************************************
	 *************************** Writing and Printing *************************
	 **************************************************************************/
	
	public void quotesToCSV(String fName) {
		try {
			File dumpFile = new File(dataDir+fName);
			BufferedWriter output = new BufferedWriter(new FileWriter(dumpFile));
			output.write("time, type, side, quantity, price, tId\n");
			for (Order q : quoteCollector) {
				String quoteString = (q.getTimestamp() + ", " +
										(q.isLimit() ? "limit" : "market") + ", " + 
										q.getSide() + ", " +
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
	
	public void midsToCSV(String fName) {
		try {
			File dumpFile = new File(dataDir+fName);
			BufferedWriter output = new BufferedWriter(new FileWriter(dumpFile));
			output.write("time, price");
			for (MidPrice m : midPrices) {
				String l = (m.time + ", " + m.price +"\n");
				output.write(l);
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void tapeToCSV(String fName) {
		try {
			File dumpFile = new File(dataDir+fName);
			BufferedWriter output = new BufferedWriter(new FileWriter(dumpFile));
			output.write("time, price, quantity, provider, taker, buyer, seller\n");
			for (Trade t : lob.getTape()) {
				output.write(t.toCSV());
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeDaysData(String tradeDataName, String quoteDataName,
							  String midDataName) {
		tapeToCSV(tradeDataName);
		quotesToCSV(quoteDataName);
		midsToCSV(midDataName);
	}

}
