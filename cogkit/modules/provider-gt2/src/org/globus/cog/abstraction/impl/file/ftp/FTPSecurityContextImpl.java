/*
 * Swift Parallel Scripting Language (http://swift-lang.org)
 * Code from Java CoG Kit Project (see notice below) with modifications.
 *
 * Copyright 2005-2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

package org.globus.cog.abstraction.impl.file.ftp;

import java.net.PasswordAuthentication;
import java.util.Map;

import org.apache.log4j.Logger;
import org.globus.cog.abstraction.impl.common.task.SecurityContextImpl;

public class FTPSecurityContextImpl extends SecurityContextImpl {

    private static Logger logger = Logger
            .getLogger(FTPSecurityContextImpl.class.getName());

    public FTPSecurityContextImpl() {
    }

    public FTPSecurityContextImpl(PasswordAuthentication credentials) {
        setCredentials(credentials);
    }

    public void setAlias(String alias) {
    }

    public String getAlias() {
        return null;
    }

    @Override
    public void setCredentialProperties(Map<String, Object> props) {
        String user = getStringProperty(props, "username");
        String pass = getStringProperty(props, "password");
        setCredentials(new PasswordAuthentication(user, pass.toCharArray()));
    }
}
