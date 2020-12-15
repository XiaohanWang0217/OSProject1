package nachos.userprog;

import java.io.EOFException;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.ArrayList;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {

    /**
     * Allocate a new process.
     */
    // my code begin 第一题
    protected OpenFile[] file_descriptors;
    private static final int MAX_FILE_NUM = 16;
    // my code end

    // my code begin 第一题
    public static int nextProcessID = 0; // 用一个静态变量维护全局的进程ID，每创建一个新的进程加1
    private int processID; // 当前进程的ID
    private static Lock processIDLock = new Lock();

    // my code end

    public UserProcess() {
        /*
         * // 第二题按需分配物理内存，放到load函数中生成pageTable int numPhysPages =
         * Machine.processor().getNumPhysPages(); pageTable = new
         * TranslationEntry[numPhysPages]; for (int i = 0; i < numPhysPages; i++)
         * pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
         */
        // my code begin 第二题
        pageTable = null;
        // my code end

        // my code begin 第一题
        processIDLock.acquire();
        processID = nextProcessID;
        nextProcessID++;
        processIDLock.release();
        // my code end

        // my code begin 第一题
        file_descriptors = new OpenFile[MAX_FILE_NUM];
        for (int i = 0; i < MAX_FILE_NUM; i++) {
            file_descriptors[i] = null;
        }
        file_descriptors[0] = UserKernel.console.openForReading(); // 标准输入
        file_descriptors[1] = UserKernel.console.openForWriting(); // 标准输出
        // my code end
    }

    /**
     * Allocate and return a new process of the correct class. The class name is
     * specified by the <tt>nachos.conf</tt> key <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to load
     * the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;

        new UThread(this).setName(name).fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch. Called by
     * <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read at
     * most <tt>maxLength + 1</tt> bytes from the specified address, search for the
     * null terminator, and convert it to a <tt>java.lang.String</tt>, without
     * including the null terminator. If no null terminator is found, returns
     * <tt>null</tt>.
     *
     * @param vaddr     the starting virtual address of the null-terminated string.
     * @param maxLength the maximum number of characters in the string, not
     *                  including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data  the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array. This
     * method handles address translation details. This method must <i>not</i>
     * destroy the current process if an error occurs, but instead should return the
     * number of bytes successfully copied (or zero if no data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to read.
     * @param data   the array where the data will be stored.
     * @param offset the first byte to write in the array.
     * @param length the number of bytes to transfer from virtual memory to the
     *               array.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses

        // if (vaddr < 0 || vaddr >= memory.length) return 0;
        // int amount = Math.min(length, memory.length - vaddr);
        // System.arraycopy(memory, vaddr, data, offset, amount);
        // return amount;

        // my code begin 第二题
        // 由Processor构造函数可知：mainMemory = new byte[pageSize * numPhysPages];
        // 虚拟页地址转物理页地址
        int vpn = Machine.processor().pageFromAddress(vaddr); // 虚拟页号
        int page_offset = Machine.processor().offsetFromAddress(vaddr); // 页内偏移
        if (null == pageTable || vpn < 0 || vpn >= pageTable.length || !pageTable[vpn].valid) {
            return 0;
        }

        // 虚拟页在pageTable中是连续的，找到对应的物理页，逐段取数据
        int numPhysPages = Machine.processor().getNumPhysPages();
        int len_read = 0; // 已读入的字节数
        int ppn, amount, paddr; // 物理页号、准备从当前页读入的字节数、实际物理地址
        while (vpn < pageTable.length && len_read < length) {
            ppn = pageTable[vpn].ppn;
            if (ppn < 0 || ppn >= numPhysPages)
                break;
            amount = Math.min(length - len_read, pageSize - page_offset); // 还需要读的字节数和本页剩余的字节数间取个小的，为本次要读的字节数
            paddr = Machine.processor().makeAddress(ppn, page_offset);
            System.arraycopy(memory, paddr, data, offset + len_read, amount);
            pageTable[vpn].used = true;
            len_read += amount; // 已读入的字节量增加
            page_offset = 0; // 第二页开始都从页首开始读
            vpn++; // 虚拟地址是连续的
            if (!pageTable[vpn].valid)
                break;
        }
        return len_read;
        // my code end
    }

    /**
     * Transfer all data from the specified array to this process's virtual memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data  the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory. This
     * method handles address translation details. This method must <i>not</i>
     * destroy the current process if an error occurs, but instead should return the
     * number of bytes successfully copied (or zero if no data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to write.
     * @param data   the array containing the data to transfer.
     * @param offset the first byte to transfer from the array.
     * @param length the number of bytes to transfer from the array to virtual
     *               memory.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        // if (vaddr < 0 || vaddr >= memory.length) return 0;

        // int amount = Math.min(length, memory.length - vaddr);
        // System.arraycopy(data, offset, memory, vaddr, amount);

        // return amount;

        // my code begin
        // 虚拟页地址转物理页地址
        int vpn = Machine.processor().pageFromAddress(vaddr); // 虚拟页号
        int page_offset = Machine.processor().offsetFromAddress(vaddr); // 页内偏移
        if (null == pageTable || vpn < 0 || vpn >= pageTable.length || !pageTable[vpn].valid
                || pageTable[vpn].readOnly) {
            return 0;
        }

        // 虚拟页在pageTable中是连续的，找到对应的物理页，逐段写数据
        int numPhysPages = Machine.processor().getNumPhysPages();
        int len_written = 0; // 已写入的字节数
        int ppn, amount, paddr; // 物理页号、准备写入当前页的字节数、实际物理地址
        while (vpn < pageTable.length && len_written < length) {
            if (pageTable[vpn].readOnly)
                break;
            ppn = pageTable[vpn].ppn;
            if (ppn < 0 || ppn >= numPhysPages)
                break;
            amount = Math.min(length - len_written, pageSize - page_offset); // 还需要写的字节数和本页剩余的字节数间取个小的，为本次要写的字节数
            paddr = Machine.processor().makeAddress(ppn, page_offset);
            System.arraycopy(data, offset + len_written, memory, paddr, amount);
            pageTable[vpn].used = true;
            pageTable[vpn].dirty = true;
            len_written += amount; // 已写入的字节量增加
            page_offset = 0; // 第二页开始都从页首开始写
            vpn++; // 虚拟地址是连续的
            if (!pageTable[vpn].valid)
                break;
        }
        return len_written;
        // my code end
    }

    /**
     * Load the executable with the specified name into this process, and prepare to
     * pass it the specified arguments. Opens the executable, reads its header
     * information, and copies sections and arguments into this process's virtual
     * memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        } catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i = 0; i < argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
            stringOffset += 1;
        }

        UserKernel.num_process++;

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into memory.
     * If this returns successfully, the process will definitely be run (this is the
     * last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        /*
         * if (numPages > Machine.processor().getNumPhysPages()) { coff.close();
         * Lib.debug(dbgProcess, "\tinsufficient physical memory"); return false; }
         */

        // my code begin 第二题
        // 按需要的页数申请物理内存
        LinkedList<Integer> pageList = UserKernel.allocatePages(numPages);
        if (null == pageList)
            return false; // 物理内存不足
        pageTable = new TranslationEntry[numPages];
        for (int i = 0; i < numPages; i++) {
            pageTable[i] = new TranslationEntry(i, pageList.removeFirst().intValue(), true, false, false, false);
        }
        // my code end 第二题

        // load sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess,
                    "\tinitializing " + section.getName() + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;

                // for now, just assume virtual addresses=physical addresses
                // section.loadPage(i, vpn);
                // my code begin 第二题
                // section.loadPage函数的第二个参数需要传入物理页面序号
                int ppn = -1;
                for (int j = 0; j < pageTable.length; j++) {
                    if (pageTable[j].vpn == vpn) {
                        ppn = pageTable[j].ppn;
                        pageTable[j].readOnly = true;
                        break;
                    }
                }

                if (-1 == ppn) {
                    return false;
                }

                section.loadPage(i, ppn);
                // my code end

            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    }

    /**
     * Initialize the processor's registers in preparation for running the program
     * loaded into this process. Set the PC register to point at the start function,
     * set the stack pointer register to point at the top of the stack, set the A0
     * and A1 registers to argc and argv, respectively, and initialize all other
     * registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    // my code begin 第二题
    private void releasePages() {
        if (null != pageTable) {
            LinkedList<Integer> pagesToRelease = new LinkedList<Integer>();
            for (int j = 0; j < pageTable.length; j++) {
                pagesToRelease.add(new Integer(pageTable[j].ppn));
            }

            if (pagesToRelease.size() > 0) {
                UserKernel.releasePages(pagesToRelease);
            }
        }
        pageTable = null;
    }

    // my code end

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {
        // my code begin
        // 应该使halt()系统调用只能由“根”进程(即系统中的第一个进程)调用。
        // 如果另一个进程尝试调用halt()，则应该忽略系统调用并立即返回。
        if (processID > 0) {
            return -1;
        }
        // my code end
        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    // my code begin
    // The maximum length of for strings passed as arguments to system calls is 256
    // bytes
    private static final int MAX_STR_LEN = 256;

    // 尝试打开指定名称的磁盘文件，如果它不存在，则创建它，并返回可用于访问该文件的文件描述符。
    // 注意，creat()只能用于在磁盘上创建文件;creat()永远不会返回引用流的文件描述符。
    // 返回新的文件描述符，如果出现错误则返回-1。
    private int handleCreat(int addr_name) {
        String file_name = readVirtualMemoryString(addr_name, MAX_STR_LEN);
        if (null == file_name) {
            // 文件名读入失败，包括addr_name<0及addr_name>= memory.length的情况
            // readVirtualMemory方法中有说明：for now, just assume that virtual addresses equal
            // physical addresses)
            return -1;
        }
        // 最多同时打开16个文件，寻找空的文件描述符
        int i = 0;
        while (i < MAX_FILE_NUM && null != file_descriptors[i])
            i++;
        if (i == MAX_FILE_NUM)
            return -1;
        // 打开文件，若不存在，则创建
        OpenFile file = UserKernel.fileSystem.open(file_name, true);
        if (null == file) {
            return -1;
        } else {
            file_descriptors[i] = file;
            return i;
        }
    }

    // 尝试打开指定名称的文件并返回一个文件描述符。
    // 注意，open()只能用于打开磁盘上的文件;open()永远不会返回引用流的文件描述符。
    // 返回新的文件描述符，如果出现错误则返回-1。
    private int handleOpen(int addr_name) {
        String file_name = readVirtualMemoryString(addr_name, MAX_STR_LEN);
        if (null == file_name) {
            return -1;
        }
        // 最多同时打开16个文件，寻找空的文件描述符
        int i = 0;
        while (i < MAX_FILE_NUM && null != file_descriptors[i])
            i++;
        if (i == MAX_FILE_NUM)
            return -1;
        // 打开文件
        OpenFile file = UserKernel.fileSystem.open(file_name, false);
        if (null == file) {
            return -1;
        } else {
            file_descriptors[i] = file;
            return i;
        }
    }

    // 尝试从由fileDescriptor指定的文件或流读count个字节到buffer中。
    // 若成功，则返回读入的字节数。如果file descriptor引用一个磁盘文件，the file position is advanced by this
    // number.
    // 如果这个数字小于请求的字节数，它不一定是一个错误。
    // 当出现错误时，返回-1，并且新文件位置未定义。
    // 这可能发生如果fileDescriptor无效,如果缓冲区是只读或无效的一部分,或者远程主机的网络流已经终止,没有更多的数据是可用的。
    private int handleRead(int fd, int addr_buffer, int size) {
        if (fd < 0 || fd >= MAX_FILE_NUM) {
            return -1;
        }
        if (null == file_descriptors[fd]) {
            return -1;
        }
        if (size < 0) {
            return -1;
        }
        if (0 == size) {
            return 0;
        }

        OpenFile file = file_descriptors[fd];
        byte[] buffer = new byte[size]; // size值上限要不要考虑呢？
        int success_len = file.read(buffer, 0, size);
        if (-1 == success_len) {
            return -1;
        }
        return writeVirtualMemory(addr_buffer, buffer, 0, success_len);
    }

    // 尝试将count个字节从buffer写到由fileDescriptor引用的文件或流。
    // write函数可以在字节实际写入文件或流之前返回。
    // 成功时，返回写的字节数（0表示没有写什么），the file position is advanced by this number。
    // 如果实际写入的字节数小于要求写入的，这是一个错误。
    // 出错时，返回-1， and the new file position is undefined.
    // 这可能发生,如果fileDescriptor无效,如果缓冲区的一部分是无效的,或者一个网络流由远程主机已经终止。
    private int handleWrite(int fd, int addr_buffer, int size) {
        if (fd < 0 || fd >= MAX_FILE_NUM) {
            return -1;
        }
        if (null == file_descriptors[fd]) {
            return -1;
        }
        if (size < 0) {
            return -1;
        }
        if (0 == size) {
            return 0;
        }
        OpenFile file = file_descriptors[fd];
        byte[] buffer = new byte[size]; // size的上限要不要考虑呢？
        int success_len = readVirtualMemory(addr_buffer, buffer, 0, size);
        return file.write(buffer, 0, success_len);
    }

    // 关闭文件描述符，之后它不再引用任何文件或流，可能被重用。
    // 如果文件描述符引用一个文件,所有用write()函数写入的数据在close()返回前将被flush到磁盘。
    // 如果文件描述符引用一个流，所有用write()函数写入的数据最终会被flush（除非流被远端终止），但不必在close()返回前flush。
    // 与文件描述符关联的资源被释放。
    // 如果磁盘文件描述符是一个已使用unlink移除的磁盘文件的最后引用，则文件被删除(这个细节是由文件系统实现)。
    // 操作成功返回0，出错返回-1。
    private int handleClose(int fd) {
        if (fd < 0 || fd >= MAX_FILE_NUM) {
            return -1;
        }
        if (null == file_descriptors[fd]) {
            return -1;
        }
        OpenFile file = file_descriptors[fd];
        file_descriptors[fd] = null;
        file.close();
        return 0;
    }

    // 从文件系统中删除文件。如果没有进程打开此文件,此文件立即被删除，它使用的空间放出，允许重用。
    // 如果当前仍有进程打开该文件,文件会仍然存在,直到最后一个引用它的文件描述符关闭,
    // 但create()和open()将无法返回该文件的文件描述符,直到它被删除。
    // 如果成功返回0，如果发生错误返回-1。
    private int handleUnlink(int addr_name) {
        String file_name = readVirtualMemoryString(addr_name, MAX_STR_LEN);
        if (null == file_name) {
            return -1;
        }

        OpenFile tmpFile, fileToRemove = null;
        int i = 0;
        while (i < MAX_FILE_NUM) {
            tmpFile = file_descriptors[i];
            if (null == tmpFile || file_name.compareTo(tmpFile.getName()) != 0) {
                i++;
            } else {
                break; // 当前进程仍打开该文件
            }
        }

        if (MAX_FILE_NUM == i) // 不要实现任何类型的文件锁定;这是文件系统的职责。 // 没有进程打开此文件
                               // 如果threadedkernel.filesysystem.open()返回一个非空的OpenFile，那么用户进程就可以访问给定的文件;
                               // 否则，您应该发出错误信号。
                               // 同样，如果多个进程试图同时访问同一个文件，您不需要担心会发生什么细节;存根文件系统为您处理这些细节。
        {
            if (UserKernel.fileSystem.remove(file_name)) {
                return 0;
            }
        }
        return -1;
    }

    // my code end

    private int handleExec(int addr_name, int argc,int argv) {
        String file_name = readVirtualMemoryString(addr_name, MAX_STR_LEN);
        if (null == file_name)
            return -1;

        if (argc == -1 || argv == -1)
            return -1;

        String args[] = new String[argc];
        byte addr[] = new byte[4*argc];

        if (readVirtualMemory(argv, addr) != 4*argc)
            return -1;

        for (int i = 0; i < argc; i++) {
            args[i] = readVirtualMemoryString(Lib.bytesToInt(addr, i * 4), 256);
            if (args[i] == null)
                return -1;
        }
        UserProcess proc = newUserProcess();
        proc.parent = this;

        boolean status = proc.execute(file_name, args);
        if (status) {
            children.add(proc);
            return proc.processID;
        } else return -1;
    }

    private int handleJoin(int fd, int addr) {
        UserProcess proc = null;
        for (int i = 0; i < children.size(); i++)
            if (children.get(i).processID == fd) {
                proc = children.get(i);
                break;
            }
        if (proc == null)
            return -1;

        lock_join.acquire();
        cond_join.sleep();
        lock_join.release();

        byte[] child_status = new byte[4];
        Lib.bytesFromInt(child_status, 0, proc.status);
        int len_written = writeVirtualMemory(addr, child_status);
        if (proc.normal_exit && len_written == 4)
            return 1;
        else
            return 0;
    }

    private int handleExit(int status) {
        coff.close();
        unloadSections();

        for (int i = 0; i < MAX_FILE_NUM; i++)
            handleClose(i);

        this.status = status;

        if (parent != null) {
            parent.lock_join.acquire();
            parent.cond_join.wake();
            parent.lock_join.release();
            parent.children.remove(this);
        }
        
        UserKernel.num_process--;
        if (UserKernel.num_process == 0)
            UserKernel.kernel.terminate();

        this.releasePages();
        KThread.finish();

        return status;
    }

    private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2, syscallJoin = 3, syscallCreate = 4,
            syscallOpen = 5, syscallRead = 6, syscallWrite = 7, syscallClose = 8, syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr>
     * <td>syscall#</td>
     * <td>syscall prototype</td>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td><tt>void halt();</tt></td>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td><tt>void exit(int status);</tt></td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td><tt>int  join(int pid, int *status);</tt></td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td><tt>int  creat(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td><tt>int  open(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td>
     * </tr>
     * <tr>
     * <td>7</td>
     * <td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td>
     * </tr>
     * <tr>
     * <td>8</td>
     * <td><tt>int  close(int fd);</tt></td>
     * </tr>
     * <tr>
     * <td>9</td>
     * <td><tt>int  unlink(char *name);</tt></td>
     * </tr>
     * </table>
     *
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallHalt:
                System.out.println("***Handle Halt***");
                return handleHalt();
            // my code begin 第一题
            case syscallCreate:
                System.out.println("***Handle Create***");
                return handleCreat(a0);
            case syscallOpen:
                System.out.println("***Handle Open***");
                return handleOpen(a0);
            case syscallRead:
                System.out.println("***Handle Read***");
                return handleRead(a0, a1, a2);
            case syscallWrite:
                System.out.println("***Handle Write***");
                return handleWrite(a0, a1, a2);
            case syscallClose:
                System.out.println("***Handle Close***");
                return handleClose(a0);
            case syscallUnlink:
                System.out.println("***Handle Unlink***");
                return handleUnlink(a0);
            // my code end

            // my code begin 第二题
            case syscallExit:
                System.out.println("***Handle Exit***");
                return handleExit(a0);
                //break;
            // my code end

            // my code begin 第三题
            case syscallExec:
                System.out.println("***Handle Exec***");
                return handleExec(a0,a1,a2);
            case syscallJoin:
                System.out.println("***Handle join***");
                return handleJoin(a0,a1);
            // my code end
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>.
     * The <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        System.out.println("***Handle exception***");
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0), processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2), processor.readRegister(Processor.regA3));
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;
            default:
                Lib.debug(dbgProcess, "Unexpected exception: " + Processor.exceptionNames[cause]);
                normal_exit = false;
                System.out.println("***go Handle exception***");
                handleExit(0);
                Lib.assertNotReached("Unexpected exception");
                // my code begin 第二题
                //releasePages(); // 非正常退出时也要释放进程的所有内存
                // my code end
        }
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    private UserProcess parent = null;
    private ArrayList<UserProcess> children = new ArrayList<UserProcess>();
    private Lock lock_join = new Lock();
    private Condition2 cond_join = new Condition2(lock_join);
    private int status;
    private boolean normal_exit = true;
}
