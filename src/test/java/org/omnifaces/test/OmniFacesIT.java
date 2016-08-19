package org.omnifaces.test;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.omnifaces.util.Reflection.toClassOrNull;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.openqa.selenium.WebDriver;

public abstract class OmniFacesIT {

	@Drone
	protected WebDriver browser;

	@ArquillianResource
	protected URL contextPath;

	@Before
	public void init() {
		browser.get(contextPath + getClass().getSimpleName() + ".xhtml");
	}

	public static class ArchiveBuilder {

		private Class<?> testClass;
		private WebArchive archive;
		private boolean beanSet;
		private boolean facesConfigSet;
		private boolean webXmlSet;

		public static <T extends OmniFacesIT> ArchiveBuilder buildWebArchive(Class<T> testClass) {
			return new ArchiveBuilder(testClass);
		}

		public static <T extends OmniFacesIT> WebArchive createWebArchive(Class<T> testClass) {
			return buildWebArchive(testClass).createDeployment();
		}

		private <T extends OmniFacesIT> ArchiveBuilder(Class<T> testClass) {
			this.testClass = testClass;
			String packageName = testClass.getPackage().getName();
			String className = testClass.getSimpleName();
			String warName = className + ".war";
			String xhtmlName = packageName + "/" + className + ".xhtml";

			archive = create(WebArchive.class, warName)
				.addAsWebResource(xhtmlName)
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
				.addAsLibrary(new File(System.getProperty("omnifaces.jar")));
		}

		public ArchiveBuilder withBean(Class<?> beanClass) {
			archive.addClass(beanClass);
			beanSet = true;
			return this;
		}

		public ArchiveBuilder withFacesConfig(FacesConfig facesConfig) {
			if (facesConfigSet) {
				throw new IllegalStateException("There can be only one faces-config.xml");
			}

			archive.addAsWebInfResource("WEB-INF/faces-config.xml/" + facesConfig.name() + ".xml", "faces-config.xml");
			facesConfigSet = true;
			return this;
		}

		public ArchiveBuilder withWebXml(WebXml webXml) {
			if (webXmlSet) {
				throw new IllegalStateException("There can be only one web.xml");
			}

			archive.setWebXML("WEB-INF/web.xml/" + webXml.name() + ".xml");

			if (webXml == WebXml.withErrorPage) {
				archive.addAsWebInfResource("WEB-INF/500.xhtml");
			}

			webXmlSet = true;
			return this;
		}

		public WebArchive createDeployment() {
			if (!beanSet) {
				Class<?> possibleBean = toClassOrNull(testClass.getName() + "Bean");

				if (possibleBean != null) {
					withBean(possibleBean);
				}
			}

			if (!facesConfigSet) {
				withFacesConfig(FacesConfig.basic);
			}

			if (!webXmlSet) {
				withWebXml(WebXml.basic);
			}

			return archive;
		}
	}

	public static enum FacesConfig {
		basic,
		withFullAjaxExceptionHandler;
	}

	public static enum WebXml {
		basic,
		withErrorPage;
	}

}