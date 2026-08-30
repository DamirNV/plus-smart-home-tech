package ru.yandex.practicum.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.dto.ProductDto;
import ru.yandex.practicum.order.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.feign.dto.ReserveResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unchecked")
class OrderServiceAcceptanceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockBean
    private ProductClient productClient;

    @MockBean
    private InventoryClient inventoryClient;

    @Test
    void shouldCreateOrderUsingRemoteProductDataAndFindOrderByIdAndEmail() throws Exception {
        when(productClient.getProductById(1L))
                .thenReturn(new ProductDto(
                        1L,
                        "Acceptance Smart Lamp",
                        new BigDecimal("3490.00"),
                        true
                ));

        when(productClient.getProductById(2L))
                .thenReturn(new ProductDto(
                        2L,
                        "Acceptance Smart Plug",
                        new BigDecimal("1290.00"),
                        true
                ));

        when(inventoryClient.reserveStock(new ReserveRequest(1L, 2)))
                .thenReturn(new ReserveResponse(
                        true,
                        10,
                        "Товар успешно зарезервирован"
                ));

        when(inventoryClient.reserveStock(new ReserveRequest(2L, 1)))
                .thenReturn(new ReserveResponse(
                        true,
                        10,
                        "Товар успешно зарезервирован"
                ));

        CreateOrderRequest request = new CreateOrderRequest(
                "Acceptance Buyer",
                "acceptance-buyer@example.com",
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)
                )
        );

        MvcResult createResponse = postJson("/api/orders", request);

        assertThat(status(createResponse))
                .as("POST /api/orders должен создавать заказ и возвращать HTTP 201 Created")
                .isEqualTo(201);

        Map<String, Object> created = readMap(createResponse);

        Long orderId = asLong(created.get("id"));

        assertThat(orderId)
                .as("Созданный заказ должен содержать поле id")
                .isNotNull();

        assertThat(created.get("status"))
                .as("После успешного резервирования заказ должен иметь статус CONFIRMED")
                .isEqualTo("CONFIRMED");

        assertThat(asDecimal(created.get("totalPrice")))
                .as("order-service должен рассчитывать totalPrice по данным product-service")
                .isEqualByComparingTo("8270.00");

        assertThat((List<?>) created.get("items"))
                .as("Заказ должен хранить снимки товарных данных")
                .hasSize(2)
                .anySatisfy(item -> assertThat((Map<String, Object>) item)
                        .containsEntry(
                                "productName",
                                "Acceptance Smart Lamp"
                        ));

        MvcResult byIdResponse =
                mvc.perform(get("/api/orders/{id}", orderId))
                        .andReturn();

        assertThat(status(byIdResponse))
                .as("GET /api/orders/{id} должен возвращать созданный заказ")
                .isEqualTo(200);

        assertThat(readMap(byIdResponse).get("customerEmail"))
                .isEqualTo("acceptance-buyer@example.com");

        MvcResult byEmailResponse =
                mvc.perform(
                                get("/api/orders/by-email")
                                        .param(
                                                "email",
                                                "acceptance-buyer@example.com"
                                        )
                        )
                        .andReturn();

        assertThat(status(byEmailResponse))
                .isEqualTo(200);

        assertThat(readList(byEmailResponse))
                .anySatisfy(item ->
                        assertThat(item)
                                .containsEntry(
                                        "customerEmail",
                                        "acceptance-buyer@example.com"
                                )
                );
    }

    @Test
    void shouldPersistPendingOrderWhenProductServiceUnavailable() throws Exception {
        when(productClient.getProductById(77L))
                .thenThrow(
                        new ProductServiceUnavailableException(
                                77L,
                                new RuntimeException("timeout")
                        )
                );

        CreateOrderRequest request = new CreateOrderRequest(
                "Pending Buyer",
                "pending-buyer@example.com",
                List.of(
                        new OrderItemRequest(77L, 2)
                )
        );

        MvcResult createResponse =
                postJson("/api/orders", request);

        assertThat(status(createResponse))
                .as("При технической недоступности product-service заказ должен быть принят")
                .isEqualTo(201);

        Map<String, Object> created =
                readMap(createResponse);

        Long orderId =
                asLong(created.get("id"));

        assertThat(orderId)
                .isNotNull();

        assertThat(created.get("status"))
                .isEqualTo("PENDING_CONFIRMATION");

        assertThat(created.get("statusDetails"))
                .isNotNull();

        assertThat(created.get("statusDetails").toString())
                .isNotBlank();

        assertThat(asDecimal(created.get("totalPrice")))
                .isEqualByComparingTo(BigDecimal.ZERO);

        List<Map<String, Object>> createdItems =
                (List<Map<String, Object>>) created.get("items");

        assertThat(createdItems)
                .hasSize(1);

        Map<String, Object> createdItem =
                createdItems.get(0);

        assertThat(asLong(createdItem.get("productId")))
                .isEqualTo(77L);

        assertThat(createdItem.get("productName").toString())
                .contains("#77");

        assertThat(asDecimal(createdItem.get("price")))
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(((Number) createdItem.get("quantity")).intValue())
                .isEqualTo(2);

        MvcResult byIdResponse =
                mvc.perform(
                                get(
                                        "/api/orders/{id}",
                                        orderId
                                )
                        )
                        .andReturn();

        assertThat(status(byIdResponse))
                .isEqualTo(200);

        Map<String, Object> persisted =
                readMap(byIdResponse);

        assertThat(persisted.get("status"))
                .as("Статус PENDING_CONFIRMATION должен быть сохранён в базе")
                .isEqualTo("PENDING_CONFIRMATION");

        assertThat(asDecimal(persisted.get("totalPrice")))
                .isEqualByComparingTo(BigDecimal.ZERO);

        List<Map<String, Object>> persistedItems =
                (List<Map<String, Object>>) persisted.get("items");

        assertThat(persistedItems)
                .hasSize(1);

        assertThat(
                asLong(
                        persistedItems
                                .get(0)
                                .get("productId")
                )
        ).isEqualTo(77L);

        assertThat(
                persistedItems
                        .get(0)
                        .get("productName")
                        .toString()
        ).contains("#77");
    }
    @Test
    void shouldReturnBadRequestForInvalidOrderPayload() throws Exception {
        CreateOrderRequest invalidRequest =
                new CreateOrderRequest(
                        "",
                        "not-an-email",
                        List.of()
                );

        MvcResult response =
                postJson("/api/orders", invalidRequest);

        assertThat(status(response))
                .as("Невалидный заказ должен возвращать HTTP 400 Bad Request")
                .isEqualTo(400);

        assertThat(readMap(response))
                .containsKeys(
                        "message",
                        "validationErrors"
                );
    }

    private MvcResult postJson(
            String url,
            Object body
    ) throws Exception {
        return mvc.perform(
                        post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(body))
                )
                .andReturn();
    }

    private static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private Map<String, Object> readMap(
            MvcResult result
    ) throws Exception {
        return json.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
    }

    private List<Map<String, Object>> readList(
            MvcResult result
    ) throws Exception {
        return json.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
    }

    private static Long asLong(Object value) {
        return value == null
                ? null
                : ((Number) value).longValue();
    }

    private static BigDecimal asDecimal(Object value) {
        return new BigDecimal(value.toString());
    }
}