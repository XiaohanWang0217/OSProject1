package nachos.threads;

import nachos.machine.*;

import java.util.Random;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {

    }

    @Override
    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());
                   
        Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
        
        getThreadState(thread).setPriority(priority);
        }
    @Override
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
    @Override
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
    @Override
	protected LotteryThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);

		return (LotteryThreadState) thread.schedulingState;
    }
    
    @Override
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	// implement me
        return new LotteryQueue(transferPriority);
    }
    
    protected class LotteryQueue extends PriorityScheduler.PriorityQueue {
        LotteryQueue(boolean transferPriority) {
            super(transferPriority);
        }
        @Override
        protected LotteryThreadState pickNextThread() {
            int n = arr.size();
            if (n == 0)
                return null;
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = getThreadState(arr.get(i)).getEffectivePriority();
                if (i > 0)
                    a[i] = a[i] + a[i-1];
            }
            int rd = random.nextInt(a[n-1]);
            //System.out.println("rand "+String.valueOf(rd)+" in range "+String.valueOf(a[n-1]));

            int t = 0;
            while (a[t] <= rd)
                t = t + 1;
            return getThreadState(arr.get(t));
        }
    }

    protected class LotteryThreadState extends PriorityScheduler.ThreadState {
        public LotteryThreadState(KThread thread) {
            super(thread);
        }
        @Override
        public int getEffectivePriority() {
            Lib.assertTrue(Machine.interrupt().disabled());
            int w = priority;

            for (int i = 0; i < HoldingQueues.size(); i++) {
                PriorityQueue q = HoldingQueues.get(i);
                for (int j = 0; j < q.arr.size(); j++) {
                    w+=getThreadState(q.arr.get(j)).getEffectivePriority();
                }
            }
            for (int j = 0; j < ((PriorityQueue)this.thread.joinQueue).arr.size(); j++) {
                w += getThreadState(((PriorityQueue)this.thread.joinQueue).arr.get(j)).getEffectivePriority();
            }
            return w;
        }
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */


    protected Random random = new Random(19);
}
