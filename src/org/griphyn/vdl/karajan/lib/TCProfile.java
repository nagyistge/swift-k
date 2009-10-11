/*
 * Created on Dec 26, 2006
 */
package org.griphyn.vdl.karajan.lib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.globus.cog.abstraction.impl.common.execution.WallTime;
import org.globus.cog.karajan.arguments.Arg;
import org.globus.cog.karajan.arguments.ArgUtil;
import org.globus.cog.karajan.arguments.NamedArguments;
import org.globus.cog.karajan.stack.VariableStack;
import org.globus.cog.karajan.util.BoundContact;
import org.globus.cog.karajan.util.TypeUtil;
import org.globus.cog.karajan.workflow.ExecutionException;
import org.globus.cog.karajan.workflow.nodes.grid.GridExec;
import org.globus.swift.catalog.TransformationCatalogEntry;
import org.globus.swift.catalog.types.Os;
import org.globus.swift.catalog.util.Profile;
import org.griphyn.vdl.karajan.TCCache;
import org.griphyn.vdl.util.FQN;

public class TCProfile extends VDLFunction {
    public static final Logger logger = Logger.getLogger(TCProfile.class);
    
	public static final Arg PA_TR = new Arg.Positional("tr");
	public static final Arg PA_HOST = new Arg.Positional("host");
	
	static {
		setArguments(TCProfile.class, new Arg[] { PA_TR, PA_HOST });
	}

	private static Map PROFILE_T;

	static {
		PROFILE_T = new HashMap();
		PROFILE_T.put("count", GridExec.A_COUNT);
		PROFILE_T.put("jobtype", GridExec.A_JOBTYPE);
		PROFILE_T.put("maxcputime", GridExec.A_MAXCPUTIME);
		PROFILE_T.put("maxmemory", GridExec.A_MAXMEMORY);
		PROFILE_T.put("maxtime", GridExec.A_MAXTIME);
		PROFILE_T.put("maxwalltime", GridExec.A_MAXWALLTIME);
		PROFILE_T.put("minmemory", GridExec.A_MINMEMORY);
		PROFILE_T.put("project", GridExec.A_PROJECT);
		PROFILE_T.put("queue", GridExec.A_QUEUE);
	}

	public Object function(VariableStack stack) throws ExecutionException {
		TCCache tc = getTC(stack);
		String tr = TypeUtil.toString(PA_TR.getValue(stack));
		BoundContact bc = (BoundContact) PA_HOST.getValue(stack);
		
		NamedArguments named = ArgUtil.getNamedReturn(stack);
		Map attrs = null;
		
		attrs = attributesFromHost(bc, attrs, named);

		TransformationCatalogEntry tce = getTCE(tc, new FQN(tr), bc);
		
		Map env = new HashMap();
		if (tce != null) {
			addEnvironment(env, tce);
			addEnvironment(env, bc);
			if (env.size() != 0) {
				named.add(GridExec.A_ENVIRONMENT, env);
			}

			attrs = attributesFromTC(tce, attrs, named);
		}
		checkWalltime(tr, named);
		addAttributes(named, attrs);
		return null;
	}
	
    private void checkWalltime(String tr, NamedArguments attrs) {
	    Object walltime = null;
	    if (attrs != null) {
	        if (attrs.hasArgument("maxwalltime")) {
	            walltime = attrs.getArgument("maxwalltime");
	        }
	    }
        if (walltime == null) {
            return;
        }
        try {
        	//validate walltime
            WallTime.timeToSeconds(walltime.toString());
        }
        catch (IllegalArgumentException e) {
            warn(tr, "Warning: invalid walltime specification for \"" + tr
                    + "\" (" + walltime + ").");
        }
	}
	
	private static final Set warnedAboutWalltime = new HashSet();
	
	private void warn(String tr, String message) {
        synchronized (warnedAboutWalltime) {
            if (warnedAboutWalltime.add(tr)) {
                System.out.println(message);
            }
        }
    }

	private void addEnvironment(Map m, TransformationCatalogEntry tce) {
		List l = tce.getProfiles(Profile.ENV);
		if (l != null) {
			Iterator i = l.iterator();
			while (i.hasNext()) {
				Profile p = (Profile) i.next();
				m.put(p.getProfileKey(), p.getProfileValue());
			}
		}
	}

	public static final String PROFILE_GLOBUS_PREFIX = (Profile.GLOBUS + "::").toLowerCase();

	private void addEnvironment(Map m, BoundContact bc) {
		Map props = bc.getProperties();
		Iterator i = props.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry e = (Map.Entry) i.next();
			String name = (String) e.getKey();
			FQN fqn = new FQN(name); 
			String value = (String) e.getValue();
			if (Profile.ENV.equalsIgnoreCase(fqn.getNamespace())) {
				m.put(fqn.getName(), value);
			}
		}
	}
	
	private void addAttributes(NamedArguments named, Map attrs) {
	    if (logger.isInfoEnabled()) {
	        logger.info("Attributes: " + attrs);
	    }
	    if (attrs == null || attrs.size() == 0) {
	        return;
	    }
	    named.add(GridExec.A_ATTRIBUTES, attrs);
	}

	private Map attributesFromTC(TransformationCatalogEntry tce, Map attrs, NamedArguments named) {
		List l = tce.getProfiles(Profile.GLOBUS);
		if (l != null) {
			Iterator i = l.iterator();
			while (i.hasNext()) {
				Profile p = (Profile) i.next();
				Arg a = (Arg) PROFILE_T.get(p.getProfileKey());
				if (a == null) {
				    if (attrs == null) {
				        attrs = new HashMap();
				    }
				    attrs.put(p.getProfileKey(), p.getProfileValue());
				}
				else {
				    named.add(a, p.getProfileValue());
				}
			}
		}
		return attrs;
	}
	
	private Map attributesFromHost(BoundContact bc, Map attrs, NamedArguments named) {
		Map props = bc.getProperties();
		if (props != null) {
		    Iterator i = props.entrySet().iterator();
		    while (i.hasNext()) {
		        Map.Entry e = (Map.Entry) i.next();
		        FQN fqn = new FQN((String) e.getKey());
		        if (Profile.GLOBUS.equalsIgnoreCase(fqn.getNamespace())) {
		            Arg a = (Arg) PROFILE_T.get(fqn.getName());
		            if (a == null) {
		                if (attrs == null) {
		                    attrs = new HashMap();
		                }
		                attrs.put(fqn.getName(), e.getValue());
		            }
		            else {
		                named.add(a, e.getValue());
		            }
		        }
		    }
		}
		return attrs;
	}
}
