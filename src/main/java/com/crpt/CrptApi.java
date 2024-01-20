package com.crpt;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

public class CrptApi {

    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final String DOC_TYPE = "LP_INTRODUCE_GOODS";

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final long timeInterval;

    private final AtomicInteger requestCounter;
    private final ReentrantLock lock;

    private final HttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit не может быть null");
        }
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("requestLimit должен быть положительным");
        }
        // Инициализация полей
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.timeInterval = timeUnit.toMillis(1);
        this.requestCounter = new AtomicInteger(0);
        this.lock = new ReentrantLock();
        this.httpClient = HttpClientBuilder.create().build();
    }

    // Метод для создания документа для ввода в оборот товара
    public void createDocument(String participantInn, String docId, String docStatus, boolean importRequest, String ownerInn, String producerInn, String productionDate, String productionType, String[] products) {
        if (participantInn == null || participantInn.isEmpty()) {
            throw new IllegalArgumentException("participantInn не может быть пустым");
        }
        if (docId == null || docId.isEmpty()) {
            throw new IllegalArgumentException("docId не может быть пустым");
        }
        if (docStatus == null || docStatus.isEmpty()) {
            throw new IllegalArgumentException("docStatus не может быть пустым");
        }
        if (ownerInn == null || ownerInn.isEmpty()) {
            throw new IllegalArgumentException("ownerInn не может быть пустым");
        }
        if (producerInn == null || producerInn.isEmpty()) {
            throw new IllegalArgumentException("producerInn не может быть пустым");
        }
        if (productionDate == null || productionDate.isEmpty()) {
            throw new IllegalArgumentException("productionDate не может быть пустым");
        }
        if (productionType == null || productionType.isEmpty()) {
            throw new IllegalArgumentException("productionType не может быть пустым");
        }
        if (products == null || products.length == 0) {
            throw new IllegalArgumentException("products не может быть пустым");
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("description", new JSONObject().put("participantInn", participantInn));
        jsonObject.put("doc_id", docId);
        jsonObject.put("doc_status", docStatus);
        jsonObject.put("doc_type", DOC_TYPE);
        jsonObject.put("importRequest", importRequest);
        jsonObject.put("owner_inn", ownerInn);
        jsonObject.put("participant_inn", participantInn);
        jsonObject.put("producer_inn", producerInn);
        jsonObject.put("production_date", productionDate);
        jsonObject.put("production_type", productionType);
        JSONObject[] productObjects = new JSONObject[products.length];
        for (int i = 0; i < products.length; i++) {

            if (products[i] == null || products[i].isEmpty()) {
                throw new IllegalArgumentException("products[" + i + "] не может быть пустым");
            }
            String[] productFields = products[i].split(",");
            if (productFields.length != 9) {
                throw new IllegalArgumentException("products[" + i + "] должен содержать 9 полей, разделенных запятыми");
            }
            JSONObject productObject = new JSONObject();
            productObject.put("certificate_document", productFields[0]);
            productObject.put("certificate_document_date", productFields[1]);
            productObject.put("certificate_document_number", productFields[2]);
            productObject.put("owner_inn", productFields[3]);
            productObject.put("producer_inn", productFields[4]);
            productObject.put("production_date", productFields[5]);
            productObject.put("tnved_code", productFields[6]);
            productObject.put("uit_code", productFields[7]);
            productObject.put("uitu_code", productFields[8]);
            productObjects[i] = productObject;
        }
        jsonObject.put("products", productObjects);
        jsonObject.put("reg_date", productionDate);
        jsonObject.put("reg_number", docId);
        // Выполнение HTTP-запроса с учетом ограничения на количество запросов
        try {
            // Попытка захватить блокировку
            lock.lock();
            // Проверка счетчика запросов
            if (requestCounter.get() >= requestLimit) {
                Thread.sleep(timeInterval);
                // Сброс счетчика запросов
                requestCounter.set(0);
            }
            // Увеличение счетчика запросов
            requestCounter.incrementAndGet();
            // Освобождение блокировки
            lock.unlock();
            HttpPost httpPost = new HttpPost(URL);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(jsonObject.toString()));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println("Документ успешно создан");
            } else {
                System.out.println("Ошибка при создании документа: " + httpResponse.getStatusLine().getReasonPhrase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}