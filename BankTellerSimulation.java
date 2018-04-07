package shutDown;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.*;

//home-make java Demo
//java缂栫▼鎬濇兂
//閾惰浠跨湡绯荤粺
//椤电爜锛�37~740

class Customer{
	private final int serviceTime;
	public Customer(int i){
		serviceTime = i;
	}
	public int getServiceTime(){
		return serviceTime;
	}
	public String toString(){
		return "[" + serviceTime + "]";
	}
}


class CustomerLine extends ArrayBlockingQueue<Customer>{
	public CustomerLine(int maxLineSize){
		super(maxLineSize);
	}
	public String toString(){
		if(this.size()==0){
			return "[Empty]";
		}
		StringBuilder result = new StringBuilder();
		for(Customer cus : this){
			result.append(cus);
		}
		return result.toString();
	}
}

class CustomerGenerator implements Runnable{
	private CustomerLine customers;
	private static Random rand = new Random(47);
	public CustomerGenerator(CustomerLine cl){
		customers = cl;
	}
	public void run(){
		try{
			while(!Thread.interrupted()){
				TimeUnit.MILLISECONDS.sleep(rand.nextInt(300));
				customers.put(new Customer(rand.nextInt(1000)));
			}
		}catch(InterruptedException e){
			System.out.println("CustomerGenerator interrupted");
		}
		System.out.println("CustomerGenerator terminating");
	}
}

class Teller implements Runnable, Comparable<Teller>{
	private static int counter = 0;
	private final int id = counter++;

	private int customersServed = 0;
	private CustomerLine customers;
	private boolean servingCustomerLine = true;

	public Teller(CustomerLine cl){
		customers = cl;
	}

	public void run(){
		try{
			while(!Thread.interrupted()){
				Customer customer = customers.take();
				TimeUnit.MILLISECONDS.sleep(customer.getServiceTime());

				synchronized(this){
					customersServed++;
					while(!servingCustomerLine)
						wait();
				}
			}
		}catch(Exception e){
			System.out.println(this + " interrupted");
		}
		System.out.println(this + "terminating");
	}

	public synchronized void doSomethingElse(){
		customersServed = 0;
		servingCustomerLine = false;
	}

	public synchronized void serveCustomerLine(){
		assert !servingCustomerLine:"already serving: " + this;
		servingCustomerLine = true;
		notifyAll();
	}

	public String toString(){
		return "Teller " + id + " ";
	}

	public String shortString(){
		return "T" + id;
	}
//used by priority queue
	public synchronized int compareTo(Teller other){
		return customersServed<other.customersServed ? -1 : (customersServed==other.customersServed ? 0 : 1);
	}
}

class TellerManger implements Runnable{
	private ExecutorService exec;
	private CustomerLine customers;
	private PriorityQueue<Teller> workingTellers = new PriorityQueue<Teller>();
	private Queue<Teller> tellersDoingOtherThings = new LinkedList<Teller>();
	private int adjustmentPeriod;
	private static Random rand = new Random(47);

	public TellerManger(ExecutorService e, CustomerLine customers, int adjustmentPeriod){
		exec = e;
		this.customers = customers;
		this.adjustmentPeriod = adjustmentPeriod;
		//start with a single Teller
		Teller teller = new Teller(customers);
		exec.execute(teller);
		workingTellers.add(teller);
	}

	public void adjustTellerNumber(){
		//
		if(customers.size()/workingTellers.size() > 2){
			if(tellersDoingOtherThings.size() > 0){
				//
				Teller teller = tellersDoingOtherThings.remove();
				teller.serveCustomerLine();
				//
				workingTellers.offer(teller);
				return;
			}
			Teller teller = new Teller(customers);
			exec.execute(teller);
			workingTellers.add(teller);
			return;
		}

		if(workingTellers.size() > 1 && customers.size()/workingTellers.size() < 2)
			reassignOneTeller();//
		if(customers.size() == 0)
			while(workingTellers.size() > 1)
				reassignOneTeller();
	}

	public void reassignOneTeller(){
		Teller teller = workingTellers.poll();//
		teller.doSomethingElse();
		tellersDoingOtherThings.offer(teller);
	}

	public void run(){
		try{
			while(!Thread.interrupted()){
				TimeUnit.MILLISECONDS.sleep(adjustmentPeriod);
				adjustTellerNumber();
				System.out.println(customers + "{");
				for(Teller teller : workingTellers){
					System.out.print(teller.shortString() + " ");
				System.out.println("}");
				}
			}
		}catch(Exception e){
			System.out.println(this + "interrupted");
		}
		System.out.println(this + "terminating");
	}

	public String toString(){
		return "TellerManger ";
	}
}

public class BankTellerSimulation{
	static final int MAX_LINE_SIZE = 50;
	static final int ADJUSTMENT_PERIOD = 1000;
	public static void main(String[] args) throws Exception{
		ExecutorService exec = Executors.newCachedThreadPool();
		//if line too long ,the customers will leave
		CustomerLine customers = new CustomerLine(MAX_LINE_SIZE);
		exec.execute(new TellerManger(
			exec, customers, ADJUSTMENT_PERIOD
		));
		//
		if(args.length > 0)
			TimeUnit.SECONDS.sleep(new Integer(args[0]));
		else{
			System.out.println("Press 'Enter' to quit");
			System.in.read();
		}
		exec.shutdownNow();
	}
}