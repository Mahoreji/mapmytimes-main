package in.mapmytour.blog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.mapmytour.blog.dto.request.blogpost.CreateBlogPostRequest;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Converter to handle CreateActivityRequest and CreateAdventureRequest from application/octet-stream
 */
@Component
public class OctetStreamToJsonConverter extends AbstractHttpMessageConverter<Object> {

    private final ObjectMapper objectMapper;

    public OctetStreamToJsonConverter(ObjectMapper objectMapper) {
        super(MediaType.APPLICATION_OCTET_STREAM);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean supports(@NonNull Class<?> clazz) {
        return CreateBlogPostRequest.class.isAssignableFrom(clazz);
    }

    @Override
    @NonNull
    protected Object readInternal(@NonNull Class<?> clazz, @NonNull HttpInputMessage inputMessage) throws IOException {
        return objectMapper.readValue(inputMessage.getBody(), clazz);
    }

    @Override
    protected void writeInternal(@NonNull Object object, @NonNull HttpOutputMessage outputMessage) throws IOException {
        objectMapper.writeValue(outputMessage.getBody(), object);
    }
}
