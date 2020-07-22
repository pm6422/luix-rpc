package org.infinity.rpc.utilities.id;

import org.infinity.rpc.utilities.id.sequence.SnowFlakeSequence;

import java.util.HashSet;
import java.util.Set;

public class SequenceTest {
	
	public static void main(String[] args) {
		Set<Long> set = new HashSet<Long>();
		final SnowFlakeSequence idWorker1 = new SnowFlakeSequence(0);
		final SnowFlakeSequence idWorker2 = new SnowFlakeSequence(1);
		Thread t1 = new Thread(new IdWorkThread(set, idWorker1));
		Thread t2 = new Thread(new IdWorkThread(set, idWorker2));
		t1.setDaemon(true);
		t2.setDaemon(true);
		t1.start();
		t2.start();
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	static class IdWorkThread implements Runnable {
		private Set<Long>         set;
		private SnowFlakeSequence idWorker;

		public IdWorkThread(Set<Long> set, SnowFlakeSequence idWorker) {
			this.set = set;
			this.idWorker = idWorker;
		}

		@Override
		public void run() {
			while (true) {
				long id = idWorker.nextId();
				if (!set.add(id)) {
					System.out.println("duplicate:" + id);
				}
			}
		}
	}
	
}
