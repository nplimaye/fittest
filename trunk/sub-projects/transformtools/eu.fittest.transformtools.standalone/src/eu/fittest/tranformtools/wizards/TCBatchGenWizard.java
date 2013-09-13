package eu.fittest.tranformtools.wizards;

import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;

import eu.fbk.se.transform.AbstractTemplateProvider;
import eu.fbk.se.transform.CTE2FlashSelenium;
import eu.fbk.se.transform.CTE2FlexSelenium;
import eu.fbk.se.transform.CTE2RobotiumAndroid;
import eu.fbk.se.transform.CTE2Selenium;
import eu.fbk.se.transform.ITranformer;
import eu.fbk.se.transform.SeleniumDriverTemplateProvider;
import eu.fbk.se.transform.TransformException;
import eu.fittest.tranformtools.Activator;

public class TCBatchGenWizard extends Wizard {

	private Shell shell;
	BatchGenPage paramPage;
	String cteFolder;
	
	public TCBatchGenWizard(Shell shell, String cteFolder) {
		super();
		this.shell = shell;
		this.cteFolder = cteFolder;
	}

	@Override
	public boolean performFinish() {

		if (paramPage != null) {
			cteFolder = paramPage.getCteFolder();
			File f = new File(cteFolder);

			final String[] fileList = f.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".cte");
				}
			});

			if (fileList != null && fileList.length > 0) {


				Job generateJob = new Job("Generate Test Cases") {

					@Override
					protected IStatus run(final IProgressMonitor monitor) {
						
						monitor.beginTask("Generating test cases...", fileList.length);
						String selenDriver = paramPage.getSeleniumDriver();
						String templateGroupFile = null;
						if (paramPage.isGenerateForAndroid()) {
							templateGroupFile = Activator.getPluginPath()
									.toOSString()
									+ File.separator
									+ "templates"
									+ File.separator
									+ "robotium.driver.stg";
							
						} else {
							if (selenDriver
									.equals(Constants.FLEX_OBJECT_DRIVER)) {
								templateGroupFile = Activator
										.getPluginPath().toOSString()
										+ File.separator
										+ "templates"
										+ File.separator
										+ "junit.flexdriver.stg";
							} else if (selenDriver
									.equals(Constants.FLASH_OBJECT_DRIVER)) {
								templateGroupFile = Activator
										.getPluginPath().toOSString()
										+ File.separator
										+ "templates"
										+ File.separator
										+ "junit.flashdriver.stg";
							} else {
								templateGroupFile = Activator
										.getPluginPath().toOSString()
										+ File.separator
										+ "templates"
										+ File.separator
										+ "junit4.wdriver.stg";
							}
						}

						for (String s : fileList) {
							String className = s.replace(".cte", "").toUpperCase() + "Test";
							AbstractTemplateProvider templateProvider = new SeleniumDriverTemplateProvider(templateGroupFile);

							if (templateProvider.isTemplateReady()) {
								ITranformer transformer;
								try {
									if (paramPage.isGenerateForAndroid()) {
										transformer = new CTE2RobotiumAndroid(
												templateProvider,
												paramPage.getPackageName(),
												className,
												paramPage.getAndroidTargetPackage(),
												paramPage.getAndroidTargetActivity());
									} else {
										if (selenDriver
												.equals(Constants.FLEX_OBJECT_DRIVER)) {
											transformer = new CTE2FlexSelenium(
													templateProvider,
													paramPage.getPackageName(),
													className,
													paramPage.getTargetPage(),
													selenDriver);
										} else if (selenDriver
												.equals(Constants.FLASH_OBJECT_DRIVER)) {
											transformer = new CTE2FlashSelenium(
													templateProvider,
													paramPage.getPackageName(),
													className,
													paramPage.getTargetPage(),
													selenDriver);
										} else {
											transformer = new CTE2Selenium(
													templateProvider,
													paramPage.getPackageName(),
													className,
													paramPage.getTargetPage(),
													selenDriver);
										}

									}
									String cteFile = cteFolder + File.separator + s;
									if (paramPage.getDomainInputFile() == null || paramPage.getDomainInputFile().isEmpty()){
										transformer.transform(cteFile, paramPage.getOutputFolder(), paramPage.isOnlyValidTestCase());
									} else {
										transformer.transform(cteFile,  paramPage.getDomainInputFile(), paramPage.getOutputFolder(), paramPage.isOnlyValidTestCase());
									}

									monitor.worked(1);
									
								} catch (TransformException e) {
									shell.getDisplay().asyncExec(
											new Runnable() {

												public void run() {
													MessageDialog
															.openError(
																	shell,
																	"Test case generation",
																	"Error occur during the transformation, please check the input parameters!");
												}
											});
									monitor.done();
								}

							}
						}
						monitor.done();
						
						shell.getDisplay().asyncExec(new Runnable() {
							public void run() {
								MessageDialog.openInformation(shell, "Test case generation", 
								"Test generation completed sucessfully !");
							}
						});
						
						return new Status(IStatus.OK, "FITTEST tranformation plugin", 
								IStatus.OK, "Finish Generating Test Suites!", null);
					}

				};
				
				generateJob.schedule();

			}

		}
		
		return true;
	}

	@Override
	public void addPages() {
		ImageDescriptor imgDes = Activator.getImageDescriptor("icons"
				+ File.separator + "fittest-logo-small.jpg");
		paramPage = new BatchGenPage("batchGenPage");
		paramPage.setTitle("Generate test cases from CTE trees");
		paramPage.setDescription("Specifying parameters");
		paramPage.setImageDescriptor(imgDes);

		paramPage.setCteFolder(cteFolder);
		
		addPage(paramPage);

	}

}
