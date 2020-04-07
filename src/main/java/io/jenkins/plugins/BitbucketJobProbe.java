package io.jenkins.plugins;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.google.common.base.Objects;

import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.GitStatus;
import hudson.plugins.mercurial.MercurialSCM;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;

public class BitbucketJobProbe {
    @Deprecated
    public void triggerPullRequestJobs(String user, String url, String scm) {
        triggerPullRequestJobs(user, url, scm, "");
    }
    public void triggerPullRequestJobs(String user, String url, String scm, String payload) {
        if ("git".equals(scm) || "hg".equals(scm)) {
            SecurityContext old = Jenkins.getInstance().getACL().impersonate(ACL.SYSTEM);
            try {
                URIish remote = new URIish(url);
                
                for (Job<?,?> job : Jenkins.getInstance().getAllItems(Job.class)) {
                    LOGGER.log(Level.INFO, "Considering candidate job {0}", job.getName());
                    PullRequestTrigger bTrigger = null;
                    if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
                        ParameterizedJobMixIn.ParameterizedJob pJob = (ParameterizedJobMixIn.ParameterizedJob) job;
                        
                        for (Object trigger : pJob.getTriggers().values()) {
                            if (trigger instanceof PullRequestTrigger) {
                                bTrigger = (PullRequestTrigger) trigger;
                                break;
                            }
                        }
                    }
                    if (bTrigger != null) {
                        if (bTrigger.getDestinationBranches().size() > 0) {
                            LOGGER.log(Level.FINE, "Considering to poke {0}", job.getFullDisplayName());
                            SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job);
                            List<SCM> scmTriggered = new ArrayList<SCM>();
                            
                            for (SCM scmTrigger : item.getSCMs()) {
                                if (match(scmTrigger, remote) && !hasBeenTriggered(scmTriggered, scmTrigger)) {
                                    LOGGER.log(Level.INFO, "Triggering BitBucket job {0}", job.getName());
                                    scmTriggered.add(scmTrigger);
                                    bTrigger.onPost(user, payload);
                                } else LOGGER.log(Level.FINE, "{0} SCM doesn't match remote repo {1}", new Object[]{job.getName(), remote});
                            }
                        }
                        
                    }

                }
            } catch(URISyntaxException e) {
                LOGGER.log(Level.WARNING, "Invalid repository URL {0}", url);
            } finally {
                SecurityContextHolder.setContext(old);
            }
            
            
        } 
    }
    private boolean hasBeenTriggered(List<SCM> scmTriggered, SCM scmTrigger) {
        for (SCM scm : scmTriggered) {
            if (scm.equals(scmTrigger)) {
                return true;
            }
        }
        return false;
    }

    private boolean match(SCM scm, URIish url) {
        if (scm instanceof GitSCM) {
            for (RemoteConfig remoteConfig : ((GitSCM) scm).getRepositories()) {
                for (URIish urIish : remoteConfig.getURIs()) {
                    // needed cause the ssh and https URI differs in Bitbucket Server.
                    if(urIish.getPath().startsWith("/scm")){
                        urIish = urIish.setPath(urIish.getPath().substring(4));
                    }
                    
                    // needed because bitbucket self hosted does not transfer any host information
                    if (StringUtils.isEmpty(url.getHost())) {
                    	urIish = urIish.setHost(url.getHost());
                    }
                    
                    LOGGER.log(Level.FINE, "Trying to match {0} ", urIish.toString() + "<-->" + url.toString());
                    if (GitStatus.looselyMatches(urIish, url)) {
                        return true;
                    }
                }
            }
        } else if (scm instanceof MercurialSCM) {
            try {
                URI hgUri = new URI(((MercurialSCM) scm).getSource());
                String remote = url.toString();
                if (looselyMatches(hgUri, remote)) {
                    return true;
                }
            } catch (URISyntaxException ex) {
                LOGGER.log(Level.SEVERE, "Could not parse jobSource uri: {0} ", ex);
            }
        }
        return false;
    }

    private boolean looselyMatches(URI notifyUri, String repository) {
        boolean result = false;
        try {
            URI repositoryUri = new URI(repository);
            result = Objects.equal(notifyUri.getHost(), repositoryUri.getHost())
            && Objects.equal(notifyUri.getPath(), repositoryUri.getPath())
            && Objects.equal(notifyUri.getQuery(), repositoryUri.getQuery());
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, "Could not parse repository uri: {0}, {1}", new Object[]{repository, ex});
        }
        return result;
    }

    private static final Logger LOGGER = Logger.getLogger(BitbucketJobProbe.class.getName());

}
