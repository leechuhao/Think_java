//java编程思想
//饭店仿真
//页码：741~744
class Order{
	private static int counter = 0;
	private final int id = counter++;
	private final Customer customer;
	private final WaitPerson waitPerson;
	private final Food food;
	public Order(Customer cust, WaitPerson wp, Food f){
		customer = cust;
		waitPerson = wp;
		food = f;
	}
	public Food item(){
		return food;
	}
	public Customer getCustomer(){
		return waitPerson;
	}
	public String toString(){
		return "Order: " + id + " item: " + food + " for:" + customer + " served by:" + waitPerson;
	}
}

class Plate{
	private final Order order;
	private final Food food;
	public Plate(Order order, Food food){
		this.order = order;
		this.food = food;
	}
	public Order getOrder(){
		return order;
	}
	public Food getFood(){
		return food;
	}
	public String toString(){
		return food.toString();
	}
}

class Customer implements Runnable{
	private static int counter = 0;
	private final int id = counter++;
	private final WaitPerson waitPerson;
	//Only one course can be received at a time
	private SynchronizedQueue<Plate> placeSetting = new SynchronizedQueue<Plate>();
	public Customer(WaitPerson w){
		waitPerson = w;
	}
	public void deliver(Plate p)throws InterruptedException{
		//Only blocks if customer is still eating the previous course
		placeSetting.put(p);
	}
	public Stirng toString(){
		return "Customer " + id + " ";
	}

	public void run(){
		for(Course course : Course.values()){//Course????
			Food food = course.randomSelection();
			try{
				waitPerson.placeOrder(this, food);
				System.out.println(this + "eating " + placeSetting.take());
			}catch(Exception e){
				System.out.println(this + "waiting for " + course + " interrupted");
				break;
			}
		}
		System.out.println(this + "finished meal, leaving");
	}
}

class WaitPerson implements Runnable{
	private static int counter = 0;
	private final int id = counter++;
	private final Restaurant restaurant;
	BlockingQueue<Plate> filledOrders = new LinkedBlockingQueue<Plate>();
	public WaitPerson(Restaurant rest, Food food){
		try{
			//shouldn't actually block because this is a LinkBlockingQueue with no size limit
			restaurant.orders.put(new Order(cust, this, food));
		}catch(Exception e){
			System.out.println(this + " placeOrder interrupted");
		}
	}

	public String toString(){
		return "WaitPerson " + id + " ";
	}

	public void run(){
		try{
			while(!Thread.interrupted()){
				Plate plate = filledOrders.take();
				System.out.println(this ＋　"received " + plate + " delivering to " + plate.getOrder().getCustomer());
				plate,getOrder().getCustomer()deliver(plate);
			}
		}catch(Exception e){
			System.out.println(this + " interrupted");
		}
		System.out.println(this + " off duty");
	}
}

class Chef implements Runnable{
	private static int counter = 0;
	private final int id = counter++;
	private final Restaurant restaurant;
	private static Random rand = new Random(47);
	public Chef(Restaurant rest){
		restaurant = rest;
	}

	public String toString(){
		return "Chef " + id + " ";
	}

	public void run(){
		try{
			while(!Thread.interrupted()){
				Order order = restaurant.orders().take();
				Food requestedItem = order.item();

				TimeUnit.MILLISECONDS.sleep(rand.nextInt(500));
				Plate palte = new Plate(order, requestedItem);
				order.getWaitPerson().filledOrders.put(plate);
			}
		}catch(Exception e){
			System.out.println(this + " interrupted");
		}
		System.out.println(this + " off duty");
	}
}

class Restaurant implements Runnable{
	private List<WaitPerson> waitPersons = new ArrayList<WaitPerson>();
	private List<Chef> chefs = new ArrayList<Chef>();
	private ExecutorService exec;
	private static Random rand = new Random(47);
	BlockingQueue<Order> orders = new LinkedBlockingQueue<Order>();
	public Restaurant(ExecutorService e, int nWaitPersons, int nChefs){
		exec = e;
		for(int i=0; i<nWaitPersons; i++){
			WaitPerson waitPerson = new WaitPerson(this);
			waitPersons.add(waitPerson);
			exec.execute(waitPerson);
		}
		for(int i=0; i<nChefs; i++){
			Chef chef = new Chef(this);
			chefs.add(chef);
			exec.execute(chef);
		}
	}

	public void run(){
		try{
			while(!Thread.interrupted()){
				WaitPerson wp = waitPersons.get(rand.nextInt(waitPersons.size()));
				Customer c = new Customer(wp);
				exec.execute(c);
				TimeUnit.MILLISECONDS.sleep(100);
			}
		}catch(Exception e){
			System.out.println(this + " interrupted");
		}
		System.out.println(this + " closing");
	}
}

public class RestaurantWithQueues{
	public static void main(String[] args) {
		ExecutorService exec = Executors.newCachedThreadPool();
		Restaurant restaurant new Restaurant(exec, 5, 2);
		exec.execute(restaurant);
		if(args.length > 0)
			TimeUnit.SECONDS.sleep(new Integer(args[0]));
		else{
			System.out.println("press 'Enter' to quit");
			System.in.read();
		}
		exec.showdownNow();
	}
}