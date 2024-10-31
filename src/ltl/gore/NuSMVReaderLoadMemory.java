package ltl.gore;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;

import com.sun.management.ThreadMXBean;

public class NuSMVReaderLoadMemory {

	public long loadMemory(File specFile) {
		CountDownLatch open = new CountDownLatch(1);
		CountDownLatch opened = new CountDownLatch(1);
		Thread thread = new Thread() {
			public void run() {
				try {
					NuSMVReader reader = new NuSMVReader();
					open.await();
					try {
						Spec spec = reader.read(specFile);
						opened.countDown();
						Thread.currentThread().wait();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		};
		try {
			ThreadMXBean threadMxBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
			thread.start();
			long startAlloc = threadMxBean.getThreadAllocatedBytes(thread.getId());
			open.countDown();
			opened.await();
			long endAlloc = threadMxBean.getThreadAllocatedBytes(thread.getId());
	        long additionalMemory = endAlloc - startAlloc;
	        thread.notify();
	        return additionalMemory;
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		return -1l;
	}
	
}