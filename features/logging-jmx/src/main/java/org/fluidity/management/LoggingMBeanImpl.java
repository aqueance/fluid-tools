/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.management;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.fluidity.foundation.Methods;
import org.fluidity.foundation.spi.LogLevels;

/**
 * @author Tibor Varga
 */
final class LoggingMBeanImpl extends StandardMBean implements LoggingMBean {

    protected LoggingMBeanImpl() throws NotCompliantMBeanException {
        super(LoggingMBean.class);
    }

    public void logLevelsUpdated() {
        LogLevels.updated();
    }

    @Override
    protected String getDescription(final MBeanInfo info) {
        return "Management interface for logging related items.";
    }

    private final String logLevelUpdateOperation = Methods.get(LoggingMBean.class, new Methods.Invoker<LoggingMBean>() {
        public void invoke(final LoggingMBean capture) throws Throwable {
            capture.logLevelsUpdated();
        }
    }).getName();

    @Override
    protected String getDescription(final MBeanOperationInfo info) {
        assert logLevelUpdateOperation.equals(info.getName()) : info.getName();
        return "Triggers log level queries in loggers through which log messages get emitted after the trigger.";
    }

    @Override
    protected int getImpact(final MBeanOperationInfo info) {
        assert logLevelUpdateOperation.equals(info.getName()) : info.getName();
        return MBeanOperationInfo.ACTION;
    }
}
