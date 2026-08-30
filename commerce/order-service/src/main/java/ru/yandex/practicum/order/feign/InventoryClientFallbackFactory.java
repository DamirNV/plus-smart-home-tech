package ru.yandex.practicum.order.feign;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.feign.dto.ReserveResponse;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    private static final Logger log =
            LoggerFactory.getLogger(InventoryClientFallbackFactory.class);

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {

            @Override
            public ReserveResponse reserveStock(ReserveRequest request) {
                propagateBusinessFailure(cause);

                log.warn(
                        "inventory-service недоступен при резервировании товара id={}",
                        request.productId(),
                        cause
                );

                throw new InventoryServiceUnavailableException(
                        request.productId(),
                        cause
                );
            }

            @Override
            public ReserveResponse releaseStock(ReserveRequest request) {
                propagateBusinessFailure(cause);

                log.warn(
                        "inventory-service недоступен при снятии резерва товара id={}",
                        request.productId(),
                        cause
                );

                throw new InventoryServiceUnavailableException(
                        request.productId(),
                        cause
                );
            }
        };
    }

    private void propagateBusinessFailure(Throwable cause) {
        if (cause instanceof FeignException feignException) {
            int status = feignException.status();

            if (status == 400 || status == 404 || status == 409) {
                throw feignException;
            }
        }
    }
}