/**
 * Copyright 2014 Modeliosoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mpi;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;


public class Intracomm {
	private static HashMap<ThreadGroup, Integer> threadGroupRanks = new HashMap<ThreadGroup, Integer>();
	private static int size = 0;
	
	public static class Message {
		int from;
		int to;
		int tag;
		Object payload;
		Datatype type;

		public Message(int from, int to, int tag, Object payload, Datatype type) {
			super();
			this.from = from;
			this.to = to;
			this.tag = tag;
			this.payload = payload;
			this.type = type;
		}

		@Override
		public String toString() {
			return "Message [from=" + from + ", to=" + to + ", tag=" + tag
					+ ", payload=" + payload + ", type=" + type + "]";
		}
		
	}

	private static LinkedList<Message> messages = new LinkedList<Message>();

	public final void send(Object buf, int count, Datatype type, int dest, int tag)
	        throws MPIException {
		Send(buf, 0, count, type, dest, tag);
	}
	
	private void Send(Object oobjs, int base, int len, Datatype type, int dst, int tag) throws MPIException {
		synchronized (messages) {
			Message msg = new Message(getCurrentRank(), dst, tag, oobjs, type);
			messages.addLast(msg);
		}
	}	

	public final Status recv(Object buf, int count,
            Datatype type, int source, int tag)
            			throws MPIException {
		return Recv(buf, 0, count, type, source, tag);
	}
	
	private Status Recv(Object oobjs, int base, int len, Datatype type, int src, int tag) throws MPIException {
		int currentThread = getCurrentRank();

		while(Iprobe(src, tag) == null) {
			Thread.yield();
		}
		
		synchronized (messages) {
			Message toRemove = null;
			for(Message message : messages) {
				if (message.to == currentThread && (message.from == src || message.from == MPI.ANY_SOURCE) && (message.tag == tag || message.tag == MPI.ANY_TAG)) {
					Object payload = message.payload;
					
					if (payload instanceof ByteBuffer && payload instanceof ByteBuffer) {
						System.arraycopy(((ByteBuffer)payload).array(), 0, ((ByteBuffer)oobjs).array(), 0, len);						
					} else {						
						System.arraycopy(payload, 0, oobjs, 0, len);
					}
							
					toRemove = message;
					break;
				}
			}			
			
			if (toRemove != null) {
				messages.remove(toRemove);
			}
		}				
		
		return status;
	}

	private static Status status;
	
	public final Status iProbe(int source, int tag) throws MPIException {
		return Iprobe(source, tag);
	}
	
	private synchronized Status Iprobe(int src, int tag) {
		int currentThread = getCurrentRank();
				
		synchronized (messages) {
			for(Message message : messages) {
				if (message.to == currentThread && message.from == src && message.tag == tag) {
					return new Status(message);
				}
			}
		}
		return null;
	}

	public final Status probe(int src, int tag) throws MPIException {
		Status status = null;
		
		do {
			status = iProbe(src, tag);
			Thread.yield();
		} while(status == null);
			
		return status;
	}
	
	// this is a copy of the one in the code generation helper...
	private static int getRank(@SuppressWarnings("rawtypes") Class program)
			throws Exception {
		String rankStr = System.getProperty(program.getName()+"_rank");
		if (rankStr != null) {
			return Integer.parseInt(rankStr);
		} else {
			try {				
				Field rankField = program.getDeclaredField("RANK");
				rankField.setAccessible(true);
				int ret = (Integer) rankField.get(null);
				return ret;
			} catch(Exception e) {
				throw new RuntimeException("Can't run without a rank for: "+ program.getName());				
			}
		}
	}
	
	private static int getCurrentRank() {
		synchronized (threadGroupRanks) {
			Integer rank = null;
			while(rank==null) {
				rank = threadGroupRanks.get(Thread.currentThread().getThreadGroup());
				Thread.yield();
			}
			return rank;
		}
	}

	@SuppressWarnings("rawtypes")
	public void registerProgram(ThreadGroup tg, Class program) throws Exception {
		synchronized (threadGroupRanks) {
			threadGroupRanks.put(tg, getRank(program));					
		}
	}
	
	public void registerProgram(ThreadGroup tg, int rank) throws Exception {
		synchronized (threadGroupRanks) {
			threadGroupRanks.put(tg, rank);					
		}
	}
	
	public int getSize() {
		return Math.max(threadGroupRanks.size(), Intracomm.size);
	}
	
	public void setSize(int size) {
		Intracomm.size = size;
	}
	
	public int getRank() {
		return getCurrentRank();
	}
}
