package jenkins.plugins.tracpublisher;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jenkins.model.Jenkins;

import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import org.jvnet.hudson.test.HudsonTestCase;
import org.mockito.Mockito;

public class TracPublisherTest extends HudsonTestCase {

	public void testXMLRPCOverHTTPS() throws IOException {
		System.setProperty("javax.net.ssl.trustStore", "/Users/gasol/kkbox.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "pass");
		System.setProperty("javax.net.debug", "all");

		String rpcAddress = "https://localhost/trac/login/xmlrpc";
		String username = "hudsonbugnote";
		String password = "hudson";

		Jenkins jenkins = Mockito.mock(Jenkins.class);
		Mockito.when(jenkins.getRootUrl()).thenReturn("http://example.com");
		AbstractBuild build = Mockito.mock(AbstractBuild.class);
		Mockito.when(build.getDisplayName()).thenReturn("Test");
		Mockito.when(build.getFullDisplayName()).thenReturn("Test");
		Mockito.when(build.getUrl()).thenReturn("http://example.com");
		Mockito.when(build.getChangeSet()).thenReturn(new ChangeLogSet<Entry>(build) {

			public Iterator<Entry> iterator() {
				Set<Entry> issues = new HashSet<Entry>();
				issues.add(new Entry() {

					@Override
					public String getMsg() {
						return "test #10396";
					}

					@Override
					public User getAuthor() {
						return null;
					}

					@Override
					public Collection<String> getAffectedPaths() {
						return null;
					}
				});
				return issues.iterator();
			}

			@Override
			public boolean isEmptySet() {
				return false;
			}
		});
		BuildListener listener = new StreamBuildListener(System.out);
		Mockito.when(build.getResult()).thenReturn(Result.SUCCESS);
		TracIssueUpdater updater = new TracIssueUpdater(build, listener, rpcAddress, username, password, false);
		updater.updateIssues();
	}

}
