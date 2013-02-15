//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Jul 21, 2005
 */
package org.globus.cog.abstraction.coaster.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

import org.apache.log4j.Logger;
import org.globus.cog.abstraction.coaster.service.job.manager.TaskNotifier;
import org.globus.cog.abstraction.impl.common.CleanUpSetImpl;
import org.globus.cog.abstraction.impl.common.IdentityImpl;
import org.globus.cog.abstraction.impl.common.StagingSetEntryImpl;
import org.globus.cog.abstraction.impl.common.StagingSetImpl;
import org.globus.cog.abstraction.impl.common.task.ExecutionServiceImpl;
import org.globus.cog.abstraction.impl.common.task.IllegalSpecException;
import org.globus.cog.abstraction.impl.common.task.JobSpecificationImpl;
import org.globus.cog.abstraction.impl.common.task.ServiceContactImpl;
import org.globus.cog.abstraction.impl.common.task.TaskImpl;
import org.globus.cog.abstraction.interfaces.CleanUpSet;
import org.globus.cog.abstraction.interfaces.ExecutionService;
import org.globus.cog.abstraction.interfaces.JobSpecification;
import org.globus.cog.abstraction.interfaces.StagingSet;
import org.globus.cog.abstraction.interfaces.StagingSetEntry;
import org.globus.cog.abstraction.interfaces.StagingSetEntry.Mode;
import org.globus.cog.abstraction.interfaces.Task;
import org.globus.cog.karajan.workflow.service.ProtocolException;
import org.globus.cog.karajan.workflow.service.channels.ChannelContext;
import org.globus.cog.karajan.workflow.service.channels.ChannelManager;
import org.globus.cog.karajan.workflow.service.handlers.RequestHandler;

public class SubmitJobHandler extends RequestHandler {
    
    Logger logger = Logger.getLogger(SubmitJobHandler.class);
    
    public static final boolean COMPRESSION = false;
    
    private CoasterService service;

    public void requestComplete() throws ProtocolException {
        Task task;
        try {
            ChannelContext channelContext = getChannel().getChannelContext();
            service = (CoasterService) channelContext.getService();
            if (COMPRESSION) {
                task = read(new InflaterInputStream(new ByteArrayInputStream(getInData(0))));
            }
            else {
                task = read(new ByteArrayInputStream(getInData(0)));
            }
            new TaskNotifier(task, channelContext);
            service.getJobQueue().setClientChannelContext(channelContext);
            service.getJobQueue().enqueue(task);
            // make sure we'll have something to send notifications to
            ChannelManager.getManager().reserveLongTerm(getChannel());
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new ProtocolException("Could not deserialize job description", e);
        }
        sendReply(task.getIdentity().toString());
    }

    private Task read(InputStream is) throws IOException, ProtocolException, IllegalSpecException {
        Helper helper = new Helper(is);

        Task task = new TaskImpl();
        task.setType(Task.JOB_SUBMISSION);
        JobSpecification spec = new JobSpecificationImpl();
        task.setSpecification(spec);

        String clientId = helper.read("identity");
        if (clientId == null) {
            throw new IllegalSpecException("Missing job identity");
        }
        task.setIdentity(new IdentityImpl(clientId + "-" + new IdentityImpl().getValue()));
        spec.setExecutable(helper.read("executable").intern());
        spec.setDirectory(helper.read("directory"));
        spec.setBatchJob(helper.readBool("batch"));
        spec.setStdInput(helper.read("stdin"));
        spec.setStdOutput(helper.read("stdout"));
        spec.setStdError(helper.read("stderr"));
        String s;
        while ((s = helper.read("arg")) != null) {
            spec.addArgument(s);
        }

        while ((s = helper.read("env")) != null) {
            spec.addEnvironmentVariable(getKey(s), getValue(s));
        }

        while ((s = helper.read("attr")) != null) {
            spec.setAttribute(getKey(s), getValue(s));
        }

        StagingSet ss = null;
        while ((s = helper.read("stagein")) != null) {
            if (ss == null) {
                ss = new StagingSetImpl();
            }
            List<String> split = splitNL(s);
            ss.add(new StagingSetEntryImpl(
            		getSource(split), getDestination(split), getMode(split)));
        }
        if (ss != null) {
            spec.setStageIn(ss);
        }

        ss = null;
        while ((s = helper.read("stageout")) != null) {
            if (ss == null) {
                ss = new StagingSetImpl();
            }
            List<String> split = splitNL(s);
            ss.add(new StagingSetEntryImpl(
            		getSource(split), getDestination(split), getMode(split)));
        }
        if (ss != null) {
            spec.setStageOut(ss);
        }
        
        CleanUpSet cs = null;
        
        while ((s = helper.read("cleanup")) != null) {
            if (cs == null) {
                cs = new CleanUpSetImpl();
            }
            cs.add(s);
        }
        
        if (cs != null) {
            spec.setCleanUpSet(cs);
        }

        ExecutionService service = new ExecutionServiceImpl();

        setServiceParams(service, intern(helper.read("contact")), intern(helper.read("provider")), intern(helper.read("jm")));
        task.setService(0, service);
        
        return task;
    }
    
    private String intern(String str) {
        if (str == null) {
            return null;
        }
        else {
            return str.intern();
        }
    }

    private static final Pattern COLON = Pattern.compile(":");

    protected void setServiceParams(ExecutionService s, String contact, 
                                    String provider, String jm) {
        if (jm == null) {
            jm = "fork";
        }
        if (contact == null) {
            contact = "localhost";
        }

        String[] els = COLON.split(jm);
        if (els.length == 2 && "fork".equals(els[1])) {
            s.setProvider("local");
        }
        else if (jm.equalsIgnoreCase("fork")) {
            s.setProvider("local");
        }
        else {
            s.setProvider("coaster");
            s.setJobManager(jm);
        }
        s.setServiceContact(new ServiceContactImpl(contact));
    }
    
    private List<String> splitNL(String s) {
    	List<String> l = new ArrayList<String>(3);
    	int last = -1;
    	int i = s.indexOf('\n');
    	while (i != -1) {
    	    l.add(s.substring(last + 1, i));
    	    last = i;
    	    i = s.indexOf('\n', i + 1);
    	}
    	l.add(s.substring(last + 1));
    	return l;
    }

    private String getSource(List<String> s) {
        return makeAbsolute(s.get(0));
    }

    private String getDestination(List<String> s) {
        return makeAbsolute(s.get(1));
    }
    
    private EnumSet<Mode> getMode(List<String> s) {
        if (s.size() == 3) {
            return StagingSetEntry.Mode.fromId(Integer.parseInt(s.get(2)));
        }
        else {
            return EnumSet.of(Mode.IF_PRESENT);
        }
    }

    private String makeAbsolute(String path) {
        String prefix = "";
        String spath;
        if (path.startsWith("pinned:")) {
            prefix = "pinned:";
            spath = path.substring(7);
        }
        else {
            spath = path;
        }
        if (spath.startsWith("proxy://localhost")) {
            return prefix + "proxy://" + 
                   getChannel().getChannelContext().getChannelID() + 
                   spath.substring("proxy://localhost".length());
        }
        else if (spath.startsWith("file://localhost")) {
            return path;
        }
        else {
            return spath;
        }
    }

    private String getKey(String s) throws ProtocolException {
        int i = s.indexOf('=');
        if (i == -1) {
            throw new ProtocolException("Invalid value: " + s);
        }
        return s.substring(0, i);
    }

    private String getValue(String s) {
        int i = s.indexOf('=');
        return s.substring(i + 1);
    }

    private class Helper {
        private InputStream is;
        private String key, value;

        public Helper(InputStream is) {
            this.is = is;
        }

        public boolean readBool(String key) throws IOException, ProtocolException {
            return Boolean.valueOf(read(key)).booleanValue();
        }

        public String read(String key) throws IOException, ProtocolException {
            if (this.key == null) {
                scan();
            }
            if (key.equals(this.key)) {
                this.key = null;
                return this.value;
            }
            else {
                return null;
            }
        }

        private void scan() throws IOException, ProtocolException {
            key = null;
            value = null;
            StringBuilder sb = new StringBuilder();
            int c = is.read();
            boolean nl = false;
            while (value == null) {
                switch (c) {
                    case '=': {
                        if (key == null) {
                            key = sb.toString();
                            sb = new StringBuilder();
                        }
                        else {
                            sb.append((char) c);
                        }
                        break;
                    }
                    case '\\': {
                        c = is.read();
                        if (c == 'n') {
                            sb.append('\n');
                        }
                        else if (c == '\\') {
                            sb.append('\\');
                        }
                        break;
                    }
                    case -1:
                    case '\n': {
                        if (key == null) {
                            throw new ProtocolException("Invalid line: " + sb.toString());
                        }
                        else {
                            value = sb.toString();
                        }
                        return;
                    }
                    default:
                        sb.append((char) c);
                }
                c = is.read();
            }
        }
    }
}
