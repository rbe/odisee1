package com.bensmann.ooo

import groovy.xml.XmlUtil
import groovy.xml.dom.DOMCategory
import org.odisee.document.writer.OOoTextTemplate

import java.text.SimpleDateFormat

/**
 * A service that is a client for the OOo service. Provides access to standalone, embedded or webservice
 * version of the service.
 */
class OooService {

    /**
     * The scope. See http://www.grails.org/Services.
     */
    def scope = "prototype" // prototype request flash flow conversation session singleton

    /**
     * Transactional?
     */
    boolean transactional = true

    /**
     * Grails.
     */
    def grailsApplication

    /**
     * The OOo document service.
     */
    def oooDocumentService

    /**
     * Convert a simple map with values for OpenOffice userfields (e.g. controller's parameters)
     * into properties for generating an OOo document.
     */
    def mapToOooProps(arg) {
        // Create map with values for template
        def prop = [:]
        prop.id = [
                value: arg.map.id ?: 0
        ]
        arg.map.findAll { k, v ->
            // Skip Grails' controller parameters
            !(k in ["controller", "action", "template"])
        }?.each { k, v ->
            prop."${k}" = [
                    value       : v,
                    postSetMacro: arg.map."${k}_postSetMacro"
            ]
        }
        prop
    }

    /**
     * Get template from document service and save it to disk.
     */
    private def saveTemplate(arg) {
        if (log.traceEnabled) log.trace "saveTemplate: ${arg}"
        // Do we have the template?
        def hasTemplate = oooDocumentService.hasDocument(name: arg.template, revision: arg.revision)
        if (!hasTemplate) {
            log.error "Can't find template ${arg.template}"
            return
        }
        // Retrieve template via DocumentService and save it to a file
        def tmpl = new File(arg.baseDir, "${arg.template}_rev${arg.revision}.ott")
        if (!tmpl.exists()) {
            def bd = oooDocumentService.getDocumentData(name: arg.template, revision: arg.revision)
            if (bd) {
                tmpl.withOutputStream { os ->
                    os.write(bd, 0, bd.length)
                }
                if (log.debugEnabled) log.debug "Saved template ${arg.template} revision ${arg.revision} as ${tmpl.getAbsolutePath()}"
            } else {
                throw new IllegalStateException("Cannot save template, template ${arg.template} with revision ${arg.revision} not found!")
            }
        } else {
            if (log.debugEnabled) log.debug "Template ${arg.template} revision ${arg.revision} already exists at ${tmpl.getAbsolutePath()}"
        }
        tmpl
    }

    /**
     * Create our base/working directory.
     */
    private def createDir(arg) {
        // Set base directory
        def baseDir = new File("${this.class.simpleName}/${arg.template}/rev${arg.revision}/id${arg.id}")
        baseDir.mkdirs()
        baseDir
    }

    /**
     * Delete working directory? Depends on arg.keepWorkingDirectory.
     */
    private def cleanupWorkingDirectory(arg) {
        use(DOMCategory) {
            def files = arg.xml.request.archive[0]?."@files" == "false"
            if (log.debugEnabled) log.debug "cleanupWorkingDirectory(${arg.inspect()}): files=${arg.xml.request.archive?."@files"} -> ${files}"
            if (files) {
                try {
                    if (log.debugEnabled) log.debug "cleanupWorkingDirectory(${arg.inspect()}): deleting: ${arg.baseDir}"
                    arg.baseDir.deleteDir()
                } catch (e) {
                    log.error "cleanupWorkingDirectory(${arg.inspect()}): Couldn't delete working directory ${arg.baseDir}: ${e}"
                }
            }
        }
    }

    /**
     * Archive document(s).
     * @param arg Map: bytes: array of byte arrays
     * @return Array of OooDocument instance(s).
     */
    private def archive(arg) {
        def document = []
        def d
        if (log.debugEnabled) log.debug "archive: ${arg.xml.request.archive?.@database}"
        arg.bytes.each { k, v ->
            try {
                d = oooDocumentService.addDocument(
                        instanceOf: [name: arg.template, revision: arg.revision],
                        filename: k.name,
                        odiseeRequest: arg.xmlString,
                        data: v
                )
                d?.refresh()
                document << d
                if (log.debugEnabled) log.debug "Document archived: ${d}"
            } catch (e) {
                e.printStackTrace()
                log.error "Couldn't archive document ${d}: ${e}"
            }
        }
        document
    }

    /**
     * Generate a document using document service and OOo service.
     * @param arg Map: xml: an XML request (see request.xsd in Odisee)
     * @return Instance of OooDocument or byte array (depends on arg.archive)
     */
    def generateDocument(arg) {
        def document
        // We need an Odisee XML request as a DOM
        assert arg.xml
        /* Convert string into DOM
        if (arg.xml instanceof String || arg.xml instanceof GString) {
            arg.xml = DOMBuilder.parse(new StringReader(arg.xml)).documentElement
        }
        assert arg.xml instanceof org.apache.xerces.dom.DeferredNode
        */
        // Check and extract some values
        use(DOMCategory) {
            arg.template = arg.xml.request.template[0]."@name"
            assert arg.template
            // The ID, if none given use actual date and time
            if (!arg.xml.request[0]."@id") {
                // Save in XML request for Odisee
                arg.xml.request[0].setAttribute("id", new SimpleDateFormat("yyyyMMdd-Hms_SSS").format(new Date()))
            }
            arg.id = arg.xml.request[0]."@id"
            // Get (latest) revision of template?
            arg.revision = arg.xml.request.template[0]."@revision"
            if (!arg.revision || arg.revision == "LATEST") {
                arg.revision = oooDocumentService.getDocumentsLatestRevision(name: arg.template)
                // Save in XML request for Odisee
                arg.xml.request.template[0].setAttribute("revision", arg.revision.toString())
            }
        }
        // Check arg
        if (!arg.template || !arg.revision || !arg.id) {
            throw new IllegalStateException("No template (${arg.template}), revision (${arg.revision ?: "NOT FOUND"}) or id (${arg.id})")
        }
        // Base/working directory
        arg.baseDir = createDir(arg)
        // Get template
        arg.tmpl = saveTemplate(arg)
        // Check and extract some values
        use(DOMCategory) {
            // Ensure template path in Odisee XML request
            arg.xml.request.template[0].setAttribute("path", arg.tmpl.getAbsolutePath())
            // Ensure document output path for Odisee XML request
            if (!arg.xml.request.ooo[0]."@outputPath") {
                arg.xml.request.ooo[0].setAttribute("outputPath", arg.tmpl.getAbsolutePath())
            }
            // The name
            arg.documentName = "${arg.template}_rev${arg.revision}_id${arg.id}"
        }
        // Serialize XML
        def xmlString = XmlUtil.serialize(arg.xml)
        // Save request to file
        new File(arg.baseDir, "${arg.documentName}_request.xml").withWriter "UTF-8", { writer ->
            writer.write(xmlString)
        }
        // Generate and save document(s)
        try {
            // Generate document: return value is map [file object: bytes]
            arg.bytes = new OOoTextTemplate().createTextDocument(xmlString)
            use(DOMCategory) {
                // Archive generated document(s)?
                if (arg.xml.request.archive[0]?."@database" == "true") {
                    arg.document = archive(arg)
                } else {
                    arg.document = arg.bytes.collect { k, v ->
                        oooDocumentService.createDocument(file: k, data: v)
                    }
                }
            }
        } catch (e) {
            e.printStackTrace()
            log.error "Could not generate document ${arg.documentName}: ${e}"
        }
        //
        cleanupWorkingDirectory(arg)
        arg.document
    }

}
