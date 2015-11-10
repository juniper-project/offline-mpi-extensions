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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import eu.juniper.platform.Rte;

public class MPI {

	public static final Datatype OBJECT = new Datatype();
	public static final Datatype INT = new Datatype();
	public static final Datatype BYTE = new Datatype();
	
	public static final int ANY_SOURCE = -1;
	public static final int ANY_TAG = -1;
	
	public static Intracomm COMM_WORLD = new Intracomm();
	
	public static void Finalize() {
		
	}

	private static Object runStaticMethod(Class cls, String methodName, Object[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for(Method m : cls.getMethods()) {
			if (m.getName().equals(methodName)) {
				return m.invoke(null, args);
			}
		}
		throw new RuntimeException("Method " + methodName + " does not exist in class " + cls.getName());
	}
	
	@SuppressWarnings("rawtypes")
	public static void RunStubMPIProgram(final Class program) throws Exception {
		RunStubMPIProgram(program, new String[0]);
	}

	@SuppressWarnings("rawtypes")
	public static void RunStubMPIProgram(final Class program, final String[] args) throws Exception {
		ThreadGroup tg = new ThreadGroup("group-"+program.getName());
		
		Thread mainThread = new Thread(tg, new Runnable() {

			@SuppressWarnings("unchecked")
			public void run() {
				Method main = null;
				try {
					main = program.getMethod("main", args.getClass());
					main.invoke(null, (Object)args);
				} catch (NoSuchMethodException nsme) {
					Class executionHelperClass;
					try {
						executionHelperClass = Class.forName("org.modelio.juniper.ExecutionHelper");
						executionHelperClass.getMethod("runJuniperProgram", new Class[] { Class.class }).invoke(null, program);
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(1);
					}					
				} catch (Exception e) {
					System.out.println("exception running: " + main);
					e.printStackTrace();
					System.exit(1);
				}
			}
			
		}, "mainThread-"+tg.getName());
		
		mainThread.start();
		COMM_WORLD.registerProgram(tg, program);		
	}

	@SuppressWarnings("rawtypes")
	public static void RunAnyMain(int rank, final Class program, final String[] args) throws Exception {
		ThreadGroup tg = new ThreadGroup("group-"+rank);
		
		Thread mainThread = new Thread(tg, new Runnable() {

			@SuppressWarnings("unchecked")
			public void run() {
				Method main = null;
				try {
					main = program.getMethod("main", args.getClass());
					main.invoke(null, (Object)args);
				} catch (Exception e) {
					System.out.println("exception running: " + main);
					e.printStackTrace();
					System.exit(1);
				}
			}
			
		}, "mainThread-"+tg.getName());
		
		mainThread.start();
		COMM_WORLD.registerProgram(tg, rank);		
	}

	public static void RunPlatformProgram(int rank, final String[] args) throws Exception {
		ThreadGroup tg = new ThreadGroup("group-"+rank);
		
		Thread mainThread = new Thread(tg, new Runnable() {

			public void run() {
				Rte.main(args);
			}
			
		}, "mainThread-"+tg.getName());
		
		mainThread.start();
		COMM_WORLD.registerProgram(tg, rank);		
	}
	
	public static String[] Init(String[] args) {
		return null;
	}
}
