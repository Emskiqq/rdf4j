package org.eclipse.rdf4j.tools.serverboot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class CompressRequestFilter implements Filter {

	private static final int MIN_COMPRESSION_SIZE = 1024;
	private static final Set<String> EXCLUDED_CONTENT_TYPES = Set.of(
			"image/", "video/", "audio/",
			"application/zip", "application/pdf", "application/octet-stream",
			"application/javascript", "font/woff2", "font/woff",
			"application/json", "text/css", "text/html"
	);

	private List<Pattern> excludePathPatterns = new ArrayList<>();

	@Override
	public void init(FilterConfig filterConfig) {
		String excludePathsParam = filterConfig.getInitParameter("excludePathPatterns");
		if (excludePathsParam != null) {
			excludePathPatterns = Arrays.stream(excludePathsParam.split(","))
					.map(String::trim)
					.map(Pattern::compile)
					.collect(Collectors.toList());
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			chain.doFilter(request, response);
			return;
		}
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String requestURI = httpRequest.getRequestURI();
		for (Pattern pattern : excludePathPatterns) {
			if (pattern.matcher(requestURI).matches()) {
				chain.doFilter(request, response);
				return;
			}
		}

		HttpServletResponse httpResponse = (HttpServletResponse) response;
		DeferredResponseWrapper wrappedResponse = new DeferredResponseWrapper(httpResponse);
		chain.doFilter(request, wrappedResponse);

		String contentType = wrappedResponse.getContentType();
		if (isExcludedContentType(contentType)) {
			wrappedResponse.writeUncompressed();
			return;
		}

		String acceptEncoding = httpRequest.getHeader("Accept-Encoding");
		boolean supportsGzip = (acceptEncoding != null && acceptEncoding.contains("gzip"));

		if (!supportsGzip) {
			wrappedResponse.writeUncompressed();
			return;
		}

		byte[] data = wrappedResponse.getData();
		if (data.length < MIN_COMPRESSION_SIZE) {
			wrappedResponse.writeUncompressed();
			return;
		}

		httpResponse.setHeader("Content-Encoding", "gzip");
		try (GZIPOutputStream gzipStream = new GZIPOutputStream(httpResponse.getOutputStream())) {
			gzipStream.write(wrappedResponse.getData());
		}
	}

	@Override
	public void destroy() {
	}

	/**
	 * Check if the content type is in our exclusion list/prefixes static loaded content.
	 */
	private boolean isExcludedContentType(String contentType) {
		if (contentType == null)
			return false;
		String baseType = contentType.split(";")[0].trim();
		return EXCLUDED_CONTENT_TYPES.contains(baseType) ||
				EXCLUDED_CONTENT_TYPES.stream().anyMatch(baseType::startsWith);
	}

	private static class DeferredResponseWrapper extends HttpServletResponseWrapper {
		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		private PrintWriter writer;
		private ServletOutputStream outputStream;

		public DeferredResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() {
			if (outputStream == null) {
				outputStream = new ServletOutputStream() {
					@Override
					public void write(int b) {
						buffer.write(b);
					}

					@Override
					public boolean isReady() {
						return true;
					}

					@Override
					public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
					}
				};
			}
			return outputStream;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (writer == null) {
				writer = new PrintWriter(new OutputStreamWriter(buffer, getCharacterEncoding()));
			}
			return writer;
		}

		public byte[] getData() {
			if (writer != null) {
				writer.flush();
			}
			return buffer.toByteArray();
		}

		public void writeUncompressed() throws IOException {
			getResponse().getOutputStream().write(getData());
		}
	}
}
