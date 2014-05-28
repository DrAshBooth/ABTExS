package simulation;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import lob.*;
import traders.*;

public class Market {

	private HashMap<Integer, Trader> tradersById = new HashMap<Integer, Trader>();
	private HashMap<String, ArrayList<Trader>> tradersByType = 
										new HashMap<String, ArrayList<Trader>>();
	private List<Integer> tIds;
	
	private final int n_NTs;
	private final int n_MMs;
	private final int n_FTs;
	
	private final double p_NT;
	private final double p_MM;
	private final double p_FT;
	
	private Properties properties;
	private OrderBook lob;
	private DataCollector data;
	private Random generator = new Random();
	
	int runNumber;
	
	public Market(Properties prop, DataCollector data) {
		super();
		this.n_NTs = Integer.valueOf(prop.getProperty("n_NTs"));
		this.n_MMs = Integer.valueOf(prop.getProperty("n_MMs"));
		this.n_FTs = Integer.valueOf(prop.getProperty("n_FTs"));
		this.p_NT = Double.valueOf(prop.getProperty("p_NT"));
		this.p_MM = Double.valueOf(prop.getProperty("p_MM"));
		this.p_FT = Double.valueOf(prop.getProperty("p_FT"));
		
		this.properties = prop;
		
		this.lob = new OrderBook(Double.valueOf(prop.getProperty("tick_size")));
		this.data = data;
		
		populateMarket();
	}
	
	public void run(int timesteps, int runNumber, boolean verbose) {
		
		this.runNumber = runNumber;
		
		for (int time = 1; time <= timesteps; time++) {
			if (verbose) {
				System.out.println("----- time = " + time + 
									"-------------------------------------------------------------------");
			}
			Trader tdr;
			// if either book empty, pick NT to trade
			if ((lob.volumeOnSide("bid")==0) || (lob.volumeOnSide("offer")==0)) {
				int ntIdx = generator.nextInt(tradersByType.get("NT").size());
				tdr = tradersByType.get("NT").get(ntIdx);
				submitOrder(tdr,time, verbose); // deals with clearing and bookkeeping
			} else {
				// pick random trader (according to probs)
				ArrayList<Trader> tempTraders;
				double p = generator.nextDouble();
				if (p < p_NT) {
					// Pick NTs
					tempTraders = tradersByType.get("NT");
				} else if (p < (p_NT+p_MM)) {
					// Pick MMs
					tempTraders = tradersByType.get("MM");
				} else {
					// Pick FTs
					tempTraders = tradersByType.get("FT");
				}
				// Pick random trader from group
				int idx = generator.nextInt(tempTraders.size());
				tdr = tempTraders.get(idx);
				submitOrder(tdr,time, verbose);
			}
			//update all traders
			for (int tId : tIds) {
				tradersById.get(tId).update(lob);
			}
			
			if (lob.bidsAndAsksExist()) {
				data.addMidPrice(time, lob.getMid(), runNumber);
			}
			if (verbose) {
				System.out.println(this.toString());
				System.out.println("----------------------------------------------------------------------------------");
			}
		}
		data.endOfDay(runNumber, lob);
	}
	
	private void submitOrder(Trader tdr, int time, boolean verbose) {
		ArrayList<Order> quotes;
		quotes = tdr.getOrders(lob, time, verbose);
		for (Order q : quotes) {
			// add sign of order to orderSigns list
			data.quoteCollector.get(runNumber).add(q);
			OrderReport orderRep = lob.processOrder(q, false);
			clearing(tdr, orderRep, verbose);
		}
	}
	
	/**
	 * If an order didn't clear or was partially cleared and was put in the 
	 * book, the trader that submitted the order is first informed of this.
	 * Next, we run bookkeep on each trader so that hey can update their books.
	 * 
	 * Finally, we 
	 * 
	 * @param quotingTrader - the guy that submitted the last order
	 * @param orderRep - the order report
	 */
	private void clearing(Trader quotingTrader, OrderReport orderRep, boolean verbose) {
		if (verbose) {
			System.out.println("Clearing:");
		}
		if (orderRep.isOrderInBook()) {
			quotingTrader.addOrder(orderRep.getOrder());
			if (verbose) {
				System.out.println("Order " + orderRep.getOrder().getqId() +
									" added to book and Trader" + 
									quotingTrader.gettId() + " informed");
			}
		}
		for (Trade t : orderRep.getTrades()) {
			Trader buyer = tradersById.get(t.getBuyer());
			Trader seller = tradersById.get(t.getSeller());
			
			if (verbose) {
				System.out.println("Book before bookkeeping:");
				System.out.println(this.toString());
			}
			
			buyer.bookkeep(true, t.getQty(), t.getPrice(), t);
			seller.bookkeep(false, t.getQty(), t.getPrice(), t);

			if (verbose) {
				System.out.println("Book after bookkeeping:");
				System.out.println(this.toString());
			}
			
			Trader bookOrderOwner = tradersById.get(t.getProvider());
			int orderID = t.getOrderHit(); // Which order was affected 
			
			bookOrderOwner.modifyStoredQuote(orderID, t.getQty());
			
		}
		
	}
	

	private void populateMarket() {
		int tId = 0;
		ArrayList<Trader> noiseTraders = new ArrayList<Trader>();
		ArrayList<Trader> marketMakers = new ArrayList<Trader>();
		ArrayList<Trader> fundamentalTraders = new ArrayList<Trader>();
		for (int i=0;i<n_NTs;i++) {
			NoiseTrader nt = new NoiseTrader(tId, properties);
			noiseTraders.add(nt);
			tradersById.put(tId, nt);
			tId+=1;
		}
		for (int i=0;i<n_MMs;i++) {
			MarketMaker mm = new MarketMaker(tId, properties);
			marketMakers.add(mm);
			tradersById.put(tId, mm);
			tId+=1;
		}
		for (int i=0;i<n_FTs;i++) {
			FundamentalTrader ft = new FundamentalTrader(tId, properties);
			fundamentalTraders.add(ft);
			tradersById.put(tId, ft);
			tId+=1;
		}
		tradersByType.put("NT", noiseTraders);
		tradersByType.put("MM", marketMakers);
		tradersByType.put("FT", fundamentalTraders);
		this.tIds = new ArrayList<Integer>(tradersById.keySet());
	}
	
	public void reset() {
		lob.reset();
		tradersById.clear();
		tradersByType.clear();
		tIds.clear();
		populateMarket();
	}
	
	public String toString() {
		StringWriter fileStr = new StringWriter();
		fileStr.write(lob.toString());
		fileStr.write("Traders:\n");
		for (int tId : tIds) {
			fileStr.write(tradersById.get(tId).toString());
		}
		return fileStr.toString();
	}

}
