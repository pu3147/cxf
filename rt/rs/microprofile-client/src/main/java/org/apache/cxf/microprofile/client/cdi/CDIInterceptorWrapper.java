/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.microprofile.client.cdi;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;


public interface CDIInterceptorWrapper {
    Logger LOG = LogUtils.getL7dLogger(CDIInterceptorWrapper.class);

    class BasicCDIInterceptorWrapper implements CDIInterceptorWrapper {
        BasicCDIInterceptorWrapper() {
        }

        @Override
        public Object invoke(Object restClient, Method m, Object[] params, Callable<Object> callable) 
            throws Exception {
            return callable.call();
        }
    }

    static CDIInterceptorWrapper createWrapper(Class<?> restClient) {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<CDIInterceptorWrapper>) () -> {
                Class<?> cdiClass = Class.forName("javax.enterprise.inject.spi.CDI", false,
                                                  restClient.getClassLoader());
                Method currentMethod = cdiClass.getMethod("current");
                Object cdiCurrent = currentMethod.invoke(null);

                Method getBeanMgrMethod = cdiClass.getMethod("getBeanManager");

                return new CDIInterceptorWrapperImpl(restClient, getBeanMgrMethod.invoke(cdiCurrent));
            });
        } catch (PrivilegedActionException pae) {
            // expected for environments where CDI is not supported
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Unable to load CDI SPI classes, assuming no CDI is available", pae);
            }
            return new BasicCDIInterceptorWrapper(); 
        }

    }

    Object invoke(Object restClient, Method m, Object[] params, Callable<Object> callable) throws Exception;
}
