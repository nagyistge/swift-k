/*
 * Created on Dec 26, 2006
 */
package org.griphyn.vdl.karajan.lib;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.globus.cog.karajan.arguments.Arg;
import org.globus.cog.karajan.arguments.ArgUtil;
import org.globus.cog.karajan.arguments.NamedArguments;
import org.globus.cog.karajan.stack.VariableStack;
import org.globus.cog.karajan.util.BoundContact;
import org.globus.cog.karajan.util.TypeUtil;
import org.globus.cog.karajan.workflow.ExecutionException;
import org.globus.cog.karajan.workflow.nodes.grid.GridExec;
import org.griphyn.cPlanner.classes.Profile;
import org.griphyn.common.catalog.TransformationCatalogEntry;
import org.griphyn.vdl.karajan.TCCache;
import org.griphyn.vdl.util.FQN;

public class TCProfile extends VDLFunction {
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
		
		attrs = attributesFromHost(bc, attrs);

		FQN fqn = new FQN(tr);
		TransformationCatalogEntry tce = getTCE(tc, new FQN(tr), bc);
		
		Map env = new HashMap();
		if (tce != null) {
			addEnvironment(env, tce);
			addEnvironment(env, bc);
			if (env.size() != 0) {
				named.add(GridExec.A_ENVIRONMENT, env);
			}

			attrs = attributesFromTC(tce, attrs);
		}
		addAttributes(named, attrs);
		return null;
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

	public static final String PROFILE_GLOBUS_PREFIX = Profile.GLOBUS + "::";

	private void addEnvironment(Map m, BoundContact bc) {
		Map props = bc.getProperties();
		Iterator i = props.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry e = (Map.Entry) i.next();
			String name = (String) e.getKey();
			String value = (String) e.getValue();
			if (name.startsWith(PROFILE_GLOBUS_PREFIX)) {
				props.put(name.substring(PROFILE_GLOBUS_PREFIX.length()), value);
			}
		}
	}
	
	private void addAttributes(NamedArguments named, Map attrs) {
	    if (attrs == null || attrs.size() == 0) {
	        return;
	    }
	    System.err.println(attrs);
	    Iterator i = attrs.entrySet().iterator();
	    while (i.hasNext()) {
	        Map.Entry e = (Map.Entry) i.next();
	        Arg a = (Arg) PROFILE_T.get(e.getKey());
	        if (a != null) {
	            named.add(a, e.getValue());
	            i.remove();
	        }
	    }
	    if (attrs.size() == 0) {
	        return;
	    }
	    named.add(GridExec.A_ATTRIBUTES, attrs);
	}

	private Map attributesFromTC(TransformationCatalogEntry tce, Map attrs) {
		List l = tce.getProfiles(Profile.GLOBUS);
		if (l != null) {
			Iterator i = l.iterator();
			while (i.hasNext()) {
				Profile p = (Profile) i.next();
				if (attrs == null) {
				    attrs = new HashMap();
				}
				attrs.put(p.getProfileKey(), p.getProfileValue());
			}
		}
		return attrs;
	}
	
	private Map attributesFromHost(BoundContact bc, Map attrs) {
		Map props = bc.getProperties();
		if (props != null) {
		    System.err.println(props);
		    Iterator i = props.entrySet().iterator();
		    while (i.hasNext()) {
		        Map.Entry e = (Map.Entry) i.next();
		        FQN fqn = new FQN((String) e.getKey());
		        if (Profile.GLOBUS.equalsIgnoreCase(fqn.getNamespace())) {
		            if (attrs == null) {
		                attrs = new HashMap();
		            }
		            attrs.put(fqn.getName(), e.getValue());
		        }
		    }
		}
		return attrs;
	}
}
