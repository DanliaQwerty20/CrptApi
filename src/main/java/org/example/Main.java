package org.example;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        // Создание объекта CrptApi с лимитом запросов в 1 запрос в секунду
        try {
            // Создаем экземпляр CrptApi с временной единицей и лимитом запросов
            CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);

            // Создаем тестовые данные для документа
            CrptApi.Document document = new CrptApi.Document(
                    "123", "Draft", "TestType",
                    "0987654321", "1234567890", "1112223334",
                    "2024-01-25", "TestProduction", "2024-01-25"
            );

            // Создаем тестовые данные для продуктов
            CrptApi.Product product = new CrptApi.Product(
                    "0987654321", "1112223334", "2024-01-25", "987654"
            );
            product.setCertificateDocument(new CrptApi.CertificateDocument("CONFORMITY_CERTIFICATE"));
            product.setCertificateDocumentDate("2024-01-25");
            product.setCertificateDocumentNumber("12345");
            product.setTnvedCode("987654");
            product.setUitCode("54321");
            product.setUituCode("12345");

            // Устанавливаем продукты в документ
            document.setProducts(Collections.singletonList(product));

            // Подписываем документ (ваша реализация подписи)
            String signature = "exampleSignature";

            // Вызываем метод для создания документа
            String result = api.createDocumentForRussianGoods(document, signature);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

