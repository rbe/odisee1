import com.bensmann.glue.*
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler

/**
 * 
 */
class OooGrailsPlugin {
	
	/**
	 * Author and plugin.
	 */
	def author = "Ralf Bensmann"
	def authorEmail = "grails@bensmann.com"
	def title = "OOo for Grails."
	def description = """Generate documents using OpenOffice.org and maintain any type of documents through a document service."""
	
	/**
	 * URL to the plugin's documentation.
	 */
	def documentation = "http://grails.org/ooo+Plugin"
	
	/**
	 * The plugin version.
	 */
	def version = "1.6.10"
	
	/**
	 * The version or versions of Grails the plugin is designed for.
	 */
	def grailsVersion = "1.3.1 > *"
	
	/**
	 * Other plugins this plugin depends on.
	 */
	def dependsOn = [
		controllers: GrailsUtil.grailsVersion,
		core: GrailsUtil.grailsVersion,
		hibernate: GrailsUtil.grailsVersion,
		glue: "1.3.0 > *"
	]
	
	/**
	 * 
	 */
	def loadAfter = [
		"controllers",
		"hibernate"
	]
	
	/**
	 * Other plugins influenced by this plugin.
	 * See http://www.grails.org/Auto+Reloading+Plugins
	 */
	def influences = [
		"controllers",
		"hibernate"
	]
	
	/**
	 * Plugins to observe for changes.
	 * See http://www.grails.org/Auto+Reloading+Plugins
	 */
	def observe = [
		"controllers",
		"hibernate"
	]
	
	/**
	 * Resources to watch.
	 * See http://www.grails.org/Auto+Reloading+Plugins
	 */
	def watchedResources = []
	
	/**
	 * Resources that are excluded from plugin packaging.
	 */
	def pluginExcludes = [
		"grails-app/conf/",
	]
	
	/**
	 * Implement runtime spring config (optional).
	 * See http://www.grails.org/Runtime+Configuration+Plugins
	 */
	def doWithSpring = {
		// Constraints
		//ConstrainedProperty.registerNewConstraint(BestFrameworkConstraint.NAME, BestFrameworkConstraint.class);
	}
	
	/**
	 * Implement post initialization spring config (optional).
	 * See http://www.grails.org/Runtime+Configuration+Plugins
	 */
	def doWithApplicationContext = { applicationContext ->
	}
	
	/**
	 * Implement additions to web.xml (optional).
	 * See http://www.grails.org/Runtime+Configuration+Plugins
	 */
	def doWithWebDescriptor = { xml ->
	}
	
	/**
	 * Inject methods into controller.
	 * @param ctx Context.
	 * @param c Grails artefact.
	 */
	private def injectMethodsIntoController(ctx, c) {
		//println "${this.class.name}: injecting methods into controller ${c}"
	}
	
	/**
	 * Inject services into controller.
	 * @param ctx Context.
	 * @param c Grails artefact.
	 */
	private def injectServicesIntoController(ctx, c) {
		//println "${this.class.name}: injecting services into controller ${c}"
	}
	
	/**
	 * Put some liquid glue in a controller...
	 */
	private def controllerLiquidGlue(ctx, c) {
		/*injectServicesIntoController(ctx, c)
		injectMethodsIntoController(ctx, c)*/
	}
	
	/**
	 * Put some liquid glue in all controllers...
	 */
	private def allControllersLiqiudGlue(application, ctx) {
		application.controllerClasses.each { c ->
			controllerLiquidGlue(ctx, c)
		}
	}
	
	/**
	 * Implement registering dynamic methods to classes (optional).
	 * See http://www.grails.org/Plugin+Dynamic+Methods
	 */
	def doWithDynamicMethods = { ctx ->
		// Controllers
		allControllersLiqiudGlue(application, ctx)
	}
	
	/**
	 * Implement code that is executed when any artefact that this plugin is
	 * watching is modified and reloaded. The event contains: event.source,
	 * event.application, event.manager, event.ctx, and event.plugin.
	 */
	def onChange = { event ->
		println "${this.class.name}: onChange"
		// Controllers
		if (application.isArtefactOfType(ControllerArtefactHandler.TYPE, event.source)) {
		}
	}
	
	/**
	 * Implement code that is executed when the project configuration changes.
	 * The event is the same as for 'onChange'.
	 */
	def onConfigChange = { event ->
		println "${this.class.name}: onConfigChange"
	}
	
	/**
	 * 
	 */
	def onShutdown = { event ->
		println "${this.class.name}: shutdown"
	}
	
}
