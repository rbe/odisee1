package com.bensmann.ooo

import groovy.sql.Sql
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.DOMBuilder
import org.odisee.OOoFileFormat

/**
 * 
 */
class OooDocumentController {
	
	/**
	 * The document service.
	 */
	def oooDocumentService
	
	/**
	 * The OOo service.
	 */
	def oooService
	
	/**
	 * TODO: Get configuration: 1. from Grails config, 2. (overwrite with) request parameters.
	protected def getConfig() {
	}
	 */
	
	/**
	 * Check (and appy default values) request parameter for streaming.
	 */
	protected def checkStreamParameter(params) {
		// Check parameter: document type defaults to PDF.
		if (!params.outputFormat) {
			params.outputFormat = "pdf"
		}
		if (!params.streamtype) {
			params.streamtype = params.outputFormat
		}
		params
	}
	
	/**
	 * Stream a certain document depending on request parameters.
	 * @param params Request parameter
	 * @param document Result from oooService.generate()
	 */
	protected def streamRequestedDocument(params, document) {
		if (log.traceEnabled) log.trace "streamRequestedDocument(${params.inspect()}, document.size=${document?.size()})"
		params = checkStreamParameter(params)
		// Stream document
		if (document instanceof List && !params.nostream) {
			// Find document by request parameter 'type'
			def d = document.find {
				//println "${it.mimeType?.name} == ${params.streamtype} || ${it.filename}.endsWith(${params.streamtype})"
				it.mimeType?.name == params.streamtype || it.filename?.endsWith(params.streamtype)
			}
			if (d) {
				if (log.debugEnabled) log.debug "streaming ${d} by type=${params.streamtype}"
				ControllerHelper.stream(response: response, document: d)
			} else {
				def e = "Couldn't find generated document by streamtype: ${params.streamtype}"
				log.error e
				render e
			}
		} else if (document && !params.nostream) {
			ControllerHelper.stream(response: response, document: document)
		} else if (params.nostream || !params.streamtype) {
			render "No stream requested"
		} else {
			render "No document generated"
		}
	}
	
	/**
	 * Generate one or more document(s) using OooService.
	 */
	protected def _generate(arg) {
		def prop = oooService.mapToOooProps(map: arg)
		def xml = new StreamingMarkupBuilder().bind {
			odisee() {
				request(name: arg.name ?: arg.template, id: prop.id.value) {
					// Example for pre-save-macro: "Standard.oooservice.preSave?language=Basic&amp;location=document",
					ooo(
						host: arg.ooohost ?: "127.0.0.1",
						port: arg.oooport ?: 2002,
						"pre-save-macro": arg.preSaveMacro,
						outputPath: arg.outputPath ?: "",
						outputFormat: arg.list("outputFormat") as String[]
					)
					template(name: arg.template, revision: arg.revision ?: "LATEST")
					archive(database: arg.archivedb ?: false, files: arg.archivefiles ?: true)
					userfields {
						prop.each { k, v ->
							userfield(name: k, "post-set-macro": v.postSetMacro, v.value)
						}
					}
				}
			}
		}
		if (log.debugEnabled) log.debug "XML=${xml.toString()}"
		_sendXmlRequest(xml.toString())
	}
	
	/**
	 * Call OooService with a DOM-ified XML request.
	 * @param arg May be a XmlSlurped document (via request.XML) or a String.
	 */
	protected def _sendXmlRequest(arg) {
		if (log.traceEnabled) log.trace "_sendXmlRequest(${arg.dump()})"
		if (arg instanceof groovy.util.slurpersupport.NodeChild) {
			// Build Odisee XML request using StreamingMarkupBuilder
			def xml
			if (arg instanceof String || arg instanceof GString) {
				xml = new StreamingMarkupBuilder().bind { mkp.yieldUnescaped arg }.toString()
			} else {
				xml = new StreamingMarkupBuilder().bind { mkp.yield arg }.toString()
			}
			arg = DOMBuilder.parse(new StringReader(xml)).documentElement
		}
		if (log.traceEnabled) log.trace "_sendXmlRequest(${arg.dump()})"
		//assert arg instanceof org.apache.xerces.dom.DeferredNode
		//assert arg instanceof com.sun.org.apache.xerces.internal.dom.DeferredElementNSImpl
		try {
			oooService.generateDocument(xml: arg)
		} catch (e) {
			log.error "_sendXmlRequest(${arg.inspect()}): ${e}"
			render e.message
		}
	}
	
	/**
	 * Before request interceptor.
	 */
	def beforeInterceptor = {
		// Don't cache our response.
		response.setHeader("Cache-Control", "no-cache,no-store,must-revalidate,max-age=0")
	}
	
	/**
	 * The index action.
	 */
	def index = {
	}
	
	/**
	 * List all document.
	 */
	def list = {
		[documents: OooDocument.list([sort: "id", order: 'desc'])]
	}
	
	/**
	 * Add a document to document service.
	 */
	def add = {
		if (params.name && params.url) {
			oooDocumentService.addDocument(file: params.name, data: params.url)
			redirect(action: "list")
		} else if (params.file) {
			def f = request.getFile("file")
			oooDocumentService.addDocument(filename: f.originalFilename, data: f.bytes)
			redirect(action: "list")
		}
	}
	
	/**
	 * Stream a document from document service.
	 */
	def stream = {
		def params = checkStreamParameter(params)
		// Fetch and stream document
		def document
		if (params.id) {
			if (log.debugEnabled) log.debug "Fetching document by ID=${params.id}"
			document = OooDocument.get(params.id)
		} else if (params.name && params.revision) {
			if (log.debugEnabled) log.debug "Fetching document by name=${params.name} and revision=${revision}"
			document = oooDocumentService.getDocument(name: params.name, revision: params.revision, mimeType: params.mimetype)
		} else if (params.name) {
			if (log.debugEnabled) log.debug "Fetching document by name=${params.name} and latest revision"
			document = oooDocumentService.getDocument(name: params.name, mimeType: params.mimetype)
			// Generate document
			if (!document) {
				params.template = params.name
				document = _generate(params)
				if (log.debugEnabled) log.debug "Document not found, generated new document=${document}, params=${params}"
			}
		}
		// Stream document
		if (document) {
			ControllerHelper.stream(params: params, response: response, document: document)
		} else {
			log.error "Can't stream. Maybe insufficient parameters: params=${params}"
			render "No data."
		}
	}
	
	/**
	 * Examples:
	 * 1. GET http://.../ooo/oooDocument/generate?template=xxx&type=odt&sql=xxx&param=value...
	 * 2. GET http://.../ooo/oooDocument/generate?template=xxx&type=odt&param=value...
	 * 3. POST http://.../ooo/oooDocument/generate
	 *		<odisee>
	 *			<request type="" id="">
	 *				<userfields>
	 *					<field name="" post-set-macro="">value</field>
	 *				</userfields>
	 *			</request>
	 *		</odisee>
	 */
	def generate = {
		// TODO If a generated document already exists (id/name/template) don't create and send XML request
		def params = checkStreamParameter(params)
		try {
			// Generate a document using a template from database and value(s) from database query.
			if (params.sql) {
				// Fetch data from database using native SQL query, not Hibernate
				// TODO: get query from config by id and use params in GString
				def result = new Sql(dataSource).rows(/**/)
				// Stream document
				streamRequestedDocument(params, _generate(result))
			}
			// Generate document using posted XML request for Odisee
			else if (request.XML) {
				// Set streamtype from XML for streamRequestedDocument()
				params.streamtype = request.XML.request.ooo.@outputFormat?.text()
				streamRequestedDocument(params, _sendXmlRequest(request.XML))
			}
			// Generate a document using a template from database and value(s) from request parameters.
			else if (params.template) {
				streamRequestedDocument(params, _generate(params))
			}
			//
			else {
				log.error "Insufficient parameters: params=${params}"
				render "Sorry."
			}
		} catch (e) {
			e.printStackTrace()
			log.error "Error parsing request: ${e}"
			render "Sorry."
		}
	}
	
}
