package jenkins.plugins.tracpublisher;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A notifier that attaches comments to Trac tickets upon successful build. The
 * notifier searches the changesets for references to ticket numbers and adds a
 * link to the jenkins build to those issues. It enables people to easily find
 * builds containing changes related to issues.
 * 
 * @author batkinson
 */
public class TracPublisher extends Notifier {

	public String rpcAddress;
	public String username;
	public String password;
	public boolean useDetailedComments;

	@DataBoundConstructor
	public TracPublisher(String rpcAddress, String username, String password,
			boolean useDetailedComments) {
		this.rpcAddress = rpcAddress;
		this.username = username;
		this.password = password;
		this.useDetailedComments = useDetailedComments;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		disableCertificateValidation();
		new TracIssueUpdater(build, listener, rpcAddress, username, password,
				useDetailedComments).updateIssues();
		return true;
	}
	
	public static void disableCertificateValidation() {
		System.out.println("disableCertificateValidation");
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] arg0, String arg1)
					throws CertificateException {
			}

			public void checkServerTrusted(
					java.security.cert.X509Certificate[] arg0, String arg1)
					throws CertificateException {
			}

			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		}};

		// Ignore differences between given hostname and certificate hostname
		HostnameVerifier verifier = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(verifier);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			load();
		}

		private String rpcAddress;
		private String username;
		private String password;
		private boolean useDetailedComments;

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Add link to Trac issues";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws hudson.model.Descriptor.FormException {

			rpcAddress = json.getString("rpcAddress");
			username = json.getString("username");
			password = json.getString("password");
			useDetailedComments = json.getBoolean("useDetailedComments");

			save();

			return super.configure(req, json);
		}

		public String getRpcAddress() {
			return rpcAddress;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		public boolean isUseDetailedComments() {
			return useDetailedComments;
		}

	}
}
