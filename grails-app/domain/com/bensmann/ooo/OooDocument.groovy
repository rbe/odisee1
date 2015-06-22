package com.bensmann.ooo

import com.bensmann.glue.*
import org.hibernate.Hibernate

import java.sql.Blob

/**
 * A document.
 */
class OooDocument {

    /**
     * Date of creation.
     */
    Date dateCreated

    /**
     * Date of last update.
     */
    Date lastUpdated

    /**
     * Name of this document.
     */
    String name

    /**
     * Is this a template?
     */
    Boolean template

    /**
     * Revision of document.
     */
    Long revision

    /**
     * This document is an instance of another -- a template.
     */
    String instanceOfName

    /**
     * This document is an instance of a certain revision of another document -- a template.
     */
    Long instanceOfRevision

    /**
     * The type.
     */
    GlueMimeType mimeType

    /**
     * Original filename.
     */
    String filename

    /**
     * The extension (automatically extracted from filename).
     */
    String extension

    /**
     * The Odisee request.
     */
    String odiseeRequest
    java.sql.Clob odiseeXmlRequest

    /**
     * The binary data.
     */
    byte[] bytes
    Blob data

    /**
     *
     */
    def beforeInsert = {
        check()
    }

    /**
     * TODO There should never be an update...
     */
    def beforeUpdate = {
        check()
    }

    /**
     *
     */
    private def check() {
        // Revision
        if (!revision) {
            revision = 1
        }
        // Set name
        if (!name && filename) {
            name = filename
        }
        // Extension
        try {
            extension = filename?.split("\\.").toList().last() // TODO Throw error when there's no extension?
        } catch (e) {
            // ignore
        }
        // Check if it's a template
        template = false
        // Is a certain string part of the mime type?
        ["template"].each {
            if (mimeType?.name?.indexOf(it) > -1 || mimeType?.browser?.indexOf(it) > -1) {
                template = true
                return
            }
        }
        // Convert bytes property to BLOB
        if (bytes) {
            data = Hibernate.createBlob(bytes)
        }
        // Create CLOB from odiseeRequest
        if (odiseeRequest) {
            odiseeXmlRequest = Hibernate.createClob(odiseeRequest)
        }
    }

    /**
     * TODO: java.lang.UnsupportedOperationException: Blob may not be manipulated from creating session
     */
    def toByteArray() {
        data?.getBytes(1L, data?.length().toInteger())
    }

    static transients = [
            "odiseeRequest",
            "bytes"
    ]

    static constraints = {
        dateCreated(nullable: true, editable: false)
        lastUpdated(nullable: true, editable: false)
        name(nullable: true)
        template(nullable: true)
        revision(nullable: true, editable: false)
        instanceOfName(nullable: true, editable: false)
        instanceOfRevision(nullable: true, editable: false)
        filename(nullable: true, editable: false)
        extension(nullable: true, editable: false)
        mimeType(nullable: true)
        odiseeXmlRequest(nullable: true, maxSize: 1 * 1024 * 1024 * 1024, editable: false)
        data(nullable: true)
    }

    static mapping = {
        table "T2_DOC"
    }

}
