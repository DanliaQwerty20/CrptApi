package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

public class CrptApi {

    private final String BASE_URL = "https://ismp.crpt.ru/api/v3";
    private String authToken = "";
    private final TimeUnit TIME_UNIT;
    private final int DEFAULT_REQUEST_LIMIT = 1;
    private final ArrayBlockingQueue<Long> REQUEST_TIME_QUEUE;
    private final Converter jsonConverter = new JsonConverter();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.TIME_UNIT = timeUnit;
        this.REQUEST_TIME_QUEUE = new ArrayBlockingQueue<>(requestLimit < 1 ? DEFAULT_REQUEST_LIMIT : requestLimit);
    }

    public String createDocumentForRussianGoods(Document document, String signature) {
        return createDocument(document, signature, new DocumentFormat("MANUAL"), String.valueOf(new Type("LP_INTRODUCE_GOODS")), jsonConverter);

    }

    private String createDocument(Document document, String signature,
                                  DocumentFormat documentFormat, String documentType, Converter converter) {
        final String CREATE_DOCUMENT_URL = BASE_URL.concat("/lk/documents/create");
        checkRequestLimit();

        String documentEncoded = encodeBase64(converter.convert(document));
        String signatureEncoded = encodeBase64(signature);
        Body body = new Body(documentFormat, documentEncoded, signatureEncoded, documentType);

        Content result = null;
        try {
            result = Request.Post(CREATE_DOCUMENT_URL)
                    .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                    .bodyString(jsonConverter.convert(body), ContentType.APPLICATION_JSON)
                    .execute().returnContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result == null ? "" : result.asString();
    }

    private void checkRequestLimit() {
        while (!REQUEST_TIME_QUEUE.offer(Instant.now().toEpochMilli())) {
            long oldestRequestTimestamp = REQUEST_TIME_QUEUE.peek();
            long elapsedTime = Instant.now().toEpochMilli() - oldestRequestTimestamp;

            if (elapsedTime >= TIME_UNIT.toMillis(1)) {
                REQUEST_TIME_QUEUE.remove(oldestRequestTimestamp);
            } else {
                try {
                    Thread.sleep(TIME_UNIT.toMillis(1) - elapsedTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String encodeBase64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    @Getter
    @Setter
    public static class Body {
        private DocumentFormat documentFormat;
        private String productDocument;
        private ProductGroup productGroup;
        private String signature;
        private String type;

        public Body(DocumentFormat documentFormat, String productDocument,
                    String signature, String type) {
            this.documentFormat = documentFormat;
            this.productDocument = productDocument;
            this.signature = signature;
            this.type = type;
        }
    }

    public interface Converter {
        String convert(Object o);
    }

    public static class JsonConverter implements Converter {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convert(Object o) {
            try {
                return objectMapper.writeValueAsString(o);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Getter
    @Setter
    public static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private List<Product> products;
        private String regDate;
        private String regNumber;

        public Document(String docId, String docStatus, String docType, String ownerInn,
                        String participantInn, String producerInn, String productionDate,
                        String productionType, String regDate) {
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.regDate = regDate;
        }
    }

    @Getter
    @Setter
    public static class Description {
        private String participantInn;

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    @Getter
    @Setter
    public static class Product {
        private CertificateDocument certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;

        public Product(String ownerInn, String producerInn,
                       String productionDate, String tnvedCode) {
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.tnvedCode = tnvedCode;
        }
    }

    @Getter
    @Setter
    public static class CertificateDocument {
        private String type;

        public CertificateDocument(String type) {
            this.type = type;
        }
    }

    @Getter
    @Setter
    public static class Type {
        private String value;

        public Type(String value) {
            this.value = value;
        }
    }

    @Getter
    @Setter
    public static class DocumentFormat {
        private String value;

        public DocumentFormat(String value) {
            this.value = value;
        }
    }

    @Getter
    @Setter
    public static class ProductGroup {
        private String value;

        public ProductGroup(String value) {
            this.value = value;
        }
    }
}
