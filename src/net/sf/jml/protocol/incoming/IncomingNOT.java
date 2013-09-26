/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.jml.protocol.incoming;

import java.io.Serializable;

import net.sf.jml.MsnProtocol;
import net.sf.jml.protocol.MsnIncomingMessage;

/**
 * A notice from a service affiliated with MSN Messenger
 * <p>
 * Syntax: NOT msgLen\r\n content
 * 
 * @author Lars Hoogweg
 */

public class IncomingNOT extends MsnIncomingMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	public IncomingNOT(MsnProtocol protocol) {
		super(protocol);
	}
	
	@Override
	protected boolean isSupportChunkData() {
        return true;
    }
}
