package com.bensmann.ooo

import org.springframework.beans.factory.InitializingBean

class OooDocumentService implements InitializingBean {

    /**
     * The scope. See http://www.grails.org/Services.
     */
    def scope = "prototype" // prototype request flash flow conversation session singleton

    /**
     * Transactional?
     */
    boolean transactional = true

    /**
     *
     */
    def grailsApplication

    /**
     *
     */
    void afterPropertiesSet() {
    }

    /**
     *
     */
    private def getName(filename) {
        def s = filename.split("\\.")
        s[0..s.length - 2].join(".")
    }

    /**
     * Read a file and save its content.
     */
    private def fromFile(path) {
        def f = new File(path)
        if (f.exists() && f.canRead()) {
            f.readBytes()
        } else {
            log.error "Can't find or read file ${path}"
        }
    }

    /**
     * Determine mime type.
     */
    def getMimeType(arg) {
        def mimeType
        // Filename with extension?
        if (arg.name.indexOf(".") > -1) {
            def ext = arg.name.split("\\.").toList().last()
            mimeType = GlueMimeType.findByExtension(ext)
        } else {
            // See GRAILS-1186, findByNameOrExtensionOrBrowser
            mimeType = GlueMimeType.findByExtensionOrBrowser(arg.name, arg.name)
        }
        if (!mimeType) {
            mimeType = GlueMimeType.findByName("Unknown") //"appliction/octet-stream"
            if (log.debugEnabled) log.debug "Could not recognize mime type of ${arg.name}; using application/octet-stream"
        } else {
            if (log.debugEnabled) log.debug "Mime type for ${arg.name} is ${mimeType}"
        }
        mimeType
    }

    /**
     *
     */
    def hasDocument(arg) {
        if (log.traceEnabled) log.trace "hasDocument: arg=${arg}"
        def doc = OooDocument.withCriteria {
            or {
                if (arg.name) {
                    ilike("name", arg.name)
                }
                if (arg.revision) {
                    eq("revision", arg.revision.toLong())
                }
                if (arg.mimeType) {
                    mimeType {
                        eq("name", arg.mimeType)
                    }
                }
            }
            projections {
                count("name")
            }
        }
        if (doc?.size() == 1) {
            true
        } else {
            false
        }
        // Old return value: doc[0]
        //OooDocument.countByNameAndRevision(arg.name, arg.revision ?: 1) ? true : false
    }

    /**
     * Add a document.
     */
    def addDocument(arg) {
        if (log.traceEnabled) log.trace "addDocument(${arg.inspect()})"
        def document = createDocument(arg)
        if (document) {
            // Revision: lookup if we have a document with same name and filename
            document.revision = OooDocument.countByNameAndFilename(document.name, document.filename) + 1
            // Save new document
            document.save(flush: true)
        }
        document
    }

    /**
     * Create a document with 'name' from path. If the document does not exist, revision 1 is created
     * otherwise the revision is incremented by 1.
     */
    def createDocument(arg) {
        if (log.traceEnabled) log.trace "createDocument(${arg.inspect()})"
        // Get filename?
        if (!arg.filename) {
            if (arg.file instanceof java.io.File) {
                arg.filename = arg.file.name
            } else if (arg.data instanceof String) {
                arg.filename = new File(arg.data).name
            } else {
                arg.filename = "unknown"//arg.file
            }
        }
        // Load data depending on type?
        if (!arg.bytes) {
            if (arg.data instanceof String) {
                arg.bytes = fromFile(arg.data)
            } else if (arg.data instanceof InputStream) {
                arg.bytes = arg.data.readBytes()
            } else if (arg.data instanceof byte[]) {
                arg.bytes = arg.data
            }
        }
        // Create new OooDocument
        def document
        if (arg.bytes) {
            document = new OooDocument()
            // Set names
            document.name = getName(arg.filename)
            document.filename = arg.filename
            // Instance of...
            document.instanceOfName = arg.instanceOf?.name
            document.instanceOfRevision = arg.instanceOf?.revision?.toLong()
            // Mime type
            document.mimeType = getMimeType(name: document.filename)
            document.odiseeRequest = arg.odiseeRequest
            //document.data = Hibernate.createBlob(arg.bytes)
            document.bytes = arg.bytes
        } else {
            log.error "createDocument(${arg.inspect()}): failed, no data!"
        }
        document
    }

    /**
     * Get all revisions of a document.
     */
    def getDocumentRevision(arg) {
        OooDocument.findAllByName(arg.name)?.collect { it.revision }
    }

    /**
     * Get latest revision for a document.
     */
    def getDocumentsLatestRevision(arg) {
        getDocumentByLatestRevision(name: arg.name)?.revision
    }

    /**
     * Get a document by name and optionally by revision (default is 1).
     */
    def getDocumentByRevision(arg) {
        OooDocument.findByNameAndRevision(arg.name, arg.revision ?: 1)
    }

    /**
     * Get a document with its latest revision.
     */
    def getDocumentByLatestRevision(arg) {
        try {
            def list = OooDocument.findAllByName(arg.name, [sort: "revision", order: "desc"])
            if (list?.size() > 0) { // TODO Ask database for "latest" revision!?
                list.first()
            }
        } catch (e) { // NoSuchElementException: Cannot access last() from an empty List
            log.error e
        }
    }

    /**
     * Returns one or more document(s) queried by given map keys.
     * @param arg Map: name, revision, mimeType
     */
    def getDocument(arg) {
        OooDocument.withCriteria {
            and {
                ilike("name", arg.name)
                eq("revision", arg.revision?.toLong() ?: getDocumentByLatestRevision(arg)?.revision)
                if (arg.mimeType) {
                    or {
                        mimeType {
                            ilike("name", "%${arg.mimeType}")
                        }
                        ilike("filename", "%${arg.mimeType}")
                    }
                }
            }
        }
    }

    /**
     * Get binary data from document by name and revision.
     */
    def getDocumentData(arg) {
        getDocument(name: arg.name, revision: arg.revision)[0]?.toByteArray()
    }

    /**
     * Get binary data from document's latest revision.
     */
    def getDocumentsLatestRevisionData(arg) {
        getDocumentByLatestRevision(name: arg.name).toByteArray()
    }

}
