package com.clothingretail.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.clothingretail.support.CheckoutTestSupport;
import com.clothingretail.support.CheckoutTestSupport.CustomerSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerAddressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createListUpdateDeleteRoundTrip() throws Exception {
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "addr-crud");

        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        MvcResult listResult = mockMvc.perform(get("/api/customer/addresses").header("Authorization", "Bearer " + session.accessToken()))
                .andReturn();
        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(listJson.size()).isEqualTo(1);
        assertThat(listJson.get(0).get("id").asLong()).isEqualTo(addressId);
        assertThat(listJson.get(0).get("isDefault").asBoolean()).isTrue();

        String updateBody = """
                {"label":"Office","addressLine1":"42 Work Ave","city":"Pune","state":"Maharashtra","postalCode":"411001","country":"India","isDefault":false}
                """;
        MvcResult updateResult = mockMvc.perform(put("/api/customer/addresses/" + addressId)
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andReturn();
        assertThat(updateResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode updateJson = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertThat(updateJson.get("city").asText()).isEqualTo("Pune");

        MvcResult deleteResult = mockMvc.perform(delete("/api/customer/addresses/" + addressId)
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andReturn();
        assertThat(deleteResult.getResponse().getStatus()).isEqualTo(204);

        MvcResult listAfterDelete = mockMvc.perform(get("/api/customer/addresses").header("Authorization", "Bearer " + session.accessToken()))
                .andReturn();
        JsonNode listAfterDeleteJson = objectMapper.readTree(listAfterDelete.getResponse().getContentAsString());
        assertThat(listAfterDeleteJson.size()).isEqualTo(0);
    }

    @Test
    void customerCannotViewUpdateOrDeleteAnotherCustomersAddress() throws Exception {
        CustomerSession owner = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "addr-owner");
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, owner.accessToken());

        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "addr-intruder");

        String updateBody = """
                {"addressLine1":"Hacked","city":"Nowhere","state":"Nowhere","postalCode":"000000","country":"Nowhere"}
                """;
        MvcResult updateResult = mockMvc.perform(put("/api/customer/addresses/" + addressId)
                        .header("Authorization", "Bearer " + intruder.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andReturn();
        assertThat(updateResult.getResponse().getStatus()).isEqualTo(404);

        MvcResult deleteResult = mockMvc.perform(delete("/api/customer/addresses/" + addressId)
                        .header("Authorization", "Bearer " + intruder.accessToken()))
                .andReturn();
        assertThat(deleteResult.getResponse().getStatus()).isEqualTo(404);

        // The owner's address is untouched.
        MvcResult listResult = mockMvc.perform(get("/api/customer/addresses").header("Authorization", "Bearer " + owner.accessToken()))
                .andReturn();
        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(listJson.size()).isEqualTo(1);
        assertThat(listJson.get(0).get("city").asText()).isEqualTo("Mumbai");
    }

    @Test
    void addressEndpointsRequireAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/customer/addresses")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }
}
