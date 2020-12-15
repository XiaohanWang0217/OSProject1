package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.HashSet;
import java.util.LinkedList;
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
	PriorityQueue(boolean transferPriority) {
		this.transferPriority = transferPriority;
		srcThd = null;
		srcQueue = new LinkedList<KThread>();
	}

	public void waitForAccess(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
		Lib.assertTrue(Machine.interrupt().disabled());
		// First remove this queue from finished thread:
		if(srcThd != null){
			srcThd.waitingQueueSet.remove(this); 	
			srcThd.invalidE();
		}
		ThreadState nextT = pickNextThread();
		if (nextT == null){
			return null;	
		}
		nextT.acquire(this);	// Set nextT's waitSet
		return nextT.thread;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	
	protected ThreadState pickNextThread() {
		Iterator i = srcQueue.iterator();
		if (i.hasNext()){
			KThread nextT = (KThread)i.next(), tmp; 
			int nextEff = getThreadState(nextT).getEffectivePriority();
			for(;i.hasNext();){
				tmp = (KThread)i.next(); 
				int tmpEff = getThreadState(tmp).getEffectivePriority();
				if (tmpEff > nextEff){
					nextT = tmp;
					nextEff = tmpEff;
				}
			}
			return getThreadState(nextT);
		}
	    return null;
	}
	
	public void print() {
		Lib.assertTrue(Machine.interrupt().disabled());
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
	public ThreadState srcThd; // The thread who hold this source now
	public LinkedList<KThread> srcQueue;
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
		waitingQueueSet = new LinkedList<PriorityQueue>();

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
		Lib.assertTrue(Machine.interrupt().disabled());

		if (effpriority != invalidEff)	return effpriority;
		effpriority = priority;
		int tmpEffp;
		for (Iterator i = waitingQueueSet.iterator();i.hasNext();)
			for (Iterator j = ((PriorityQueue)i.next()).srcQueue.iterator();j.hasNext();){
				//ThreadState tmp = getThreadState((KThread)j.next());
				tmpEffp = getThreadState((KThread)j.next()).getEffectivePriority();
				if(tmpEffp > effpriority)
					effpriority = tmpEffp;
			}
		
		for (Iterator j = ((PriorityQueue)this.thread.waitForJoin).srcQueue.iterator();j.hasNext();){
			//ThreadState tmp = getThreadState((KThread)j.next());
			tmpEffp = getThreadState((KThread)j.next()).getEffectivePriority();
			if(tmpEffp > effpriority)
				effpriority = tmpEffp;
		}
	
		return effpriority;
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
		this.invalidE();
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
	public void waitForAccess(PriorityQueue waitQueue) {
		waitQueue.srcQueue.add(this.thread);
		if (waitQueue.srcThd != null)
			waitQueue.srcThd.invalidE();
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
		waitQueue.srcQueue.remove(this.thread);
		waitQueue.srcThd = this;
		this.waitingQueueSet.add(waitQueue);
		this.invalidE();
		//}
	}	

	public void invalidE(){
		effpriority = invalidEff;
	}
	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	public int effpriority;
	protected LinkedList<PriorityQueue> waitingQueueSet;	// All queues of srcs that this thread is holding.
	public int invalidEff = -1;  
	}
	

	
	public static void self_test() {
		System.out.println("\n\n---------------------------\n\n");
		System.out.println("happy to test PriorityScheduler");
		KThread thread1 = new KThread(
			new Runnable() {
				public void run() {
					System.out.println("-----------------------------------------running thread1!!");
				}
			}
		);
		KThread thread2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("-----------------------------------------running thread2!!");
			}
		});
		KThread thread3 = new KThread(new Runnable() {
			public void run() {
				thread2.join();
				System.out.println("-----------------------------------------running thread3!!");
			}
		});
		thread1.setName("thread1");
		thread2.setName("thread2");
		thread3.setName("thread3");

		boolean intStatue = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(thread1, 3);
		ThreadedKernel.scheduler.setPriority(thread2, 1);
		ThreadedKernel.scheduler.setPriority(thread3, 4);
		System.out.println("finish setting priority");
		Machine.interrupt().setStatus(intStatue);

		thread1.fork();
		thread2.fork();
		thread3.fork();

		thread1.join();
		//thread2.join();
		thread3.join();
	}
}
