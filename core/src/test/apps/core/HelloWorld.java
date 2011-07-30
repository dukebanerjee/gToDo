package test.apps.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class HelloWorld {
	private static List<String> receivedSymbols = new ArrayList<String>(5);
	private static List<Double> receivedPrices = new ArrayList<Double>(5);
	
	public static void main(String[] args) throws Exception {
		String[] stockSymbols = new String[] { "ORCL", "MSFT", "IBM", "AAPL", "GOOG" };
		for(int i = 0; i < stockSymbols.length; i++) {
			Thread t = new Thread() {
				public void run() {
					try {
						URLConnection cn = new URL("http://finance.yahoo.com/d/quotes.csv?s=" + getName() + "&f=l1yr").openConnection();
						BufferedReader br = new BufferedReader(new InputStreamReader(cn.getInputStream()));
						double price = Double.parseDouble(br.readLine().split(",")[0].trim());
						
						synchronized (receivedSymbols) {
							receivedSymbols.add(getName());
							receivedPrices.add(price);
							receivedSymbols.notifyAll();
						}
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
			};
			t.setName(stockSymbols[i]);
			t.start();
		}
		for(int i = 0; i < stockSymbols.length; i++) {
			synchronized (receivedSymbols) {
				if(receivedSymbols.size() == 0) {
					receivedSymbols.wait();
				}
				System.out.println(receivedSymbols.remove(0));
				System.out.println(receivedPrices.remove(0));
			}
		}
	}
}
