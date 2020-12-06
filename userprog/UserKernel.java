package nachos.userprog;

import java.util.HashMap;
import java.util.LinkedList;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {

  /**
   * Allocate a new user kernel.
   */
  public UserKernel() {
    super();
  }

  /**
   * Initialize this kernel. Creates a synchronized console and sets the
   * processor's exception handler.
   */

  // my code begin 第二题

  // 空闲物理页面的全局链表
  private static LinkedList<Integer> freePhysicalPageList = new LinkedList<Integer>();
  private static Lock freePhyPagesLock; // 链表的访问锁

  // my code end

  public void initialize(String[] args) {
    super.initialize(args);

    console = new SynchConsole(Machine.console());

    Machine
      .processor()
      .setExceptionHandler(
        new Runnable() {
          public void run() {
            exceptionHandler();
          }
        }
      );

    // my code begin 第二题
    // 初始化空闲物理页面全局链表
    int physicalPageNum = Machine.processor().getNumPhysPages(); // 物理内存总页数
    for (int i = 0; i < physicalPageNum; i++) {
      freePhysicalPageList.add(new Integer(i)); // 初始化时所有页面都是空闲的
    }

    // 初始化空闲物理页面链表的访问锁
    freePhyPagesLock = new Lock();
    // my code end
  }

  // my code begin 第二题
  // 请求指定数量的空闲物理页，若成功则返回物理页序号列表，若失败则返回null
  public static LinkedList<Integer> allocatePages(int num) {
    if (freePhysicalPageList.size() < num) return null; // 空闲物理页数不足

    freePhyPagesLock.acquire();

    LinkedList<Integer> result = new LinkedList<Integer>();
    for (int i = 0; i < num; i++) {
      result.add(freePhysicalPageList.removeFirst());
    }

    freePhyPagesLock.release();

    return result;
  }

  // 释放指定的物理页
  public static void releasePages(LinkedList<Integer> pagesToRelease) {
    freePhyPagesLock.acquire();

    freePhysicalPageList.addAll(pagesToRelease);

    freePhyPagesLock.release();
    return;
  }

  // my code end

  /**
   * Test the console device.
   */
  public void selfTest() {
    super.selfTest();

    System.out.println("Testing the console device. Typed characters");
    System.out.println("will be echoed until q is typed.");

    char c;

    do {
      c = (char) console.readByte(true);
      console.writeByte(c);
    } while (c != 'q');

    System.out.println("");
  }

  /**
   * Returns the current process.
   *
   * @return	the current process, or <tt>null</tt> if no process is current.
   */
  public static UserProcess currentProcess() {
    if (!(KThread.currentThread() instanceof UThread)) return null;

    return ((UThread) KThread.currentThread()).process;
  }

  /**
   * The exception handler. This handler is called by the processor whenever
   * a user instruction causes a processor exception.
   *
   * <p>
   * When the exception handler is invoked, interrupts are enabled, and the
   * processor's cause register contains an integer identifying the cause of
   * the exception (see the <tt>exceptionZZZ</tt> constants in the
   * <tt>Processor</tt> class). If the exception involves a bad virtual
   * address (e.g. page fault, TLB miss, read-only, bus error, or address
   * error), the processor's BadVAddr register identifies the virtual address
   * that caused the exception.
   */
  public void exceptionHandler() {
    Lib.assertTrue(KThread.currentThread() instanceof UThread);

    UserProcess process = ((UThread) KThread.currentThread()).process;
    int cause = Machine.processor().readRegister(Processor.regCause);
    process.handleException(cause);
  }

  /**
   * Start running user programs, by creating a process and running a shell
   * program in it. The name of the shell program it must run is returned by
   * <tt>Machine.getShellProgramName()</tt>.
   *
   * @see	nachos.machine.Machine#getShellProgramName
   */
  public void run() {
    super.run();

    UserProcess process = UserProcess.newUserProcess();

    String shellProgram = Machine.getShellProgramName(); // nachos.conf中指定为halt.coff
    Lib.assertTrue(process.execute(shellProgram, new String[] {}));

    KThread.currentThread().finish();
  }

  /**
   * Terminate this kernel. Never returns.
   */
  public void terminate() {
    super.terminate();
  }

  /** Globally accessible reference to the synchronized console. */
  public static SynchConsole console;

  // dummy variables to make javac smarter
  private static Coff dummy1 = null;
}
