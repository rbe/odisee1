package com.bensmann.ooo

/**
 * A helper for controllers.
 */
class ControllerHelper {
	
	/**
	 * Stream a OooDocument or just bytes to client.
	 */
	def static stream(arg) {
		def bytes
		def response = arg.response
		def document = arg.document
		def contentType
		def contentLength
		def contentName
		// OooDocument
		if (document instanceof OooDocument) {
			contentType = document.mimeType.browser ?: "application/octet-stream"
			contentLength = document.data?.length() ?: document.bytes?.length
			contentName = document.filename
			if (document.data) {
				bytes = document.data?.getBytes(1L, document.data?.length().toInteger())
			} else if (document.bytes) {
				bytes = document.bytes
			}
		}
		// byte[] 
		else if (document instanceof byte[]) {
			contentType = "application/octet-stream"
			contentLength = document.length
			contentName = "file_${new Random().nextInt(System.currentTimeMillis())}"
			bytes = document
		}
		// List of documents
		else if (document instanceof List) {
			def d = document.find {
				// Find document to stream by request parameter 'type'
				it.mimeType?.name == arg.params.streamtype || it.filename.endsWith(arg.params.streamtype)
			}
			contentType = d.mimeType.browser
			bytes = d.toByteArray()
			contentLength = bytes.length
			contentName = d.filename ?: d.name
		}
		// No data message
		else {
			contentType = "text/plain"
			bytes = "No data".bytes
			contentLength = bytes.length
		}
		// Stream to client
		if (bytes) {
			// Content type and length
			response.contentType = contentType
			if (contentLength) response.contentLength = contentLength
			// Content disposition
			def cd = arg.contentDisposition ?: 'inline'
			if (contentName) {
				response.setHeader("Content-disposition", "${cd}; filename=${contentName}")
			}
			// Append bytes to output stream
			response.outputStream << bytes
			// Flush stream
			response.outputStream.flush()
			// println "streamed: type=${contentType} length=${contentLength} content-disposition=${cd}"
		}
	}
	
}