package net.greatsoft.retryclient.interceptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

public class ClientHttpResponseWrapper implements ClientHttpResponse {

	private final ClientHttpResponse delegate ;
	private final byte[] buffer;
	public ClientHttpResponseWrapper(ClientHttpResponse delegate) {
		this.delegate = delegate ;
		try {
			buffer = StreamUtils.copyToByteArray(delegate.getBody()) ;
		} catch (IOException e) {
			throw new RuntimeException("复制响应body错误", e) ;
		}
	}
	
	@Override
	public InputStream getBody() throws IOException {
		return new ByteArrayInputStream(buffer) ;
	}

	@Override
	public HttpHeaders getHeaders() {
		return delegate.getHeaders();
	}

	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return delegate.getStatusCode() ;
	}

	@Override
	public String getStatusText() throws IOException {
		return delegate.getStatusText() ;
	}

	@Override
	public void close() {
		this.delegate.close() ; 
	}
}
