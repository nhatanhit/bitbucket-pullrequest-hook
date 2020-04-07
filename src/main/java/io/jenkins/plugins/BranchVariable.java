package io.jenkins.plugins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class BranchVariable extends AbstractDescribableImpl<BranchVariable> {
	public static class DescriptorImpl extends Descriptor<BranchVariable> {
	    @Override
	    public String getDisplayName() {
	      return "";
	    }
	}
	
	private  String branchName;
	
	
	@DataBoundConstructor
	public BranchVariable(String branchName) {
		this.branchName = branchName;
	}
	
	public String getBranchName() {
		return this.branchName;
	}

	@DataBoundSetter
	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	@Override
	public String toString() {
	   return "BranchVariable [branchName=" + branchName + "]";
	}
	
	@Extension public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	
}