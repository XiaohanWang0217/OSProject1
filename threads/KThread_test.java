package nachos.threads;

import nachos.machine.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an argument
 * when creating <tt>KThread</tt>, and forked. For example, a thread that
 * computes pi could be written as follows:
 *
 * <p>
 * <blockquote>
 * 
 * <pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre>
 * 
 * </blockquote>
 * <p>
 * The following code would then create a thread and start it running:
 *
 * <p>
 * <blockquote>
 * 
 * <pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre>
 * 
 * </blockquote>
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return the current thread.
     */
    public static KThread currentThread() {
        Lib.assertTrue(currentThread != null);
        return currentThread;
    }

    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
        if (currentThread != null) {
            // 再次调用构造函数创建新线程
            // 每个线程有自己的nachos.machine.TCB对象
            // 为上下文切换、线程创建、线程销毁和线程生成提供低级支持
            // 每个TCB控制一个底层JVM线程对象
            tcb = new TCB();
        } else {

            // my code begin 
            joinQueue = ThreadedKernel.scheduler.newThreadQueue(true);
            joinQueue.acquire(this);
            // my code end


            // 该构造函数第一次被调用
            // 创建线程的就绪队列
            readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
            // 将CPU分配给正在创建的新线程对象
            readyQueue.acquire(this);
            // 将正在创建的新线程对象设置为当前线程对象
            currentThread = this;
            // 将正在创建的新线程对象的TCB对象设置为当前TCB对象
            // 这样就把当前正在运行的底层JVM线程分配给了正在创建的新线程对象
            tcb = TCB.currentTCB();
            // 第一个线程的名称
            name = "main";
            // 执行线程启动前的准备工作
            // 将线程状态更改为statusRunning
            restoreState();
            // 创建空闲线程
            createIdleThread();
        }
    }

    /**
     * Allocate a new KThread.
     *
     * @param target the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
        this();
        this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param target the object whose <tt>run</tt> method is called.
     * @return this thread.
     */
    public KThread setTarget(Runnable target) {
        Lib.assertTrue(status == statusNew);

        this.target = target;
        return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes only.
     *
     * @param name the name to give to this thread.
     * @return this thread.
     */
    public KThread setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes only.
     *
     * @return the name given to this thread.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return the full name given to this thread.
     */
    public String toString() {
        return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another thread.
     */
    public int compareTo(Object o) {
        KThread thread = (KThread) o;

        if (id < thread.id)
            return -1;
        else if (id > thread.id)
            return 1;
        else
            return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads are
     * running concurrently: the current thread (which returns from the call to the
     * <tt>fork</tt> method) and the other thread (which executes its target's
     * <tt>run</tt> method).
     */
    public void fork() {
        Lib.assertTrue(status == statusNew);
        Lib.assertTrue(target != null);

        Lib.debug(dbgThread, "Forking thread: " + toString() + " Runnable: " + target);

        System.out.println("in fork function of "+this.getName());

        boolean intStatus = Machine.interrupt().disable();

        tcb.start(new Runnable() {
            public void run() {
                runThread();
            }
        });

        ready();

        Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
        begin();
        target.run();
        finish();
    }

    private void begin() {
        Lib.debug(dbgThread, "Beginning thread: " + toString());

        Lib.assertTrue(this == currentThread);

        restoreState();

        Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is safe to
     * do so. This method is automatically called when a thread's <tt>run</tt>
     * method returns, but it may also be called directly.
     *
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to delete
     * this thread.
     */
    public static void finish() {
        Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());

        Machine.interrupt().disable();

        Machine.autoGrader().finishingCurrentThread();

        Lib.assertTrue(toBeDestroyed == null);
        toBeDestroyed = currentThread;

        currentThread.status = statusFinished;

        // my code begin

        //System.out.println(currentThread.getName()+" has finished");

        KThread thread = joinQueue.nextThread();
        if ( null != thread)
        {
            thread.ready();
        }
        // my code end

        sleep();
    }

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise returns
     * when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add itself
     * to the ready queue and switch to the next thread. On return, restores
     * interrupts to the previous state, in case <tt>yield()</tt> was called with
     * interrupts disabled.
     */
    public static void yield() {
        Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
        Lib.assertTrue(currentThread.status == statusRunning);
        // 禁用中断，返回本操作执行前中断的状态（禁用的/启用的）
        boolean intStatus = Machine.interrupt().disable();
        // 将当前线程的状态设置为就绪，并该当前线程加入调度器的就绪队列中
        // 如果当前线程是空闲线程，则不执行入队操作
        currentThread.ready();
        // 从调度器的就绪队列中取下一个线程（若返回null，则替之以空闲线程）并启动
        runNextThread();
        // 中断状态恢复到进入本方法时的状态
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it is
     * blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e. a
     * <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually some
     * thread will wake this thread up, putting it back on the ready queue so that
     * it can be rescheduled. Otherwise, <tt>finish()</tt> should have scheduled
     * this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
        Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());

        Lib.assertTrue(Machine.interrupt().disabled());

        if (currentThread.status != statusFinished)
            currentThread.status = statusBlocked;

        runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's ready
     * queue.
     */
    public void ready() {
        Lib.debug(dbgThread, "Ready thread: " + toString());

        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(status != statusReady);

        status = statusReady;
        System.out.println("in ready function of "+getName());
        if (this != idleThread)
            readyQueue.waitForAccess(this);

        Machine.autoGrader().readyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished, return
     * immediately. This method must only be called once; the second call is not
     * guaranteed to return. This thread must not be the current thread.
     */
    // my code begin
    private boolean joinFlag = false;
    private static ThreadQueue joinQueue = null;
    // my code end
    public void join() {
        Lib.debug(dbgThread, "Joining to thread: " + toString());

        Lib.assertTrue(this != currentThread);

        // my-code-begin
        //System.out.println("in join function of "+this.getName());
        if (statusFinished == this.status)
        {
            return;
        }

        boolean intStatus = Machine.interrupt().disable();

        if (!currentThread.joinFlag)
        {
            //System.out.println(this.getName()+" is joined in "+currentThread().getName());
            joinQueue.waitForAccess(currentThread);
            joinFlag = true;
            sleep();
        }

        Machine.interrupt().restore(intStatus);
        // my-code-end
    }

    /**
     * Create the idle thread. Whenever there are no threads ready to be run, and
     * <tt>runNextThread()</tt> is called, it will run the idle thread. The idle
     * thread must never block, and it will only be allowed to run when all other
     * threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
        Lib.assertTrue(idleThread == null);
        // 创建一个新线程
        idleThread = new KThread(new Runnable() {
            public void run() {
                while (true)
                    yield();
            }
        });
        // 设置新线程的名称为空闲
        idleThread.setName("idle");

        Machine.autoGrader().setIdleThread(idleThread);

        // 将空闲线程从主线程分离
        idleThread.fork();
    }

    /**
     * Determine the next thread to run, then dispatch the CPU to the thread using
     * <tt>run()</tt>.
     */
    private static void runNextThread() {
        KThread nextThread = readyQueue.nextThread();
        if (nextThread == null)
            nextThread = idleThread;
        System.out.println("finding next thread --- "+nextThread.getName());

        nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread, switch
     * to the new thread by calling <tt>TCB.contextSwitch()</tt>, and load the state
     * of the new thread. The new thread becomes the current thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must still
     * call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been changed
     * from running to blocked or ready (depending on whether the thread is sleeping
     * or yielding).
     *
     * @param finishing <tt>true</tt> if the current thread is finished, and should
     *                  be destroyed by the new thread.
     */
    private void run() {
        Lib.assertTrue(Machine.interrupt().disabled());
        System.out.println("in run function of "+getName());

        Machine.yield();

        currentThread.saveState();

        Lib.debug(dbgThread, "Switching from: " + currentThread.toString() + " to: " + toString());

        currentThread = this;

        tcb.contextSwitch();

        currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to <tt>statusRunning</tt>
     * and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {
        Lib.debug(dbgThread, "Running thread: " + currentThread.toString());

        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(this == currentThread);
        Lib.assertTrue(tcb == TCB.currentTCB());

        Machine.autoGrader().runningThread(this);

        status = statusRunning;

        if (toBeDestroyed != null) {
            toBeDestroyed.tcb.destroy();
            toBeDestroyed.tcb = null;
            toBeDestroyed = null;
        }
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not need to
     * do anything here.
     */
    protected void saveState() {
        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(this == currentThread);
    }

    private static class PingTest implements Runnable {
        PingTest(int which) {
            this.which = which;
        }

        public void run() {
            for (int i = 0; i < 5; i++) {
                System.out.println("*** thread " + which + " looped " + i + " times");
                currentThread.yield();
            }
        }

        private int which;
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
        Lib.debug(dbgThread, "Enter KThread.selfTest");

        new KThread(new PingTest(1)).setName("forked thread").fork();
        new PingTest(0).run();

        // 加入对自己写的join的测试
        System.out.println("\n Tests Of Thread Join");
        testOfJoin1();
        testOfJoin2();
    }

    // my test code begin
    private static void testOfJoin1() {
        System.out.println("*** test 1 of thread join!");

        KThread thread1 = new KThread(
            new Runnable() {
                public void run() {
                    System.out.println("*** thread1 in testOfJoin1 run!");
                }
            }
        );
        thread1.fork();

        KThread thread2 = new KThread(
            new Runnable() {
                public void run() {
                    System.out.println("*** thread2 in testOfJoin1 run!");
                    thread1.join();
                }
            }
        );
        thread2.fork();

        thread2.join();

        Lib.assertTrue((thread1.status == statusFinished), " thread1 should be finished.");
    }

    private static void testOfJoin2() {
        System.out.println("*** test 2 of thread join!");

        KThread thread1 = new KThread(
            new Runnable() {
                public void run() {
                    System.out.println("*** thread1 in testOfJoin2 run!");
                }
            }
        );
        thread1.fork();

        KThread thread2 = new KThread(
            new Runnable() {
                public void run() {
                    System.out.println("*** thread2 in testOfJoin2 run!");
                    thread1.join();
                }
            }
        );
        thread2.fork();

        KThread thread3 = new KThread(
            new Runnable() {
                public void run() {
                    System.out.println("*** thread3 in testOfJoin2 run!");
                    thread2.join();
                }
            }
        );
        thread3.fork();

        thread3.join();

        Lib.assertTrue((thread1.status == statusFinished), " thread1 should be finished.");
        Lib.assertTrue((thread2.status == statusFinished), " thread2 should be finished.");
    }
    // my test code end

    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;

    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;

    /**
     * The status of this thread. A thread can either be new (not yet forked), ready
     * (on the ready queue but not running), running, or blocked (not on the ready
     * queue and not running).
     */
    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;

    /**
     * Unique identifer for this thread. Used to deterministically compare threads.
     */
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. */
    private static int numCreated = 0;

    private static ThreadQueue readyQueue = null;
    private static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;
}
