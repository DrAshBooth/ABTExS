
package traders;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import lob.*;

/**
 * @author Ash Booth
 *
 */
public abstract class Trader {
	
	protected int tId;
	private double cash;
	private int numAssets;
	private ArrayList<Trade> blotter = new ArrayList<Trade>();
	// key: qId, value: order currently in the book
	public HashMap<Integer, storedQuote> ordersInBook = new HashMap<Integer, storedQuote>();
	protected Random generator = new Random();
	
	public Trader(int tId, Properties prop) {
		super();
		this.tId = tId;
		this.cash = Double.valueOf(prop.getProperty("starting_cash"));
		this.numAssets = Integer.valueOf(prop.getProperty("starting_assets"));
	}
	
	public void addOrder(Order order) {
		int qId = order.getqId();
		ordersInBook.put(qId, new storedQuote(order.getTimestamp(), 
											  order.getQuantity(),
											  order.getSide(), 
											  order.getPrice(),
											  qId));
	}
	
//	public void delOrder(int qId) {
//		ordersInBook.remove(qId);
//	}
	
	
	public void modifyStoredQuote(int id, int qtyToRemove) {
		if (ordersInBook.containsKey(id)) {
			storedQuote sq = ordersInBook.get(id);
			int qtyRemaining = sq.getQuantity()-qtyToRemove;
			if (qtyRemaining==0) {
				ordersInBook.remove(id);
			} else if (qtyRemaining > 0) {
				sq.setQuantity(qtyRemaining);
			} else {
				throw new IllegalStateException("Trade qty > that order qty" + 
												" in traders  book");
			}
		} else {
			throw new IllegalStateException("Trader told his order was hit" +
											"but he has no record of the order!");
		}
	}
	
	
	public void bookkeep(boolean bought, int qty, 
						 double price, Trade t) {
		if (bought) {
			this.cash-= (qty*price);
			this.numAssets += price;
		} else {
			this.cash += (qty*price);
			this.numAssets -= qty;
		}
		blotter.add(t);
		iTraded(bought, price, qty);
	}
	
	
	protected storedQuote oldestOrder() {
		int oldestID = -1;
		int oldestTime = Integer.MAX_VALUE;
		for (Map.Entry<Integer, storedQuote> entry : ordersInBook.entrySet()) {
			int quoteTime = entry.getValue().getTimestamp();
			if (quoteTime< oldestTime) {
				oldestTime = quoteTime;
				oldestID = entry.getKey();
			}
		}
		return ordersInBook.get(oldestID);
	}
	
	/**
	 * Called as part of bookkeep, some agents need to update specific internal
	 * Parameters in response to their trades executing.
	 * @param t
	 */
	protected abstract void iTraded(boolean bought, double price, int qty);
	
	public abstract ArrayList<Order> getOrders(OrderBook lob, int time, boolean verbose);
	
	/**
	 * Update the internal parameters of the trader given changes in the lob
	 * 
	 * @param lob	// the limit order book
	 * @param trade	// did a 
	 */
	public abstract void update(OrderBook lob);

	
	protected boolean noOrdersInBook() {
		return this.ordersInBook.isEmpty();
	}
	
	class storedQuote {
		private int timestamp;
		private int quantity;
		private String side;
		private double price;
		private int qId;

		public storedQuote(int time, int quantity, 
						   String side, double price,
						   int qId) {
			this.timestamp = time;
			this.side = side;
			this.quantity = quantity;
			this.price = price;
			this.qId = qId;
		}
		
		@Override
		public String toString() {
			return quantity + "\t@\t" + price + 
					"\tt=" + timestamp + "\tqId=" + qId +"\n";
		}

		public int getQuantity() {
			return quantity;
		}
		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}
		public int getTimestamp() {
			return timestamp;
		}
		public String getSide() {
			return side;
		}
		public double getPrice() {
			return price;
		}
		public int getqId() {
			return qId;
		}
	}

	@Override
	public String toString() {
		StringWriter fileStr = new StringWriter();
		fileStr.write(" -------- Trader " + tId + "--------\n");
		fileStr.write(" Cash = " + cash + "\n");
		fileStr.write(" Number of assets = " + numAssets + "\n");
		fileStr.write(" Blotter:\n" + blotter + "\n");
		fileStr.write(" Orders currently in book:\n");
		for (Map.Entry<Integer, storedQuote> entry : ordersInBook.entrySet()) {
			fileStr.write(entry.getValue().toString());
		}
		fileStr.write("\n --------------------------------\n");
		return fileStr.toString();
	}

	public int gettId() {
		return tId;
	}
	
}
