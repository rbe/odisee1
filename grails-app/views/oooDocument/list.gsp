			<p>
				Got ${documents?.size()} document(s)
			</p>
			<table>
				<g:each in="${documents}" var="item">
					<tr>
						<td>${item.id}</td>
						<td><ooo:stream id="${item.id}">stream by ID</ooo:stream>
						<td><ooo:stream name="${item.name}">stream by name, latest rev</ooo:stream>
						<td><ooo:stream name="${item.name}" mimetype="${item.extension}">stream by name, latest rev, type=${item.extension}</ooo:stream>
						<td><ooo:stream name="${item.name}" mimetype="doc" firma="bla">stream by name, latest rev, type=doc</ooo:stream>
						<td>${item.name}</td>
						<td>${item.template ? "yes" : "no"}</td>
						<td>${item.revision}</td>
						<td>${item.instanceOfName} ${item.instanceOfRevision}</td>
						<td>${item.filename}</td>
						<td>${item.extension}</td>
						<td>${item.mimeType.name}</td>
						<td>${item.data?.length()}</td>
					</tr>
				</g:each>
			</table>
