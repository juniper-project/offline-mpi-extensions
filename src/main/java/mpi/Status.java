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

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

public class Status {
	
	private Intracomm.Message message;
	
	public Status(Intracomm.Message message) {
		this.message = message;
	}
	
	public int getCount(Datatype datatype) throws MPIException {
		Object payload = message.payload;
				
		if (payload instanceof ByteBuffer) {
			return ((ByteBuffer)payload).capacity();
		} else {
			return Array.getLength(payload);
		}
	}
}
