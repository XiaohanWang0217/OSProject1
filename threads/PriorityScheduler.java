package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
		//System.out.println("initializing a priorityscheduler");
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {

	public ArrayList<ThreadState> arr;
	public ThreadState state;

	PriorityQueue(boolean transferPriority) {
		this.transferPriority = transferPriority;
		arr = new ArrayList<ThreadState>();
		state = null;
		//System.out.println("initializing a priorityqueue");
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	private int current_state;
	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
		// implement me
		
		//my code begin

		current_state = -1;
		ThreadState t=null, threadstate=null;
		int mx=-1;

		while ((t=pickNextThread()) != null) {
			int w = t.getEffectivePriority();
			if (w > mx) {
				mx = w;
				threadstate = t;
			}
		}

		/*if (idx != -1) {
			if (arr.get(idx).thread.getName() == "thread1")
				System.out.println("next thread is thread 1");
			if (arr.get(idx).thread.getName() == "thread2")
				System.out.println("next thread is thread 2");
			if (arr.get(idx).thread.getName() == "thread3")
				System.out.println("next thread is thread 3");
			if (mx == 0)
				System.out.println("value:0");
				
			if (mx == 1)
				System.out.println("value:1");
				
			if (mx == 2)
				System.out.println("value:2");
				
			if (mx == 3)
				System.out.println("value:3");
				
			if (mx == 4)
				System.out.println("value:4");
				
			if (mx == 5)
				System.out.println("value:5");
		}*/

		if (mx == -1)
			return null;
		else
			return arr.remove(arr.indexOf(threadstate)).thread;

		//my code end
	    //return null;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
		// implement me
		// my code begin
		if (++current_state < arr.size()) {
			return arr.get(current_state);
		}
		// my code end
	    return null;
	}
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    
		setPriority(priorityDefault);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {


	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
		// implement me
		// my code begin
		/*System.out.println("get a priority");
		if (thread.getName() == "thread1")
			System.out.println("name:thread1");
		else if (thread.getName() == "thread2")
			System.out.println("name:thread2");
		else if (thread.getName() == "thread3")
			System.out.println("name:thread3");
		else
			System.out.println("name:others");

		if (waitQueue.arr.size() == 0)
			System.out.println("arr size=0");
		else if (waitQueue.arr.size() == 1)
			System.out.println("arr size=1");
		else if (waitQueue.arr.size() == 2)
			System.out.println("arr size=2");
		else if (waitQueue.arr.size() == 3)
			System.out.println("arr size=3");
		else if (waitQueue.arr.size() == 4)
			System.out.println("arr size=4");
		else if (waitQueue.arr.size() == 5)
			System.out.println("arr size=5");
		else
			System.out.println("arr size>5");*/

		
		/*
		if (priority == -1)
			System.out.println("priority:empty");
		else if (priority == 0)
			System.out.println("priority:0");
		else if (priority == 1)
			System.out.println("priority:1");
		else if (priority == 2)
			System.out.println("priority:2");
		else if (priority == 3)
			System.out.println("priority:3");
		else if (priority == 4)
			System.out.println("priority:4");
		else if (priority == 5)
			System.out.println("priority:5");
		else
			System.out.println("priority:>5");
			
		System.out.println("get a priority end");*/

		//System.out.printf("name:%s, priority:%d",thread.getName(),mx);
		// my code end
	    return priority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
	    this.priority = priority;
	    
	    // implement me
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	protected ArrayList<PriorityQueue> HoldingQueues = new ArrayList<PriorityQueue>();
	protected ArrayList<ThreadState> waitQueue = new ArrayList<ThreadState>();

	public void updatepriority() {
		if (HoldingQueues == null)
			return;
		int mx = -1, w;
		ThreadState t = null;
		for (int i = 0; i < HoldingQueues.size(); i++) {
			t = HoldingQueues.get(i).state;
			for (int j = 0; j < t.waitQueue.size(); j++) {
				if (t.waitQueue.get(j) != this) {
					w = t.waitQueue.get(j).getEffectivePriority();
					if (w > mx)
						mx = w;
				}
			}
		}
		if (mx > this.getEffectivePriority())
			this.setPriority(mx);
	}
	public void waitForAccess(PriorityQueue waitQueue) {
		// implement me
		// my code begin
		/*System.out.println("Let's go for adding");
		
		if (thread.getName() == "thread1")
			System.out.println("name:thread1");
		else if (thread.getName() == "thread2")
			System.out.println("name:thread2");
		else if (thread.getName() == "thread3")
			System.out.println("name:thread3");
		else 
			System.out.println("name:others");*/
		waitQueue.arr.add(this);
		
		if (waitQueue.transferPriority == true) {
			HoldingQueues.add(waitQueue);
			if (waitQueue.state != null && waitQueue.state != this) {
				waitQueue.state.waitQueue.add(this);
				updatepriority();
			}
		}
		/*
		if (waitQueue.arr.size() == 0)
			System.out.println("now arr size=0");
		else if (waitQueue.arr.size() == 1)
			System.out.println("now arr size=1");
		else if (waitQueue.arr.size() == 2)
			System.out.println("now arr size=2");
		else if (waitQueue.arr.size() == 3)
			System.out.println("now arr size=3");
		else if (waitQueue.arr.size() == 4)
			System.out.println("now arr size=4");
		else if (waitQueue.arr.size() == 5)
			System.out.println("now arr size=5");
		else
			System.out.println("now arr size>5");
	
		System.out.println("ending for adding");*/
		// my code end
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
		// implement me
		// my code begin
		Lib.assertTrue(waitQueue.arr.isEmpty());
		waitQueue.state = this;
		// my code end
	}	

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	}
	
	public static void self_test() {
		System.out.println("\n\n---------------------------\n\n");
		System.out.println("happy to test PriorityScheduler");
		KThread thread1 = new KThread(
			new Runnable() {
				public void run() {
					System.out.println("running thread1!!");
				}
			}
		);
		KThread thread2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("running thread2!!");
			}
		});
		KThread thread3 = new KThread(new Runnable() {
			public void run() {
				System.out.println("running thread3!!");
			}
		});
		thread1.setName("thread1");
		thread2.setName("thread2");
		thread3.setName("thread3");

		boolean intStatue = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(thread1, 1);
		ThreadedKernel.scheduler.setPriority(thread2, 3);
		ThreadedKernel.scheduler.setPriority(thread3, 2);
		System.out.println("finish setting priority");
		Machine.interrupt().setStatus(intStatue);

		thread1.fork();
		thread2.fork();
		thread3.fork();

		thread1.join();
		thread2.join();
		thread2.join();
	}
}
