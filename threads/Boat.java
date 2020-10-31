package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	//System.out.println("\n ***Testing Boats with only 2 children***");
	//begin(0, 2, b);

	//System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
  	//begin(1, 2, b);

  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(3, 3, b);
	}
	
	private static boolean boat_position,is_pilot,is_adult_go,task_finished;
	private static Lock lock;
	private static Condition children_Oahu_lock,adult_Oahu_lock,children_Molokai_lock,task_finished_lock,boat_wait;
	private static int children_Oahu,children_Molokai,adult_Oahu,adult_Molokai;

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
		bg = b;

		lock = new Lock();
		children_Oahu = children;
		children_Oahu_lock = new Condition(lock);
		adult_Oahu = adults;
		adult_Oahu_lock = new Condition(lock);
		children_Molokai = 0;
		children_Molokai_lock = new Condition(lock);
		adult_Molokai = 0;
		task_finished_lock = new Condition(lock);
		boat_wait = new Condition(lock);
		for (int i = 0; i < adults; i++) {
			KThread t = new KThread(new Runnable() {
				public void run() {
					AdultItinerary();
				}
			});
			
			if (i==0)
				t.setName("0");
			else if (i==1)
				t.setName("1");
			else if (i==2)
				t.setName("2");
			else if (i==3)
				t.setName("3");
			else if (i==4)
				t.setName("4");
			else if (i==5)
				t.setName("5");
			else if (i==6)
				t.setName("6");
			else if (i==7)
				t.setName("7");
			else if (i==8)
				t.setName("8");
			else if (i==9)
				t.setName("9");
			else if (i==10)
				t.setName("10");
			t.fork();
		}
		for (int i = 0; i < children; i++) {
			KThread t = new KThread(new Runnable() {
				public void run() {
					ChildItinerary();
				}
			});
			if (i==0)
				t.setName("0");
			else if (i==1)
				t.setName("1");
			else if (i==2)
				t.setName("2");
			else if (i==3)
				t.setName("3");
			else if (i==4)
				t.setName("4");
			else if (i==5)
				t.setName("5");
			else if (i==6)
				t.setName("6");
			else if (i==7)
				t.setName("7");
			else if (i==8)
				t.setName("8");
			else if (i==9)
				t.setName("9");
			else if (i==10)
				t.setName("10");
			t.fork();
		}

		boat_position = false;// false: Oahu true : Molokai
		is_pilot = true;
		is_adult_go = false;
		task_finished = false;
		lock.acquire();
		while (children_Molokai != children || adult_Molokai != adults || boat_position == false)
			task_finished_lock.sleep();
		task_finished = true;
		children_Oahu_lock.wakeAll();
		children_Molokai_lock.wakeAll();
		adult_Oahu_lock.wakeAll();
		lock.release();
	}

	// Instantiate global variables here
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	/*Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();

    }*/

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
	lock.acquire();
	//System.out.println("begin a new adult");
	while (!is_adult_go || boat_position)
		adult_Oahu_lock.sleep();
	
		/*if (KThread.currentThread().getName()=="0")
			System.out.println("begin 0-th adult loop");
		else if (KThread.currentThread().getName()=="1")
			System.out.println("begin 1-th adult loop");
		else if (KThread.currentThread().getName()=="2")
			System.out.println("begin 2-th adult loop");
		else if (KThread.currentThread().getName()=="3")
			System.out.println("begin 3-th adult loop");
		else if (KThread.currentThread().getName()=="4")
			System.out.println("begin 4-th adult loop");
		else if (KThread.currentThread().getName()=="5")
			System.out.println("begin 5-th adult loop");
		else if (KThread.currentThread().getName()=="6")
			System.out.println("begin 6-th adult loop");
		else if (KThread.currentThread().getName()=="7")
			System.out.println("begin 7-th adult loop");
		else if (KThread.currentThread().getName()=="8")
			System.out.println("begin 8-th adult loop");
		else if (KThread.currentThread().getName()=="9")
			System.out.println("begin 9-th adult loop");*/

	bg.AdultRowToMolokai();
	adult_Oahu--;
	adult_Molokai++;
	if (adult_Oahu > 0 && children_Molokai > 1) {
		is_adult_go = true;
		//System.out.println("change is_adult_go to true");
	} else {
		//System.out.println("change is_adult_go to false");
		is_adult_go = false;
	}
	boat_position = true;
	
	//System.out.println("a adult ended");
	children_Molokai_lock.wake();
	
	lock.release();
    }

    static void ChildItinerary()
    {
		bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
		//DO NOT PUT ANYTHING ABOVE THIS LINE. 
		lock.acquire();
		//System.out.println("begin a child");
		boolean position = false;
		boolean is_first_go = true;
		while (children_Oahu > 0 || adult_Oahu > 0) {
			/*if (KThread.currentThread().getName()=="0")
				System.out.println("begin 0-th child loop");
			else if (KThread.currentThread().getName()=="1")
				System.out.println("begin 1-th child loop");
			else if (KThread.currentThread().getName()=="2")
				System.out.println("begin 2-th child loop");
			else if (KThread.currentThread().getName()=="3")
				System.out.println("begin 3-th child loop");
			else if (KThread.currentThread().getName()=="4")
				System.out.println("begin 4-th child loop");
			else if (KThread.currentThread().getName()=="5")
				System.out.println("begin 5-th child loop");
			else if (KThread.currentThread().getName()=="6")
				System.out.println("begin 6-th child loop");
			else if (KThread.currentThread().getName()=="7")
				System.out.println("begin 7-th child loop");
			else if (KThread.currentThread().getName()=="8")
				System.out.println("begin 8-th child loop");
			else if (KThread.currentThread().getName()=="9")
				System.out.println("begin 9-th child loop");*/
    
			if (position == false) {
				/*if (boat_position)
					System.out.println("boat at Molokai");
				else
					System.out.println("boat at Oahu");*/
				while (boat_position || is_adult_go)
					children_Oahu_lock.sleep();
				if (!task_finished) {


			/*if (KThread.currentThread().getName()=="0")
				System.out.println("mid 0-th child loop");
			else if (KThread.currentThread().getName()=="1")
				System.out.println("mid 1-th child loop");
			else if (KThread.currentThread().getName()=="2")
				System.out.println("mid 2-th child loop");
			else if (KThread.currentThread().getName()=="3")
				System.out.println("mid 3-th child loop");
			else if (KThread.currentThread().getName()=="4")
				System.out.println("mid 4-th child loop");
			else if (KThread.currentThread().getName()=="5")
				System.out.println("mid 5-th child loop");
			else if (KThread.currentThread().getName()=="6")
				System.out.println("mid 6-th child loop");
			else if (KThread.currentThread().getName()=="7")
				System.out.println("mid 7-th child loop");
			else if (KThread.currentThread().getName()=="8")
				System.out.println("mid 8-th child loop");
			else if (KThread.currentThread().getName()=="9")
				System.out.println("mid 9-th child loop");*/

					if (is_pilot) {
						//System.out.println("row to Molokai");
						bg.ChildRowToMolokai();
						children_Oahu--;
						children_Molokai++;
						is_pilot = false;
						position = true;
						if (children_Oahu>0) {
							//System.out.println("wait for partner");
							children_Oahu_lock.wake();
							boat_wait.sleep();
							//System.out.println("has found a partner");
						} else {
							//System.out.println("single travel");
							is_pilot = true;
							if (adult_Oahu == 0) {
								task_finished_lock.wake();
							}
						}
						//children_Molokai_lock.sleep();
						//System.out.println("next task given");
					} else {
						//System.out.println("ride to Molokai");
						bg.ChildRideToMolokai();
						position = true;
						boat_position = true;
						children_Oahu--;
						children_Molokai++;
						is_pilot = true;
						boat_wait.wake();
						if (adult_Oahu == 0 && children_Oahu == 0) {
							//System.out.println("task finished !!");
							task_finished_lock.wake();
							children_Molokai_lock.sleep();
						} else {
							if (adult_Oahu > 0) {
								//System.out.println("change is_adult_go to true");
								is_adult_go = true;
							}
						
							children_Molokai_lock.wake();
						}
					}
				}
			} else {
				/*if (boat_position)
					System.out.println("boat at Molokai");
				else
					System.out.println("boat at Oahu");*/
					while (!boat_position)
						children_Molokai_lock.sleep();
					if (children_Oahu > 0 || adult_Oahu > 0) {
						//System.out.println("back to Oahu");
						bg.ChildRowToOahu();
						position = false;
						boat_position = false;
						children_Molokai--;
						children_Oahu++;

						if (adult_Oahu == 0) {
							//System.out.println("(children next");
							is_adult_go = false;
							//System.out.println("change is_adult_go to false");
							children_Oahu_lock.wake();
						} else {
							if (is_adult_go) {
								//System.out.println("(adult next");
								adult_Oahu_lock.wake();
								children_Oahu_lock.sleep();
							}
							else {
								//System.out.println("(children next");
								children_Oahu_lock.wake();
							}
						}
					}
			}
		}
		lock.release();
	}

	public static void SampleItinerary()
	{
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
	}
}
