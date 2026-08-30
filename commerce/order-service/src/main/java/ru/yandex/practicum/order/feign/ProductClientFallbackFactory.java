package ru.yandex.practicum.order.feign;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    private static final Logger log =
            LoggerFactory.getLogger(ProductClientFallbackFactory.class);

    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            if (cause instanceof FeignException feignException
                    && feignException.status() == 404) {
                throw feignException;
            }

            log.warn(
                    "product-service недоступен при запросе товара id={}",
                    productId,
                    cause
            );

            throw new ProductServiceUnavailableException(productId, cause);
        };
    }
}